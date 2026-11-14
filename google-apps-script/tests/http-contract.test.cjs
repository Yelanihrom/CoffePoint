const assert = require('node:assert/strict');
const {test} = require('node:test');
const {createHarness, schema} = require('./support.cjs');

const oldTimestamp = '2026-10-18T10:07:00';
const newTimestamp = '2026-10-19T10:07:00';
const warehouse = (itemType, quantity, updatedAt = newTimestamp) =>
  ({itemType, quantity, minQuantity: 2, maxQuantity: 12, updatedAt});

test('analytics maps one report to stations and leaves all stored data unchanged', () => {
  const app = createHarness();
  const station = app.sheets.coffee_station.object();
  app.sheets.coffee_station.seed([station,
    {...station, id: 8, name: 'Second', terminalId: 111},
    {...station, id: 9, name: 'Unconfigured', terminalId: ''}]);
  const before = JSON.stringify(app.sheets);
  app.responses.push({status: 200, body: {success: true, items: [
    {terminal_id: station.terminalId, incoming_count: 12},
    {terminal_id: 999, incoming_count: 1000},
  ]}});
  const result = app.call('GET', {action: 'sales_analytics', startDate: '2026-09-01'});
  assert.equal(result.success, true);
  assert.equal(result.data.endDate, null);
  assert.equal(result.data.totalSales, 12);
  assert.deepEqual(result.data.stations.map(s => s.salesCount), [12, 0, null]);
  assert.equal(app.requests.length, 1);
  const url = new URL(app.requests[0].url);
  assert.equal(url.searchParams.get('DateFrom'), '2026-09-01 00:00:00');
  assert.equal(url.searchParams.has('DateTo'), false);
  assert.equal(JSON.stringify(app.sheets), before);
});

test('analytics explicit end date includes the selected Moscow day', () => {
  const app = createHarness();
  app.responses.push({status: 200, body: {success: true, items: []}});
  const result = app.call('GET', {action: 'sales_analytics', startDate: '2026-09-01', endDate: '2026-09-02'});
  assert.equal(result.success, true);
  assert.equal(result.data.totalSales, 0);
  assert.equal(new URL(app.requests[0].url).searchParams.get('DateTo'), '2026-09-02 23:59:59');
});

test('analytics rejects invalid periods and unauthorized access before Vendista request', () => {
  for (const request of [
    {startDate: '2026-02-30'}, {startDate: 'bad'}, {},
    {startDate: '2026-09-02', endDate: '2026-09-01'},
    {startDate: '2999-01-01'}, {startDate: '2026-09-01', endDate: '2999-01-01'},
  ]) {
    const app = createHarness();
    assert.equal(app.call('GET', {action: 'sales_analytics', ...request}).error.status, 400);
    assert.equal(app.requests.length, 0);
  }
  const app = createHarness();
  assert.equal(app.call('GET', {action: 'sales_analytics', startDate: '2026-09-01'}, 'wrong').error.status, 401);
  assert.equal(app.requests.length, 0);
});

test('analytics handles empty stations without Vendista and rejects duplicate terminals', () => {
  const app = createHarness();
  const station = app.sheets.coffee_station.object();
  app.sheets.coffee_station.seed([]);
  assert.equal(app.call('GET', {action: 'sales_analytics', startDate: '2026-09-01'}).data.totalSales, 0);
  assert.equal(app.requests.length, 0);
  app.sheets.coffee_station.seed([station, {...station, id: 9}]);
  assert.equal(app.call('GET', {action: 'sales_analytics', startDate: '2026-09-01'}).error.status, 500);
  assert.equal(app.requests.length, 0);
});

test('analytics does not return a partial total on upstream failure', () => {
  const app = createHarness();
  app.responses.push({status: 403, body: {success: false}});
  const result = app.call('GET', {action: 'sales_analytics', startDate: '2026-09-01'});
  assert.equal(result.success, false);
  assert.equal(result.data, undefined);
});

for (const [name, expected] of Object.entries(schema)) {
  test(`Config schema matches the independent contract for ${name}`, () => {
    const {config} = createHarness();
    const actual = Object.values(config.SHEETS).find(sheet => sheet.name === name);
    assert.ok(actual, `Missing sheet: ${name}`);
    assert.equal(actual.key, expected.key);
    assert.deepEqual(Array.from(actual.headers), expected.headers);
  });
}

test('authorization reads the token from Script Properties', () => {
  const app = createHarness();
  assert.equal(app.config.CONFIG.SYNC_TOKEN, undefined);
  assert.equal(app.call('GET', {action: 'full_sync'}).success, true);
});

