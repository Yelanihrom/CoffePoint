const assert = require('node:assert/strict');
const {test} = require('node:test');
const {createHarness} = require('./support.cjs');

const setting = (extra = {}) => ({nodeType: 'MIXERS', intervalDays: 30, salesLimit: 500,
  updatedAt: '2026-11-17T09:00:00.123Z', ...extra});
const write = (app, data) => app.call('POST', {action: 'upsert', entity: 'maintenance_settings', data});

test('maintenance rules are global, selective and read-only on full_sync', () => {
  const app = createHarness();
  const before = JSON.stringify(app.sheets);
  delete app.sheets.coffee_station;
  const result = app.call('GET', {action: 'full_sync', entities: 'maintenance_settings', stationId: '999'});
  assert.equal(result.success, true);
  assert.equal(result.data.maintenanceSettings.length, 6);
  assert.equal(result.data.maintenanceSettings.find(row => row.nodeType === 'MIXERS').salesLimit, 0);
  assert.equal(JSON.stringify(app.sheets.maintenance_settings), JSON.stringify(JSON.parse(before).maintenance_settings));
  assert.equal(app.requests.length, 0);
});

test('upsert updates one rule, repeats do not add rows, zero disables sales threshold', () => {
  const app = createHarness();
  const before = JSON.stringify(app.sheets.station_maintenance);
  for (const salesLimit of [500, 500, 0, Number.MAX_SAFE_INTEGER]) {
    assert.equal(write(app, setting({salesLimit})).success, true);
    assert.equal(app.sheets.maintenance_settings.getLastRow(), 7);
    const result = app.call('GET', {action: 'full_sync', entities: 'maintenance_settings'});
    const row = result.data.maintenanceSettings.find(row => row.nodeType === 'MIXERS');
    assert.equal(row.salesLimit, salesLimit);
    assert.equal(row.updatedAt, setting().updatedAt);
  }
  assert.equal(JSON.stringify(app.sheets.station_maintenance), before);
  assert.equal(app.lock.held, false);
});

test('invalid rules and missing timestamps never change stored data', () => {
  for (const extra of [{nodeType: 'UNKNOWN'}, {intervalDays: 0}, {intervalDays: -1},
    {intervalDays: 1.5}, {intervalDays: '30'}, {intervalDays: 2147483648},
    {salesLimit: -1}, {salesLimit: 1.5}, {salesLimit: '500'}, {salesLimit: null},
    {salesLimit: 9007199254740992}, {updatedAt: ''}, {updatedAt: '17.11.2026'},
    {stationId: 7}]) {
    const app = createHarness();
    const before = JSON.stringify(app.sheets);
    assert.equal(write(app, setting(extra)).error.status, 400, JSON.stringify(extra));
    assert.equal(JSON.stringify(app.sheets), before);
    assert.equal(app.lock.held, false);
  }
});

test('older settings conflict, unauthorized writes and duplicate rows are rejected', () => {
  const app = createHarness();
  assert.equal(write(app, setting()).success, true);
  const before = JSON.stringify(app.sheets);
  assert.equal(write(app, setting({updatedAt: '2026-11-16T00:00:00Z'})).error.status, 409);
  assert.equal(app.call('POST', {action: 'upsert', entity: 'maintenance_settings', data: setting()}, 'wrong').error.status, 401);
  assert.equal(JSON.stringify(app.sheets), before);
  app.sheets.maintenance_settings.seed([setting(), setting()]);
  assert.equal(app.call('GET', {action: 'full_sync', entities: 'maintenance_settings'}).error.status, 500);
  assert.equal(write(app, setting()).error.status, 409);
});

test('batch validates every rule before writing any of them', () => {
  const app = createHarness();
  const before = JSON.stringify(app.sheets);
  const request = items => app.call('POST', {action: 'upsert_batch', entity: 'maintenance_settings', data: {items}});
  assert.equal(request([setting(), setting({nodeType: 'BREW_UNIT', intervalDays: 0})]).error.status, 400);
  assert.equal(JSON.stringify(app.sheets), before);
  assert.equal(request([setting(), setting()]).error.status, 400);
  assert.equal(request([setting(), setting({nodeType: 'BREW_UNIT'})]).success, true);
  app.sheets.maintenance_settings.seed([setting({salesLimit: -1})]);
  assert.equal(app.call('GET', {action: 'full_sync', entities: 'maintenance_settings'}).error.status, 500);
});
