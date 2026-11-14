const assert = require('node:assert/strict');
const {test} = require('node:test');
const fs = require('node:fs');
const path = require('node:path');
const {contentHash, buildCandidate, validateSheetHeaders, validateSupplierRoster, deploymentConfig, jsonRequest,
  refreshAccess, preflight, smoke, retrySmoke, publish} = require('../production-backend.cjs');

const root = path.resolve(__dirname, '../..');
const environment = {environment: 'production', scriptId: 'prod-script', deploymentId: 'prod-deployment',
  spreadsheetId: 'prod-sheet', rootFolderId: 'prod-folder'};
const manifest = {timeZone: 'Europe/Moscow', runtimeVersion: 'V8',
  webapp: {executeAs: 'USER_DEPLOYING', access: 'ANYONE_ANONYMOUS'}};
const files = text => [{name: 'Code', type: 'SERVER_JS', source: text},
  {name: 'appsscript', type: 'JSON', source: JSON.stringify(manifest)}];
const source = files('previous editor');
const oldVersion = files('previous published');
const candidate = {files: files('candidate'), schema: [{name: 'coffee_station', headers: ['id', 'name']}]};

function deployment(version = 23) {
  return {deploymentId: environment.deploymentId, deploymentConfig: {scriptId: environment.scriptId,
    versionNumber: version, manifestFileName: 'appsscript', description: 'Existing release'},
  entryPoints: [{entryPointType: 'WEB_APP', webApp: {
    url: `https://script.google.com/macros/s/${environment.deploymentId}/exec`}}]};
}

function fixture() {
  const state = {head: structuredClone(source), deployment: deployment(),
    versions: {23: structuredClone(oldVersion)}, log: [], metadata: {spreadsheetId: environment.spreadsheetId,
      properties: {timeZone: 'Europe/Moscow'}, sheets: [{properties: {title: 'coffee_station'}}]}};
  const api = async (url, method = 'GET', body) => {
    state.log.push({url, method});
    if (url.includes('/spreadsheets/') && !url.includes('/values:')) return structuredClone(state.metadata);
    if (url.includes('/values:')) return {valueRanges: [{values: [['id', 'name']]}]};
    if (url.includes('/deployments/')) {
      if (method === 'PUT') state.deployment.deploymentConfig = structuredClone(body.deploymentConfig);
      return structuredClone(state.deployment);
    }
    if (url.endsWith('/versions') && method === 'POST') {
      const versionNumber = Math.max(...Object.keys(state.versions).map(Number)) + 1;
      state.versions[versionNumber] = structuredClone(state.head);
      return {versionNumber};
    }
    if (url.includes('/content?versionNumber=')) return {files: structuredClone(state.versions[Number(url.split('=').pop())])};
    if (url.endsWith('/content')) {
      if (method === 'PUT') state.head = structuredClone(body.files);
      return {files: structuredClone(state.head)};
    }
    throw new Error('Unexpected fake endpoint');
  };
  return {state, api};
}

test('candidate is generated from repository files with production isolation and no local output', () => {
  const prod = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/production.json'), 'utf8'));
  const testEnv = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));
  const actual = buildCandidate(root, prod, testEnv);
  const config = actual.files.find(f => f.name === 'Config').source;
  assert.match(config, /"ENVIRONMENT": "production"/);
  assert.ok(config.includes(prod.spreadsheetId));
  assert.ok(!config.includes(testEnv.spreadsheetId));
  assert.equal(JSON.parse(actual.files.find(f => f.name === 'appsscript').source).webapp.access, 'ANYONE_ANONYMOUS');
  assert.ok(JSON.parse(actual.files.find(f => f.name === 'appsscript').source)
    .dependencies.enabledAdvancedServices.some(service => service.userSymbol === 'Sheets' && service.version === 'v4'));
  assert.ok(actual.schema.some(sheet => sheet.name === 'notification_settings'));
  assert.throws(() => buildCandidate(root, {...prod, spreadsheetId: 'wrong'}, testEnv));
  assert.throws(() => buildCandidate(root, {...prod, scriptId: testEnv.scriptId}, testEnv));
});

test('source digest ignores Google metadata and file ordering but detects content changes', () => {
  assert.equal(contentHash(source), contentHash([...source].reverse().map(f => ({...f, createTime: 'ignored'}))));
  assert.notEqual(contentHash(source), contentHash(candidate.files));
});