test('missing server token returns a configuration error', () => {
  const app = createHarness({});
  const result = app.call('GET', {action: 'full_sync'});
  assert.equal(result.success, false);
  assert.equal(result.error.status, 500);
  assert.match(result.error.message, /SYNC_TOKEN/);
});

test('wrong token blocks writes with 401', () => {
  const app = createHarness();
  const before = structuredClone(app.sheets.warehouse_stock.rows);
  const result = app.call('POST', {action: 'upsert', entity: 'warehouse_stock',
    data: warehouse('milk', 9)}, 'wrong-token');
  assert.equal(result.error.status, 401);
  assert.deepEqual(app.sheets.warehouse_stock.rows, before);
});

test('full_sync without entities returns all response collections', () => {
  const result = createHarness().call('GET', {action: 'full_sync'});
  assert.equal(result.success, true);
  assert.deepEqual(Object.keys(result.data).sort(), Object.values(schema).map(s => s.responseKey).sort());
  for (const [key, rows] of Object.entries(result.data)) {
    assert.equal(rows.length, key === 'stationServiceVisits' ? 0 : 1);
  }
  assert.equal(result.data.stations[0].salesAfterService, 54);
  assert.equal(result.data.warehouseStock[0].minQuantity, 24);
  assert.equal(result.data.warehouseStock[0].maxQuantity, 48);
});

test('selective full_sync does not access unrelated sheets', () => {
  const app = createHarness();
  delete app.sheets.coffee_station;
  const result = app.call('GET', {action: 'full_sync', entities: ' warehouse_stock, '});
  assert.equal(result.success, true);
  assert.equal(result.data.warehouseStock.length, 1);
  assert.equal(result.data.stations.length, 0);
});

test('service start filters one station and does not read settings or warehouse for operator', () => {
  const app = createHarness();
  const station = app.sheets.coffee_station.object();
  const inventory = app.sheets.station_inventory.object();
  app.sheets.coffee_station.seed([station, {...station, id: 99}]);
  app.sheets.station_inventory.seed([inventory, {...inventory, stationId: 99}]);
  delete app.sheets.station_settings;
  delete app.sheets.warehouse_stock;
  const result = app.call('GET', {action: 'full_sync', stationId: String(station.id),
    entities: 'coffee_station,station_inventory'});
  assert.equal(result.success, true);
  assert.deepEqual(result.data.stations.map(row => row.id), [station.id]);
  assert.deepEqual(result.data.inventories.map(row => row.stationId), [station.id]);
  assert.deepEqual(result.data.settings, []);
  assert.deepEqual(result.data.warehouseStock, []);
});

test('supplier service start includes warehouse independently of station filter', () => {
  const app = createHarness();
  delete app.sheets.station_settings;
  const result = app.call('GET', {action: 'full_sync', stationId: '7',
    entities: 'coffee_station,station_inventory,warehouse_stock'});
  assert.equal(result.success, true);
  assert.equal(result.data.warehouseStock.length, 1);
  assert.deepEqual(result.data.settings, []);
});

test('invalid station filter fails instead of returning all stations', () => {
  for (const stationId of ['0', '-1', 'abc', '7,8']) {
    const result = createHarness().call('GET', {action: 'full_sync', stationId});
    assert.equal(result.error.status, 400);
  }
});

test('warehouse header mismatch is reported without rewriting headers', () => {
  const app = createHarness();
  app.sheets.warehouse_stock.rows[0][2] = 'outdatedMinimum';
  const result = app.call('GET', {action: 'full_sync', entities: 'warehouse_stock'});
  assert.equal(result.error.status, 500);
  assert.equal(result.error.message, 'Sheet headers mismatch: warehouse_stock');
  assert.equal(app.sheets.warehouse_stock.rows[0][2], 'outdatedMinimum');
});

test('upsert preserves unrelated fields and uses the incoming local timestamp', () => {
  const app = createHarness();
  const result = app.call('POST', {action: 'upsert', entity: 'warehouse_stock',
    data: {itemType: 'milk', quantity: 40, updatedAt: newTimestamp}});
  assert.equal(result.success, true);
  assert.deepEqual(app.sheets.warehouse_stock.object(),
    {itemType: 'milk', quantity: 40, minQuantity: 24, maxQuantity: 48, updatedAt: newTimestamp});
});

