const assert = require('node:assert/strict');
const {test} = require('node:test');
const {createHarness} = require('./support.cjs');

const action = {action: 'upsert', entity: 'notification_settings'};
const stamp = '2026-11-14T08:15:30.399Z';

test('notification settings selective read is independent of station and employee sheets', () => {
  const app = createHarness();
  delete app.sheets.coffee_station;
  delete app.sheets.employees;
  const result = app.call('GET', {action: 'full_sync', entities: 'notification_settings', stationId: '7'});
  assert.equal(result.success, true);
  assert.equal(result.data.notificationSettings[0].role, 'SUPPLY_OPERATOR');
  assert.equal(result.data.notificationSettings[0].warehouseEnabled, false);
  assert.deepEqual(result.data.stations, []);
});

test('partial update preserves other toggles, UTC milliseconds and row identity on retry', () => {
  const app = createHarness();
  const data = {role: 'SUPPLY_OPERATOR', warehouseEnabled: true, updatedAt: stamp};
  assert.equal(app.call('POST', {...action, data}).success, true);
  assert.equal(app.call('POST', {...action, data}).success, true);
  assert.equal(app.sheets.notification_settings.rows.length, 2);
  const row = app.sheets.notification_settings.object();
  assert.equal(row.warehouseEnabled, true);
  assert.equal(row.criticalStockEnabled, true);
  assert.equal(row.brewUnitEnabled, false);
  assert.equal(row.updatedAt.toISOString(), stamp);
  assert.equal(app.call('GET', {action: 'full_sync', entities: 'notification_settings'})
    .data.notificationSettings[0].updatedAt, stamp);
  assert.equal(app.call('POST', {...action, data: {...data, updatedAt: '2026-11-14T08:15:30.398Z'}})
    .error.status, 409);
});

test('rejects invalid flags, unsupported roles and invalid timestamps without changing cells', () => {
  for (const patch of [
    {warehouseEnabled: 'false'}, {warehouseEnabled: 0}, {role: 'UNKNOWN'},
    {refillEnabled: true}, {updatedAt: 'bad'}, {updatedAt: ''},
    {updatedAt: '2026-11-14T08:15:30'},
  ]) {
    const app = createHarness();
    const before = JSON.stringify(app.sheets.notification_settings.rows);
    const result = app.call('POST', {...action, data: {role: 'SUPPLY_OPERATOR', updatedAt: stamp, ...patch}});
    assert.equal(result.error.status, 400);
    assert.equal(JSON.stringify(app.sheets.notification_settings.rows), before);
  }
});

test('missing flags cannot create an incomplete role and wrong token cannot write', () => {
  const app = createHarness();
  assert.equal(app.call('POST', {...action, data: {role: 'MANAGER', updatedAt: stamp}}).error.status, 400);
  assert.equal(app.call('POST', {...action, data: {role: 'SUPPLY_OPERATOR', warehouseEnabled: true,
    updatedAt: stamp}}, 'wrong').error.status, 401);
});

test('manual duplicate roles and non-checkbox values are rejected on read', () => {
  const app = createHarness();
  const row = app.sheets.notification_settings.object();
  app.sheets.notification_settings.seed([row, row]);
  assert.equal(app.call('GET', {action: 'full_sync', entities: 'notification_settings'}).error.status, 500);
  app.sheets.notification_settings.seed([{...row, warehouseEnabled: 'false'}]);
  assert.equal(app.call('GET', {action: 'full_sync', entities: 'notification_settings'}).error.status, 500);
});
