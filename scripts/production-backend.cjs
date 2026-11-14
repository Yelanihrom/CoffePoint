const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const crypto = require('node:crypto');
const {execFileSync} = require('node:child_process');
const {validateProduction} = require('./prepare-production-access.cjs');

function contentHash(files) {
  return crypto.createHash('sha256').update(JSON.stringify(files.map(({name, type, source}) =>
    ({name, type, source})).sort((a, b) => a.name.localeCompare(b.name)))).digest('hex');
}

function buildCandidate(root, environment, test) {
  validateProduction(environment, test);
  const folder = path.join(root, 'google-apps-script');
  const source = fs.readFileSync(path.join(folder, 'Config.gs'), 'utf8');
  const context = vm.createContext({});
  vm.runInContext(source, context, {timeout: 1000});
  const {CONFIG, SHEETS} = vm.runInContext('({CONFIG, SHEETS})', context);
  if (CONFIG.SPREADSHEET_ID !== environment.spreadsheetId || CONFIG.ROOT_FOLDER_ID !== environment.rootFolderId ||
      CONFIG.TIMEZONE !== 'Europe/Moscow') throw new Error('Candidate production storage/timezone mismatch.');
  if (!Number.isSafeInteger(CONFIG.SUPPLY_EARNINGS_PER_VISIT) || CONFIG.SUPPLY_EARNINGS_PER_VISIT <= 0) {
    throw new Error('Invalid supplier earnings rate.');
  }
  const config = {...CONFIG, ENVIRONMENT: 'production'};
  const files = fs.readdirSync(folder).filter(name => name.endsWith('.gs') || name === 'appsscript.json')
    .sort().map(filename => ({
      name: filename === 'appsscript.json' ? 'appsscript' : filename.slice(0, -3),
      type: filename === 'appsscript.json' ? 'JSON' : 'SERVER_JS',
      source: filename === 'Config.gs'
        ? `const CONFIG = ${JSON.stringify(config, null, 2)};\nconst SHEETS = ${JSON.stringify(SHEETS, null, 2)};\n`
        : fs.readFileSync(path.join(folder, filename), 'utf8'),
    }));
  const manifest = JSON.parse(files.find(file => file.name === 'appsscript').source);
  if (manifest.timeZone !== config.TIMEZONE || manifest.runtimeVersion !== 'V8') {
    throw new Error('Unexpected Apps Script manifest.');
  }
  if (!manifest.dependencies?.enabledAdvancedServices?.some(service =>
      service.userSymbol === 'Sheets' && service.serviceId === 'sheets' && service.version === 'v4')) {
    throw new Error('Unexpected Apps Script manifest: Advanced Sheets v4 is required for visits.');
  }
  // Preserve the existing public token-authenticated API, as in the test bundle generator.
  manifest.webapp = {executeAs: 'USER_DEPLOYING', access: 'ANYONE_ANONYMOUS'};
  files.find(file => file.name === 'appsscript').source = JSON.stringify(manifest, null, 2) + '\n';
  return {files, schema: Object.values(SHEETS).map(sheet => ({name: sheet.name, headers: Array.from(sheet.headers)}))};
}

function validateSheetHeaders(schema, metadata, ranges, spreadsheetId) {
  if (metadata.spreadsheetId !== spreadsheetId || metadata.properties?.timeZone !== 'Europe/Moscow') {
    throw new Error('Production spreadsheet/timezone mismatch.');
  }
  const tabs = new Set(metadata.sheets.map(sheet => sheet.properties.title));
  const missing = schema.filter(sheet => !tabs.has(sheet.name)).map(sheet => sheet.name);
  if (missing.length) throw new Error('Missing production sheets: ' + missing.join(', '));
  schema.forEach((sheet, i) => {
    const actual = ranges[i]?.values?.[0] || [];
    if (!sheet.headers.every((header, index) => header === actual[index])) {
      throw new Error('Production headers mismatch: ' + sheet.name);
    }
  });
}

function deploymentConfig(deployment, environment, expectedVersion) {
  const config = deployment.deploymentConfig;
  if (deployment.deploymentId !== environment.deploymentId || config?.scriptId !== environment.scriptId ||
      config.versionNumber !== expectedVersion || !Number.isInteger(expectedVersion) || expectedVersion <= 0 ||
      config.manifestFileName !== 'appsscript') throw new Error('Production deployment/version mismatch.');
  const url = `https://script.google.com/macros/s/${environment.deploymentId}/exec`;
  if (!deployment.entryPoints?.some(entry => entry.entryPointType === 'WEB_APP' && entry.webApp?.url === url)) {
    throw new Error('Expected production web app URL was not found.');
  }
  return {...config};
}