test('production roster requires one real active supplier with unique positive employee IDs', () => {
  const operator = [1, 'Maxim', 'OPERATOR', '2026-09-13', true];
  const supplier = [12, 'Denis', 'SUPPLY_OPERATOR', '', true];
  assert.doesNotThrow(() => validateSupplierRoster([operator, supplier, []]));
  for (const rows of [[operator], [operator, [...supplier.slice(0, 4), false]],
    [operator, supplier, [13, 'Other', 'SUPPLY_OPERATOR', '', true]],
    [operator, [1, ...supplier.slice(1)]], [operator, [0, ...supplier.slice(1)]],
    [operator, [12, '', 'SUPPLY_OPERATOR', '', true]]]) {
    assert.throws(() => validateSupplierRoster(rows));
  }
});

test('visit candidate preflight checks supplier roster before any remote writes', async () => {
  const f = fixture();
  const next = {...candidate, schema: [...candidate.schema,
    {name: 'employees', headers: ['id', 'name', 'role', 'workStartDate', 'isActive']},
    {name: 'station_service_visits', headers: ['id']}]};
  f.state.metadata.sheets.push({properties: {title: 'employees', gridProperties: {rowCount: 100}}},
    {properties: {title: 'station_service_visits'}});
  let roster = [[1, 'Maxim', 'OPERATOR', '', true]];
  const api = async (url, ...args) => {
    if (url.includes('/values:')) {
      const ranges = new URL(url).searchParams.getAll('ranges');
      if (ranges[0] === "'employees'!A2:E100") return {valueRanges: [{values: roster}]};
      return {valueRanges: next.schema.map(item => ({values: [item.headers]}))};
    }
    return f.api(url, ...args);
  };
  await assert.rejects(preflight(api, environment, next, 23), /exactly one named active supplier/);
  assert.ok(f.state.log.every(call => call.method === 'GET'));
  roster.push([12, 'Denis', 'SUPPLY_OPERATOR', '', true]);
  await preflight(api, environment, next, 23);
  assert.ok(f.state.log.every(call => call.method === 'GET'));
});

test('preflight checks version URL timezone and column order before any write', async () => {
  const f = fixture();
  const backup = await preflight(f.api, environment, candidate, 23);
  assert.equal(backup.previous.versionNumber, 23);
  assert.equal(contentHash(backup.oldHead), contentHash(source));
  assert.equal(contentHash(backup.oldVersion), contentHash(oldVersion));
  assert.ok(f.state.log.every(call => call.method === 'GET'));
  assert.throws(() => deploymentConfig(deployment(), environment, 22));
  const other = deployment(); other.entryPoints[0].webApp.url = 'https://example.com/exec';
  assert.throws(() => deploymentConfig(other, environment, 23));
  assert.throws(() => validateSheetHeaders(candidate.schema, {...f.state.metadata,
    properties: {timeZone: 'UTC'}}, [{values: [['id', 'name']]}], environment.spreadsheetId));
  assert.throws(() => validateSheetHeaders(candidate.schema, f.state.metadata,
    [{values: [['name', 'id']]}], environment.spreadsheetId));
});

test('missing sheet or changed manifest stops preflight without uploading code', async () => {
  const f = fixture(); f.state.metadata.sheets = [];
  await assert.rejects(preflight(f.api, environment, candidate, 23), /Missing production sheets/);
  assert.ok(f.state.log.every(call => call.method === 'GET'));
  const second = fixture();
  const modified = structuredClone(candidate);
  modified.files[1].source = JSON.stringify({...manifest, oauthScopes: ['new-scope']});
  await assert.rejects(preflight(second.api, environment, modified, 23), /separate review/);
  assert.ok(second.state.log.every(call => call.method === 'GET'));
});

async function execute(f, checkSmoke = async () => {}) {
  const backup = await preflight(f.api, environment, candidate, 23);
  const report = {};
  const writes = [];
  const args = {api: f.api, environment, candidate, backup, commit: 'a'.repeat(40), expectedVersion: 23,
    checkSmoke, report, saveReport: () => writes.push(structuredClone(report))};
  return {args, report, writes};
}

test('publish updates existing deployment only and records commit hashes and version', async () => {
  const f = fixture(); let smokes = 0;
  const run = await execute(f, async () => { smokes++; });
  await publish(run.args);
  assert.equal(run.report.status, 'published');
  assert.equal(run.report.previousVersion, 23);
  assert.equal(run.report.backupHeadVersion, 24);
  assert.equal(run.report.newVersion, 25);
  assert.equal(f.state.deployment.deploymentConfig.versionNumber, 25);
  assert.equal(smokes, 1);
  assert.equal(contentHash(f.state.head), contentHash(candidate.files));
  assert.equal(f.state.log.filter(c => c.method === 'POST').length, 2);
  assert.ok(f.state.log.filter(c => c.method === 'POST').every(c => c.url.endsWith('/versions')));
  assert.ok(!f.state.log.some(c => c.url.includes(':run') || c.method === 'DELETE'));
  assert.ok(!JSON.stringify(run.report).includes('previous editor'));
});

