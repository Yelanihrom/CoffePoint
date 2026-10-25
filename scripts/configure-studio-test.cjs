const fs = require('node:fs');
const path = require('node:path');
const { buildTestFiles } = require('./prepare-test-backend.cjs');

function androidTestProperties(environment, token) {
  if (environment.environment !== 'test') throw new Error('Only the test environment is allowed');
  if (!/^[A-Za-z0-9_-]+$/.test(environment.deploymentId || '')) throw new Error('Test deployment ID is required');
  if (!token || !token.trim() || /[\r\n\\]/.test(token.trim())) throw new Error('A valid test token is required');
  return '# Generated local test settings. Never commit this file.\n' +
    `webAppUrl=https://script.google.com/macros/s/${environment.deploymentId}/exec\n` +
    `syncToken=${token.trim()}\n`;
}

function configure(root) {
  const environment = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));
  // Reuse the server generator's protection against production storage IDs.
  buildTestFiles(path.join(root, 'google-apps-script'), environment);
  const token = fs.readFileSync(path.join(root, '.codex/test-secrets/sync-token.txt'), 'utf8');
  const properties = androidTestProperties(environment, token);
  fs.writeFileSync(path.join(root, 'test-backend.properties'), properties, { mode: 0o600 });
}

if (require.main === module) {
  configure(path.resolve(__dirname, '..'));
  console.log('Android Studio test backend configured. Sync Gradle, then run the debug app.');
}

module.exports = { androidTestProperties };