function validateSupplierRoster(rows) {
  const employees = rows.filter(row => row.some(value => value !== '' && value != null));
  const ids = employees.map(row => Number(row[0]));
  if (ids.some(id => !Number.isSafeInteger(id) || id <= 0) || new Set(ids).size !== ids.length) {
    throw new Error('Production employee IDs are invalid or duplicated.');
  }
  const suppliers = employees.filter(row => row[2] === 'SUPPLY_OPERATOR' &&
    [true, 1, 'true', 'TRUE', '1'].includes(row[4]));
  if (suppliers.length !== 1 || typeof suppliers[0][1] !== 'string' || !suppliers[0][1].trim()) {
    throw new Error('Production requires exactly one named active supplier before publication.');
  }
}

// No upstream response bodies/URLs in errors: both can contain credentials or business data.
async function jsonRequest(fetcher, url, options, label) {
  let response;
  try { response = await fetcher(url, {...options, signal: AbortSignal.timeout(60000)}); }
  catch { throw new Error(label + ' transport failure.'); }
  if (!response.ok) throw new Error(label + ' HTTP ' + response.status);
  try { return await response.json(); } catch { throw new Error(label + ' invalid JSON.'); }
}

function googleClient(accessToken, fetcher = fetch) {
  return (url, method = 'GET', body) => jsonRequest(fetcher, url, {
    method, headers: {Authorization: 'Bearer ' + accessToken, 'Content-Type': 'application/json'},
    ...(body === undefined ? {} : {body: JSON.stringify(body)}),
  }, 'Google API');
}

async function refreshAccess(auth, fetcher = fetch) {
  const token = auth.tokens?.default;
  if (!token?.client_id || !token.client_secret || !token.refresh_token) throw new Error('Missing clasp OAuth credentials.');
  const result = await jsonRequest(fetcher, 'https://oauth2.googleapis.com/token', {
    method: 'POST', body: new URLSearchParams({grant_type: 'refresh_token', client_id: token.client_id,
      client_secret: token.client_secret, refresh_token: token.refresh_token}),
  }, 'Google OAuth');
  if (!result.access_token) throw new Error('Google OAuth returned no access token.');
  return result.access_token;
}

async function preflight(api, environment, candidate, expectedVersion) {
  const project = `https://script.googleapis.com/v1/projects/${environment.scriptId}`;
  const sheet = `https://sheets.googleapis.com/v4/spreadsheets/${environment.spreadsheetId}`;
  const deployment = await api(`${project}/deployments/${environment.deploymentId}`);
  const previous = deploymentConfig(deployment, environment, expectedVersion);
  const metadata = await api(sheet + '?fields=spreadsheetId,properties(timeZone),sheets(properties(title,gridProperties(rowCount)))');
  const missing = candidate.schema.filter(item => !metadata.sheets?.some(s => s.properties.title === item.name));
  if (missing.length) throw new Error('Missing production sheets: ' + missing.map(s => s.name).join(', '));
  const params = new URLSearchParams({valueRenderOption: 'UNFORMATTED_VALUE'});
  candidate.schema.forEach(item => params.append('ranges', `'${item.name}'!1:1`));
  const headers = await api(`${sheet}/values:batchGet?${params}`);
  validateSheetHeaders(candidate.schema, metadata, headers.valueRanges || [], environment.spreadsheetId);
  if (candidate.schema.some(item => item.name === 'station_service_visits')) {
    const rowCount = metadata.sheets.find(s => s.properties.title === 'employees')?.properties.gridProperties?.rowCount;
    if (!Number.isInteger(rowCount) || rowCount < 2 || rowCount > 10000) {
      throw new Error('Production employee grid requires a bounded roster review.');
    }
    const ranges = new URLSearchParams({valueRenderOption: 'UNFORMATTED_VALUE'});
    for (let row = 2; row <= rowCount; row += 500) {
      ranges.append('ranges', `'employees'!A${row}:E${Math.min(row + 499, rowCount)}`);
    }
    const roster = await api(`${sheet}/values:batchGet?${ranges}`);
    if (roster.valueRanges?.length !== Math.ceil((rowCount - 1) / 500)) {
      throw new Error('Incomplete production employee roster response.');
    }
    validateSupplierRoster(roster.valueRanges.flatMap(range => range.values || []));
  }
  const oldHead = (await api(project + '/content')).files;
  const oldVersion = (await api(project + '/content?versionNumber=' + expectedVersion)).files;
  if (!oldHead?.length || !oldVersion?.length) throw new Error('Cannot read previous server source.');
  const oldManifest = JSON.parse(oldVersion.find(file => file.name === 'appsscript')?.source || '{}');
  const newManifest = JSON.parse(candidate.files.find(file => file.name === 'appsscript').source);
  if (oldManifest.webapp?.executeAs !== newManifest.webapp?.executeAs ||
      oldManifest.webapp?.access !== newManifest.webapp?.access ||
      JSON.stringify([...(oldManifest.oauthScopes || [])].sort()) !==
        JSON.stringify([...(newManifest.oauthScopes || [])].sort())) {
    throw new Error('Web app access or explicit OAuth scope changes require separate review.');
  }
  return {previous, oldHead, oldVersion};
}