test('warehouse UTC timestamp is displayed in Moscow without changing the instant', () => {
  const app = createHarness();
  const utc = '2026-10-22T06:38:06.664Z';
  const result = app.call('POST', {action: 'upsert', entity: 'warehouse_stock',
    data: {itemType: 'milk', quantity: 40, updatedAt: utc}});
  assert.equal(result.success, true);
  const saved = app.sheets.warehouse_stock.object();
  assert.equal(saved.updatedAt.toISOString(), utc);
  assert.equal(saved.updatedAt.getTime(), Date.parse(utc));
  assert.deepEqual(app.sheets.warehouse_stock.formats,
    [{row: 2, column: 5, height: 1, width: 1, pattern: 'dd.MM.yyyy HH:mm'}]);
  assert.equal(saved.minQuantity, 24);
  assert.equal(saved.maxQuantity, 48);
  const sync = app.call('GET', {action: 'full_sync', entities: 'warehouse_stock'});
  assert.equal(sync.data.warehouseStock[0].updatedAt, utc);
  const stale = app.call('POST', {action: 'upsert', entity: 'warehouse_stock',
    data: {itemType: 'milk', quantity: 1, updatedAt: '2026-10-22T06:38:05.664Z'}});
  assert.equal(stale.error.status, 409);
  assert.equal(app.sheets.warehouse_stock.object().quantity, 40);
});

test('warehouse Moscow offset is not shifted a second time', () => {
  const app = createHarness();
  const moscow = '2026-10-22T09:38:06.664+03:00';
  assert.equal(app.call('POST', {action: 'upsert', entity: 'warehouse_stock',
    data: warehouse('milk', 40, moscow)}).success, true);
  assert.equal(app.sheets.warehouse_stock.object().updatedAt.toISOString(), '2026-10-22T06:38:06.664Z');
});

test('an older upsert returns 409 and leaves the row intact', () => {
  const app = createHarness();
  const before = structuredClone(app.sheets.warehouse_stock.rows);
  const result = app.call('POST', {action: 'upsert', entity: 'warehouse_stock',
    data: warehouse('milk', 9, '2026-10-17T00:00:00')});
  assert.equal(result.error.status, 409);
  assert.deepEqual(app.sheets.warehouse_stock.rows, before);
  assert.equal(app.lock.held, false);
});

test('upsert requires a key and updatedAt', () => {
  for (const data of [{quantity: 1, updatedAt: newTimestamp}, {itemType: 'milk', quantity: 1}]) {
    const result = createHarness().call('POST', {action: 'upsert', entity: 'warehouse_stock', data});
    assert.equal(result.error.status, 400);
  }
});

test('batch upsert updates existing and appends new warehouse rows', () => {
  const app = createHarness();
  const result = app.call('POST', {action: 'upsert_batch', entity: 'warehouse_stock',
    data: {items: [warehouse('milk', 10), warehouse('coffee', 6)]}});
  assert.equal(result.success, true);
  assert.equal(result.data.updatedCount, 2);
  assert.equal(app.sheets.warehouse_stock.object().quantity, 10);
  assert.equal(app.sheets.warehouse_stock.object(3).itemType, 'coffee');
});

test('a conflicting batch item prevents all data rows in the batch from being written', () => {
  const app = createHarness();
  const before = structuredClone(app.sheets.warehouse_stock.rows);
  const result = app.call('POST', {action: 'upsert_batch', entity: 'warehouse_stock',
    data: {items: [warehouse('coffee', 6), warehouse('milk', 10, '2026-10-17T00:00:00')]}});
  assert.equal(result.error.status, 409);
  assert.deepEqual(app.sheets.warehouse_stock.rows, before);
});

test('duplicate keys and empty batches are rejected before writing data', () => {
  for (const items of [[], [warehouse('milk', 1), warehouse('milk', 2)]]) {
    const app = createHarness();
    const before = structuredClone(app.sheets.warehouse_stock.rows);
    const result = app.call('POST', {action: 'upsert_batch', entity: 'warehouse_stock', data: {items}});
    assert.equal(result.error.status, 400);
    assert.deepEqual(app.sheets.warehouse_stock.rows, before);
  }
});

test('operator completion changes exactly the three service and sales columns', () => {
  const app = createHarness();
  const before = app.sheets.coffee_station.object();
  const result = app.call('POST', {action: 'complete_service', entity: 'coffee_station',
    data: {id: 7, lastServiceDate: newTimestamp, salesAfterService: 0, salesUpdatedAt: newTimestamp}});
  assert.equal(result.success, true);
  assert.deepEqual(app.sheets.coffee_station.object(), {...before,
    lastServiceDate: newTimestamp, salesAfterService: 0, salesUpdatedAt: newTimestamp});
  assert.equal(app.lock.held, false);
});

