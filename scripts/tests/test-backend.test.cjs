const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const vm = require('node:vm');
const fs = require('node:fs');
const { buildTestFiles } = require('../prepare-test-backend.cjs');

const root = path.resolve(__dirname, '../..');
const environment = JSON.parse(fs.readFileSync(path.join(root, 'config/environments/test.json'), 'utf8'));
const source = path.join(root, 'google-apps-script');

test('test backend targets the copy and does not include production storage IDs', () => {
  const files = buildTestFiles(source, environment);
  const context = vm.createContext({});
  vm.runInContext(files['Config.gs'], context);
  const config = vm.runInContext('CONFIG', context);
  assert.equal(config.SPREADSHEET_ID, environment.spreadsheetId);
  assert.equal(config.ROOT_FOLDER_ID, '');
  assert.equal(config.ENVIRONMENT, 'test');
  assert.ok(files['Router.gs']);
  assert.ok(files['appsscript.json']);
  assert.ok(!Object.keys(files).some((name) => name.includes('tests/')));
  const production = vm.createContext({});
  vm.runInContext(fs.readFileSync(path.join(source, 'Config.gs'), 'utf8'), production);
  for (const id of vm.runInContext('[CONFIG.SPREADSHEET_ID, CONFIG.ROOT_FOLDER_ID]', production)) {
    assert.ok(!Object.values(files).join('\n').includes(id));
  }
});

test('rejects production storage and a non-test environment', () => {
  const production = vm.createContext({});
  vm.runInContext(fs.readFileSync(path.join(source, 'Config.gs'), 'utf8'), production);
  const config = vm.runInContext('CONFIG', production);
  assert.throws(() => buildTestFiles(source, { ...environment, spreadsheetId: config.SPREADSHEET_ID }));
  assert.throws(() => buildTestFiles(source, { ...environment, rootFolderId: config.ROOT_FOLDER_ID }));
  assert.throws(() => buildTestFiles(source, { ...environment, environment: 'production' }));
});