async function smoke(environment, syncToken, schema, fetcher = fetch, entities) {
  if (!syncToken) throw new Error('Missing production sync token.');
  const url = new URL(`https://script.google.com/macros/s/${environment.deploymentId}/exec`);
  url.searchParams.set('action', 'full_sync');
  url.searchParams.set('token', syncToken);
  if (entities) url.searchParams.set('entities', entities);
  const response = await jsonRequest(fetcher, url, {}, 'Production smoke');
  if (!response.success || !response.data) throw new Error('Production smoke returned an API error.');
  for (const definition of Object.values(schema)) {
    const rows = response.data[definition.responseKey];
    if (!Array.isArray(rows) || rows.some(row => !row ||
        !definition.headers.every(header => Object.hasOwn(row, header)))) {
      throw new Error('Production smoke response contract mismatch.');
    }
  }
}

async function retrySmoke(check, delay = ms => new Promise(resolve => setTimeout(resolve, ms))) {
  for (let attempt = 0; attempt < 3; attempt++) {
    try { return await check(); }
    catch (error) {
      if (attempt === 2) throw error;
      await delay(3000);
    }
  }
}

async function publish({api, environment, candidate, backup, commit, expectedVersion, checkSmoke, report, saveReport}) {
  const project = `https://script.googleapis.com/v1/projects/${environment.scriptId}`;
  const deploymentUrl = `${project}/deployments/${environment.deploymentId}`;
  const candidateHash = contentHash(candidate.files);
  report.previousVersion = expectedVersion;
  report.previousHeadHash = contentHash(backup.oldHead);
  report.previousPublishedHash = contentHash(backup.oldVersion);
  report.candidateHash = candidateHash;
  saveReport();
  // An editor change after approval/preflight must not be silently overwritten.
  const current = await api(deploymentUrl);
  deploymentConfig(current, environment, expectedVersion);
  if (contentHash((await api(project + '/content')).files) !== report.previousHeadHash) {
    throw new Error('Remote editor changed since preflight.');
  }
  // Save unpublished editor/trigger code too; the existing deployment already preserves its own version.
  const headBackup = await api(project + '/versions', 'POST', {description: 'Before Git ' + commit});
  if (!Number.isInteger(headBackup.versionNumber) || headBackup.versionNumber <= expectedVersion) {
    throw new Error('Invalid created backup version.');
  }
  report.backupHeadVersion = headBackup.versionNumber; saveReport();
  if (contentHash((await api(project + '/content?versionNumber=' + headBackup.versionNumber)).files) !== report.previousHeadHash) {
    throw new Error('Remote editor changed during backup.');
  }
  deploymentConfig(await api(deploymentUrl), environment, expectedVersion);
  let attemptedHeadWrite = false;
  let createdVersion;
  try {
    attemptedHeadWrite = true;
    report.status = 'updating-head'; saveReport();
    await api(project + '/content', 'PUT', {files: candidate.files});
    if (contentHash((await api(project + '/content')).files) !== candidateHash) {
      throw new Error('Uploaded source verification failed.');
    }
    const version = await api(project + '/versions', 'POST', {description: 'Git ' + commit});
    createdVersion = version.versionNumber;
    if (!Number.isInteger(createdVersion) || createdVersion <= expectedVersion) throw new Error('Invalid created version.');
    report.newVersion = createdVersion; saveReport();
    const publishedFiles = (await api(project + '/content?versionNumber=' + createdVersion)).files;
    if (contentHash(publishedFiles) !== candidateHash) throw new Error('Version source verification failed.');
    if (contentHash((await api(project + '/content')).files) !== candidateHash) {
      throw new Error('Remote editor changed during publication.');
    }
    await api(deploymentUrl, 'PUT', {deploymentConfig: {...backup.previous,
      versionNumber: createdVersion, description: 'Git ' + commit}});
    deploymentConfig(await api(deploymentUrl), environment, createdVersion);
    await checkSmoke();
    report.status = 'published'; saveReport();
  } catch (error) {
    report.status = 'failed'; saveReport();
    if (attemptedHeadWrite) {
      try {
        const live = await api(deploymentUrl);
        const version = live.deploymentConfig?.versionNumber;
        if (version !== expectedVersion && version !== createdVersion) {
          throw new Error('Deployment changed externally; refusing rollback.');
        }
        const headHash = contentHash((await api(project + '/content')).files);
        if (headHash !== candidateHash && headHash !== report.previousHeadHash) {
          throw new Error('Editor changed externally; refusing rollback.');
        }
        if (headHash === candidateHash) await api(project + '/content', 'PUT', {files: backup.oldHead.map(
          ({name, type, source}) => ({name, type, source}))});
        if (version !== expectedVersion) await api(deploymentUrl, 'PUT', {deploymentConfig: backup.previous});
        deploymentConfig(await api(deploymentUrl), environment, expectedVersion);
        if (contentHash((await api(project + '/content')).files) !== report.previousHeadHash) {
          throw new Error('Rollback verification failed.');
        }
        report.status = 'rolled-back'; saveReport();
      } catch {
        report.status = 'rollback-needs-attention'; saveReport();
        throw new Error('Production deployment failed; rollback needs manual attention.');
      }
    }
    throw error;
  }
}

