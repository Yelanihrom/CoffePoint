const fs = require('node:fs');
const path = require('node:path');

function validateProduction(environment, test) {
  if (environment.environment !== 'production') throw new Error('Expected production environment.');
  for (const field of ['scriptId', 'deploymentId', 'spreadsheetId', 'rootFolderId']) {
    if (!/^[A-Za-z0-9_-]+$/.test(environment[field] || '')) {
      throw new Error(`Production ${field} is not configured.`);
    }
    if (environment[field] === test[field]) throw new Error(`Production ${field} must differ from test.`);
  }
  return {scriptId: environment.scriptId, rootDir: '.'};
}

function validateCommand(args) {
  const names = new Set(['--help', '--version', 'show-authorized-user', 'list-deployments',
    'deployments', 'list-versions', 'versions', 'pull', 'show-file-status', 'status']);
  if (args.length !== 1 || !names.has(args[0])) {
    throw new Error('Production access wrapper permits only one read-only command; publishing is a separate operation.');
  }
}

function main(args) {
  validateCommand(args);
  const root = path.resolve(__dirname, '..');
  const environment = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/production.json'), 'utf8'));
  const test = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));
  const config = validateProduction(environment, test);
  const destination = path.join(root, '.codex/apps-script-production');
  fs.mkdirSync(destination, {recursive: true});
  const file = path.join(destination, '.clasp.json');
  if (fs.existsSync(file) && JSON.parse(fs.readFileSync(file, 'utf8')).scriptId !== config.scriptId) {
    throw new Error('Existing production snapshot belongs to another script; refusing to replace its configuration.');
  }
  fs.writeFileSync(file, JSON.stringify(config, null, 2) + '\n');
  console.log('Prepared production read-only access. No code uploaded, deployments or triggers changed.');
}

module.exports = {validateProduction, validateCommand};
if (require.main === module) {
  try { main(process.argv.slice(2)); }
  catch (error) { console.error(error.message); process.exitCode = 1; }
}