test('failed smoke restores both previous deployment and editor code without hiding failure', async () => {
  const f = fixture();
  const run = await execute(f, async () => { throw new Error('Smoke failed'); });
  await assert.rejects(publish(run.args), /Smoke failed/);
  assert.equal(run.report.status, 'rolled-back');
  assert.equal(f.state.deployment.deploymentConfig.versionNumber, 23);
  assert.equal(contentHash(f.state.head), contentHash(source));
});

test('timeout after applied content upload is reconciled and restored', async () => {
  const f = fixture(); const run = await execute(f);
  const original = run.args.api; let first = true;
  run.args.api = async (url, method, body) => {
    const result = await original(url, method, body);
    if (first && method === 'PUT' && url.endsWith('/content')) {
      first = false; throw new Error('Google API transport failure.');
    }
    return result;
  };
  await assert.rejects(publish(run.args), /transport failure/);
  assert.equal(run.report.status, 'rolled-back');
  assert.equal(contentHash(f.state.head), contentHash(source));
});

test('concurrent editor changes are not overwritten by publication or rollback', async () => {
  const f = fixture(); const run = await execute(f);
  f.state.head = files('manual edit');
  await assert.rejects(publish(run.args), /Remote editor changed/);
  assert.ok(f.state.log.every(call => call.method === 'GET'));
  const second = fixture();
  const run2 = await execute(second, async () => { second.state.head = files('manual edit'); throw new Error('failed'); });
  await assert.rejects(publish(run2.args), /manual attention/);
  assert.equal(run2.report.status, 'rollback-needs-attention');
  assert.equal(contentHash(second.state.head), contentHash(files('manual edit')));
});

test('OAuth and API errors never expose response bodies or authorized URLs', async () => {
  const fetcher = async () => { throw new Error('https://secret/?token=do-not-log'); };
  await assert.rejects(jsonRequest(fetcher, 'https://secret/', {}, 'Google API'), /^Error: Google API transport failure\.$/);
  await assert.rejects(refreshAccess({}), /Missing clasp OAuth/);
  await assert.rejects(jsonRequest(async () => ({ok: false, status: 401}), 'https://secret/', {}, 'Google API'), /HTTP 401/);
});

test('smoke is read-only and validates every expected response collection and header', async () => {
  const schema = {coffee_station: {responseKey: 'stations', headers: ['id', 'name']}};
  let observed;
  await smoke(environment, 'test-token', schema, async (url, options) => {
    observed = {url, options};
    return {ok: true, json: async () => ({success: true, data: {stations: [{id: 1, name: 'Station'}]}})};
  });
  assert.equal(observed.url.searchParams.get('action'), 'full_sync');
  assert.equal(observed.options.method, undefined);
  await assert.rejects(smoke(environment, 'test-token', schema, async () => ({ok: true,
    json: async () => ({success: true, data: {stations: [{id: 1}]}})})), /contract mismatch/);
});

test('read-only smoke tolerates short deployment propagation but does not retry forever', async () => {
  let attempts = 0;
  let delays = 0;
  await retrySmoke(async () => { if (++attempts < 3) throw new Error('not ready'); }, async () => { delays++; });
  assert.equal(attempts, 3);
  assert.equal(delays, 2);
  attempts = 0;
  await assert.rejects(retrySmoke(async () => { attempts++; throw new Error('unavailable'); }, async () => {}));
  assert.equal(attempts, 3);
});

test('workflow is manual protected serial and read-only by default with no credential artifacts', () => {
  const workflow = fs.readFileSync(path.join(root, '.github/workflows/apps-script-production.yml'), 'utf8');
  assert.match(workflow, /workflow_dispatch:/);
  assert.doesNotMatch(workflow, /^  (push|pull_request|schedule):/m);
  assert.match(workflow, /environment: production-apps-script/);
  assert.match(workflow, /cancel-in-progress: false/);
  assert.match(workflow, /default: false/);
  assert.match(workflow, /deployment-report\.json/);
  assert.doesNotMatch(workflow, /path:.*(?:clasprc|backup|\.codex)/);
  assert.match(workflow, /ref: \$\{\{ inputs.source_sha \}\}/);
});