test('supplier completion changes only the selected maintenance nodes', () => {
  const app = createHarness();
  const before = app.sheets.coffee_station.object();
  const result = app.call('POST', {action: 'complete_service', entity: 'coffee_station',
    data: {id: 7, mixersServiceDate: '2026-10-19', liquidWasteContainerServiceDate: '2026-10-19'}});
  assert.equal(result.success, true);
  assert.deepEqual(app.sheets.coffee_station.object(), {...before,
    mixersServiceDate: '2026-10-19', liquidWasteContainerServiceDate: '2026-10-19'});
});

test('Russian service dates keep day month and full year before generic date parsing', () => {
  const app = createHarness();
  for (let day = 1; day <= 12; day += 1) {
    const text = `${String(day).padStart(2, '0')}.11.2026`;
    for (const suffix of ['', ' 10:03', ' 10:03:45']) {
      const date = app.context.parseSyncTimestamp(text + suffix);
      assert.equal(date.getFullYear(), 2026, text + suffix);
      assert.equal(date.getMonth(), 10, text + suffix);
      assert.equal(date.getDate(), day, text + suffix);
      assert.equal(date.getHours(), suffix ? 10 : 0);
      assert.equal(date.getMinutes(), suffix ? 3 : 0);
      assert.equal(date.getSeconds(), suffix.endsWith(':45') ? 45 : 0);
    }
  }
});

test('timestamp parsing preserves ISO offsets and truncates only fractional seconds', () => {
  const app = createHarness();
  for (const text of [
    '2026-11-09T10:02:27.259123456Z',
    '2026-11-09T13:02:27.259123456+03:00',
    '2026-11-09 13:02:27.259123456+03:00',
  ]) {
    assert.equal(app.context.parseSyncTimestamp(text).toISOString(), '2026-11-09T10:02:27.259Z');
  }
  const date = new Date('2026-11-09T10:02:27.259Z');
  assert.equal(app.context.parseSyncTimestamp(date), date);
  assert.equal(app.context.parseSyncTimestamp('2026-11-09').toISOString(), '2026-11-09T00:00:00.000Z');
});

test('supplier November 9 completion replaces September 27 in text Date and ISO cells', () => {
  for (const current of ['27.09.2026', new Date('2026-09-27T00:00:00+03:00'),
    '2026-09-27T00:00:00+03:00']) {
    const app = createHarness();
    const before = {...app.sheets.coffee_station.object(), mixersServiceDate: current};
    app.sheets.coffee_station.seed([before]);
    const request = {action: 'complete_service', entity: 'coffee_station',
      data: {id: before.id, mixersServiceDate: '09.11.2026'}};
    assert.equal(app.call('POST', request).success, true);
    assert.deepEqual(app.sheets.coffee_station.object(), {...before, mixersServiceDate: '09.11.2026'});
    assert.equal(app.call('POST', request).success, true, 'Same date retry must remain safe');
  }
});

test('supplier completion still rejects a genuinely newer mixer date', () => {
  for (const current of ['10.11.2026', new Date('2026-11-10T00:00:00+03:00'),
    '2026-11-10T00:00:00+03:00']) {
    const app = createHarness();
    const before = {...app.sheets.coffee_station.object(), mixersServiceDate: current};
    app.sheets.coffee_station.seed([before]);
    const result = app.call('POST', {action: 'complete_service', entity: 'coffee_station',
      data: {id: before.id, mixersServiceDate: '09.11.2026'}});
    assert.equal(result.error.status, 409);
    assert.deepEqual(app.sheets.coffee_station.object(), before);
    assert.equal(app.lock.held, false);
  }
});

test('stale operator completion is rejected without resetting sales', () => {
  const app = createHarness();
  const before = structuredClone(app.sheets.coffee_station.rows);
  const result = app.call('POST', {action: 'complete_service', entity: 'coffee_station',
    data: {id: 7, lastServiceDate: '2026-10-17T00:00:00', salesAfterService: 0,
      salesUpdatedAt: '2026-10-17T00:00:00'}});
  assert.equal(result.error.status, 409);
  assert.deepEqual(app.sheets.coffee_station.rows, before);
});

