const assert = require('node:assert/strict');
const {test} = require('node:test');
const {validateProduction, validateCommand} = require('../prepare-production-access.cjs');

const environment = {environment: 'production', scriptId: 'prod-script', deploymentId: 'prod-deployment',
  spreadsheetId: 'prod-sheet', rootFolderId: 'prod-folder'};
const testEnvironment = {environment: 'test', scriptId: 'test-script', deploymentId: 'test-deployment',
  spreadsheetId: 'test-sheet', rootFolderId: ''};

test('production access requires explicit IDs distinct from test', () => {
  assert.deepEqual(validateProduction(environment, testEnvironment), {scriptId: 'prod-script', rootDir: '.'});
  assert.throws(() => validateProduction({...environment, environment: 'test'}, testEnvironment));
  for (const field of ['scriptId', 'deploymentId', 'spreadsheetId', 'rootFolderId']) {
    assert.throws(() => validateProduction({...environment, [field]: ''}, testEnvironment));
    assert.throws(() => validateProduction({...environment, [field]: testEnvironment[field]}, testEnvironment));
  }
});

test('production wrapper cannot upload deploy execute delete or override the target', () => {
  for (const command of ['list-deployments', 'list-versions', 'pull', '--help', 'show-authorized-user']) {
    assert.doesNotThrow(() => validateCommand([command]));
  }
  for (const args of [[], ['push'], ['deploy'], ['redeploy'], ['version'], ['run'],
    ['delete'], ['undeploy'], ['clone'], ['list-deployments', 'another-script'],
    ['--project', 'other.json', 'pull'], ['pull', '--force']]) {
    assert.throws(() => validateCommand(args));
  }
});
