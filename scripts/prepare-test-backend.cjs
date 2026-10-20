const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

function buildTestFiles(sourceRoot, environment) {
  if (environment.environment !== 'test' || !/^[\w-]+$/.test(environment.spreadsheetId || '')) {
    throw new Error('Expected a test environment with a spreadsheetId.');
  }
  const context = vm.createContext({});
  vm.runInContext(fs.readFileSync(path.join(sourceRoot, 'Config.gs'), 'utf8'), context);
  const { CONFIG, SHEETS } = vm.runInContext('({CONFIG, SHEETS})', context);
  if (environment.spreadsheetId === CONFIG.SPREADSHEET_ID) {
    throw new Error('Test spreadsheet must differ from production.');
  }
  if (environment.rootFolderId && environment.rootFolderId === CONFIG.ROOT_FOLDER_ID) {
    throw new Error('Test photo folder must differ from production.');
  }
  const config = {
    ...CONFIG,
    ENVIRONMENT: 'test',
    SPREADSHEET_ID: environment.spreadsheetId,
    ROOT_FOLDER_ID: environment.rootFolderId || '',
  };
  const files = Object.fromEntries(fs.readdirSync(sourceRoot)
    .filter((name) => name.endsWith('.gs') || name === 'appsscript.json')
    .map((name) => [name, fs.readFileSync(path.join(sourceRoot, name), 'utf8')]));
  files['Config.gs'] = `const CONFIG = ${JSON.stringify(config, null, 2)};\n\nconst SHEETS = ${JSON.stringify(SHEETS, null, 2)};\n`;
  const manifest = JSON.parse(files['appsscript.json']);
  manifest.webapp = { executeAs: 'USER_DEPLOYING', access: 'ANYONE_ANONYMOUS' };
  files['appsscript.json'] = JSON.stringify(manifest, null, 2) + '\n';
  files['.claspignore'] = '**/**\n!*.gs\n!appsscript.json\n';
  return files;
}

function main(args) {
  if (args.length && (args.length !== 2 || args[0] !== '--script-id' || !/^[\w-]+$/.test(args[1]))) {
    throw new Error('Usage: node scripts/prepare-test-backend.cjs [--script-id SCRIPT_ID]');
  }
  const root = path.resolve(__dirname, '..');
  const environment = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));
  const files = buildTestFiles(path.join(root, 'google-apps-script'), environment);
  const scriptId = args[1] || environment.scriptId;
  if (scriptId) {
    if (!/^[\w-]+$/.test(scriptId)) throw new Error('Invalid test scriptId.');
    files['.clasp.json'] = JSON.stringify({ scriptId, rootDir: '.' }, null, 2) + '\n';
  }
  const destination = path.join(root, '.codex/apps-script-test');
  fs.mkdirSync(destination, { recursive: true });
  const unexpected = fs.readdirSync(destination).filter((name) => !(name in files));
  if (unexpected.length) throw new Error(`Unexpected files in output directory: ${unexpected.join(', ')}. Use a clean destination.`);
  for (const [name, content] of Object.entries(files)) fs.writeFileSync(path.join(destination, name), content);
  console.log(`Prepared ${Object.keys(files).length} test backend files in ${destination}`);
  console.log(`Spreadsheet: ${environment.spreadsheetId}. No files uploaded; no triggers installed.`);
}

module.exports = { buildTestFiles };
if (require.main === module) {
  try { main(process.argv.slice(2)); } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