test('station upsert preserves newer maintenance dates and server sales', () => {
  const app = createHarness();
  const result = app.call('POST', {action: 'upsert', entity: 'coffee_station',
    data: {id: 7, name: 'Renamed', lastServiceDate: '2026-10-17T00:00:00', updatedAt: newTimestamp}});
  assert.equal(result.success, true);
  const station = app.sheets.coffee_station.object();
  assert.equal(station.name, 'Renamed');
  assert.equal(station.lastServiceDate, oldTimestamp);
  assert.equal(station.salesAfterService, 54);
});

test('sales refresh keeps unrelated station fields and sets an integer format', () => {
  const app = createHarness();
  const before = app.sheets.coffee_station.object();
  assert.equal(app.context.updateStationSalesFields_({id: 7, salesAfterService: 63,
    salesUpdatedAt: newTimestamp, expectedLastServiceDate: '2026-10-18 10:07:00'}), true);
  assert.deepEqual(app.sheets.coffee_station.object(), {...before,
    salesAfterService: 63, salesUpdatedAt: newTimestamp});
  assert.deepEqual(app.sheets.coffee_station.formats,
    [{row: 2, column: 9, height: 1, width: 1, pattern: '0'}]);
});

test('sales calculated before a newer service are not written', () => {
  const app = createHarness();
  const before = structuredClone(app.sheets.coffee_station.rows);
  assert.equal(app.context.updateStationSalesFields_({id: 7, salesAfterService: 99,
    salesUpdatedAt: newTimestamp, expectedLastServiceDate: '2026-10-17 10:07:00'}), false);
  assert.deepEqual(app.sheets.coffee_station.rows, before);
});

test('missing service fields return 400 and unknown stations return 404', () => {
  const app = createHarness();
  assert.equal(app.call('POST', {action: 'complete_service', data: {id: 7}}).error.status, 400);
  assert.equal(app.call('POST', {action: 'complete_service', data: {
    id: 9999, lastServiceDate: newTimestamp, salesAfterService: 0, salesUpdatedAt: newTimestamp,
  }}).error.status, 404);
});

test('photo route forwards payload to photo storage', () => {
  const app = createHarness();
  app.context.uploadStationPhotos = data => ({machinePhotoUrl: `test-photo-${data.stationId}`});
  const result = app.call('POST', {action: 'upload_photos', data: {stationId: 7}});
  assert.equal(result.success, true);
  assert.equal(result.data.machinePhotoUrl, 'test-photo-7');
});

test('unknown actions and using GET for writes return 400', () => {
  const app = createHarness();
  assert.equal(app.call('POST', {action: 'unknown'}).error.status, 400);
  assert.equal(app.call('GET', {action: 'upsert'}).error.status, 400);
});

test.todo('Android sends POST delete supply_catalog, but Router.gs has no delete handler');

test('Vendista server request uses Script Properties and explicit date boundaries', () => {
  const app = createHarness();
  app.responses.push({status: 200, body: {success: true, items_count: 1,
    items: [{terminal_id: 258576, incoming_count: 12}]}});
  const result = app.context.fetchVendistaSales_('2026-10-18 00:00:00', '2026-10-18 23:59:59', 'test');
  const url = new URL(app.requests[0].url);
  assert.equal(url.pathname, '/reports/common');
  assert.equal(url.searchParams.get('token'), 'test-vendista-token');
  assert.equal(url.searchParams.get('DateFrom'), '2026-10-18 00:00:00');
  assert.equal(url.searchParams.get('DateTo'), '2026-10-18 23:59:59');
  assert.equal(result.salesByTerminal['258576'], 12);
});

test('Vendista 429 is retried before using a successful report', () => {
  const app = createHarness();
  app.responses.push({status: 429, body: {success: false}},
    {status: 200, body: {success: true, items: []}});
  app.context.fetchVendistaSales_(newTimestamp, null, 'test');
  assert.deepEqual(app.sleeps, [10000]);
  assert.equal(app.requests.length, 2);
});

test('Vendista paginated response is rejected rather than saving partial sales', () => {
  const app = createHarness();
  app.responses.push({status: 200, body: {success: true, items_count: 2,
    items: [{terminal_id: 258576, incoming_count: 12}]}});
  assert.throws(() => app.context.fetchVendistaSales_(newTimestamp, null, 'test'), /paginated/);
});

test('Vendista missing token stops execution before any external request', () => {
  const app = createHarness({SYNC_TOKEN: 'test-token'});
  assert.throws(() => app.context.fetchVendistaSales_(newTimestamp, null, 'test'), /VENDISTA_TOKEN/);
  assert.equal(app.requests.length, 0);
});