async function main() {
  const root = path.resolve(__dirname, '..');
  const commit = process.env.SOURCE_SHA || '';
  if (process.env.GITHUB_ACTIONS !== 'true' || process.env.GITHUB_REF !== 'refs/heads/master' ||
      !/^[a-f0-9]{40}$/.test(commit)) throw new Error('Run from the approved master GitHub workflow.');
  const git = (...args) => execFileSync('git', args, {cwd: root, encoding: 'utf8'}).trim();
  if (git('rev-parse', 'HEAD') !== commit || git('status', '--porcelain', '--untracked-files=no')) {
    throw new Error('Expected a clean checkout of the selected commit.');
  }
  git('merge-base', '--is-ancestor', commit, 'origin/master');
  const environment = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/production.json'), 'utf8'));
  const test = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));
  const candidate = buildCandidate(root, environment, test);
  const schema = JSON.parse(fs.readFileSync(path.join(root, 'app/src/test/resources/contracts/sheet-schema.json'), 'utf8'));
  const expectedVersion = Number(process.env.EXPECTED_VERSION);
  if (!Number.isInteger(expectedVersion) || expectedVersion <= 0) throw new Error('Expected version must be a positive integer.');
  const reportFile = path.join(process.env.RUNNER_TEMP, 'deployment-report.json');
  const report = {commit, deploymentId: environment.deploymentId, expectedVersion, status: 'preflight',
    publishRequested: process.env.PUBLISH === 'true'};
  const saveReport = () => fs.writeFileSync(reportFile, JSON.stringify(report, null, 2) + '\n');
  saveReport();
  try {
    if (!process.env.PRODUCTION_SYNC_TOKEN) throw new Error('Missing production sync token.');
    const auth = JSON.parse(process.env.PRODUCTION_CLASP_AUTH_JSON || '{}');
    const api = googleClient(await refreshAccess(auth));
    const backup = await preflight(api, environment, candidate, expectedVersion);
    await smoke(environment, process.env.PRODUCTION_SYNC_TOKEN, {coffee_station: schema.coffee_station}, fetch, 'coffee_station');
    if (process.env.PUBLISH !== 'true') {
      report.status = 'preflight-passed'; saveReport();
      console.log('Production preflight passed. No remote writes.');
      return;
    }
    await publish({api, environment, candidate, backup, commit, expectedVersion,
      checkSmoke: () => retrySmoke(() => smoke(environment, process.env.PRODUCTION_SYNC_TOKEN, schema)), report, saveReport});
    console.log('Production deployment verified. See deployment-report.json for versions.');
  } catch (error) {
    if (report.status === 'preflight') { report.status = 'preflight-failed'; saveReport(); }
    throw error;
  }
}

module.exports = {contentHash, buildCandidate, validateSheetHeaders, validateSupplierRoster, deploymentConfig,
  jsonRequest, refreshAccess, preflight, smoke, retrySmoke, publish};
if (require.main === module) main().catch(error => {
  // Never print arbitrary exceptions: network libraries may attach authorized URLs.
  const safe = /^(Expected |Run from |Missing production|Candidate |Unexpected Apps Script|Production |Google |Cannot read |Web app |Remote editor |Uploaded source |Invalid created |Version source )/.test(error.message);
  console.error(safe ? error.message : 'Production workflow failed; no credential details logged.');
  process.exitCode = 1;
});
