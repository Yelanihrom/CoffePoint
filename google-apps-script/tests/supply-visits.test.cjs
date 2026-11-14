const assert = require('node:assert/strict');
const {test} = require('node:test');
const {createHarness} = require('./support.cjs');

const payload = {id: 'visit-1', employeeId: 2, stationId: 7, completedAt: '2026-10-18T10:00:00Z'};
function app() {
  const result = createHarness();
  result.config.CONFIG.ENVIRONMENT = 'test';
  result.sheets.employees.seed([{id: 2, name: 'Denis', role: 'SUPPLY_OPERATOR', isActive: true}]);
  return result;
}
const complete = (a, data = payload) => a.call('POST', {action: 'complete_supply_visit', data});

test('completion atomically creates visit and server-priced earning without changing station', () => {
  const a = app();
  const station = JSON.stringify(a.sheets.coffee_station.rows);
  const result = complete(a, {...payload, amount: 99999});
  assert.equal(result.success, true);
  assert.equal(result.data.earning.amount, 2000);
  assert.match(result.data.earning.id, /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
  assert.equal(result.data.earning.sourceId, payload.id);
  assert.equal(result.data.earning.sourceType, 'SUPPLY_SERVICE');
  assert.equal(result.data.visit.completedAt, '2026-10-18T10:00:00.000Z');
  assert.equal(a.atomicBatches.length, 1);
  assert.equal(a.atomicBatches[0].requests.length, 2);
  assert.equal(JSON.stringify(a.sheets.coffee_station.rows), station);
  assert.equal(a.lock.held, false);
});

test('lost-response retry preserves IDs timestamps and amount without another write', () => {
  const a = app();
  const first = complete(a);
  a.config.CONFIG.SUPPLY_EARNINGS_PER_VISIT = 3000;
  assert.deepEqual(complete(a), first);
  assert.equal(a.atomicBatches.length, 1);
  assert.equal(a.sheets.station_service_visits.getLastRow(), 2);
  assert.equal(a.sheets.employees_transactions.getLastRow(), 3);
});

test('invalid supplier station date identity and wrong authorization cannot create money', () => {
  for (const change of [{employeeId: 1}, {stationId: 999}, {completedAt: '2026-02-30T10:00:00Z'},
    {completedAt: '2999-01-01T00:00:00Z'}, {id: ''}, {employeeId: '2'}]) {
    const a = app();
    assert.equal(complete(a, {...payload, ...change}).success, false);
    assert.equal(a.atomicBatches.length, 0);
  }
  const a = app();
  assert.equal(a.call('POST', {action: 'complete_supply_visit', data: payload}, 'wrong').error.status, 401);
  a.sheets.employees.seed([{id: 2, role: 'SUPPLY_OPERATOR', isActive: false}]);
  assert.equal(complete(a).error.status, 400);
  a.sheets.employees.seed([{id: 2, role: 'OPERATOR', isActive: true}]);
  assert.equal(complete(a).error.status, 400);
  assert.equal(a.atomicBatches.length, 0);
});

test('same visit ID cannot be reassigned to another completion', () => {
  const a = app();
  complete(a);
  assert.equal(complete(a, {...payload, completedAt: '2026-10-18T11:00:00Z'}).error.status, 409);
  assert.equal(a.atomicBatches.length, 1);
});

test('batch failure leaves neither visit nor earning and a retry can succeed', () => {
  const a = app();
  a.context.failAtomicBatch = true;
  assert.equal(complete(a).success, false);
  assert.equal(a.sheets.station_service_visits.getLastRow(), 1);
  assert.equal(a.sheets.employees_transactions.getLastRow(), 2);
  assert.equal(a.lock.held, false);
  a.context.failAtomicBatch = false;
  assert.equal(complete(a).success, true);
});

test('existing unrelated earning ID conflicts instead of being overwritten', () => {
  const a = app();
  const earningId = 'b7d18d52-0c8c-44cb-a13a-94ae52d009c0';
  a.sheets.employees_transactions.seed([{id: earningId, employeeId: 1, type: 'PAYMENT', amount: 500}]);
  assert.equal(complete(a, {...payload, earningId}).error.status, 409);
  assert.equal(a.atomicBatches.length, 0);
});

test('visit-only legacy record is repaired once and selectable full_sync reads visits', () => {
  const a = app();
  a.sheets.station_service_visits.seed([{...payload, createdAt: '2026-10-18T10:01:00Z', updatedAt: '2026-10-18T10:01:00Z'}]);
  assert.equal(complete(a).success, true);
  assert.equal(a.atomicBatches[0].requests.length, 1);
  assert.equal(complete(a).success, true);
  assert.equal(a.atomicBatches.length, 1);
  const result = a.call('GET', {action: 'full_sync', entities: 'station_service_visits', stationId: '7'});
  assert.equal(result.data.stationServiceVisits.length, 1);
  assert.equal(result.data.employeeTransactions.length, 0);
});

test('production completion and retry use the same atomic visit and earning contract', () => {
  const a = app();
  a.config.CONFIG.ENVIRONMENT = 'production';
  const first = complete(a);
  assert.equal(first.success, true);
  assert.equal(first.data.earning.amount, 2000);
  assert.deepEqual(complete(a), first);
  assert.equal(a.atomicBatches.length, 1);
});

test('missing or invalid server rate never creates a visit or earning', () => {
  for (const rate of [undefined, 0, -1, 1.5, '2000']) {
    const a = app();
    a.config.CONFIG.SUPPLY_EARNINGS_PER_VISIT = rate;
    assert.equal(complete(a).error.status, 500);
    assert.equal(a.atomicBatches.length, 0);
  }
});

test('generic upsert cannot create or overwrite a visit earning', () => {
  const a = app();
  const first = complete(a);
  const before = JSON.stringify(a.sheets.employees_transactions.rows);
  const result = a.call('POST', {action: 'upsert', entity: 'employees_transactions',
    data: {...first.data.earning, amount: 1}});
  assert.equal(result.error.status, 403);
  assert.equal(JSON.stringify(a.sheets.employees_transactions.rows), before);
});

test('earning-only record is repaired without a second earning', () => {
  const a = app();
  complete(a);
  const earning = a.sheets.employees_transactions.object(3);
  a.sheets.station_service_visits.seed([]);
  assert.equal(complete(a).success, true);
  assert.equal(a.atomicBatches[1].requests.length, 1);
  assert.deepEqual(a.sheets.employees_transactions.object(3), earning);
  assert.equal(a.sheets.employees_transactions.getLastRow(), 3);
});

test('client UUID is retained and another UUID on retry does not create another earning', () => {
  const a = app();
  const earningId = 'b7d18d52-0c8c-44cb-a13a-94ae52d009c0';
  const first = complete(a, {...payload, earningId});
  assert.equal(first.data.earning.id, earningId);
  assert.deepEqual(complete(a, {...payload, earningId: '88c86f02-2ff6-4684-8832-0b91971ea1af'}), first);
  assert.equal(a.atomicBatches.length, 1);
  assert.equal(complete(a, {...payload, earningId: 'bad-id'}).error.status, 400);
});

test('ordinary upsert without source fields cannot overwrite a server-managed UUID', () => {
  const a = app();
  const first = complete(a);
  assert.equal(a.call('POST', {action: 'upsert', entity: 'employees_transactions', data: {
    id: first.data.earning.id, employeeId: 2, type: 'PAYMENT', amount: 1, updatedAt: '2999-01-01T00:00:00Z'
  }}).error.status, 403);
  assert.equal(a.sheets.employees_transactions.object(3).amount, 2000);
});

test('visit date cells use Moscow date-time serials while API and retry preserve UTC milliseconds', () => {
  const a = app();
  const value = '2026-10-18T23:44:42.399Z';
  const first = complete(a, {...payload, completedAt: value});
  assert.equal(first.success, true);
  const cells = a.atomicBatches[0].requests[0].appendCells.rows[0].values;
  assert.equal(cells[3].userEnteredFormat.numberFormat.pattern, 'yyyy-mm-dd hh:mm:ss');
  const wallClock = new Date(Math.round((cells[3].userEnteredValue.numberValue - 25569) * 86400000));
  assert.equal(wallClock.toISOString(), '2026-10-19T02:44:42.399Z');
  const fullSync = a.call('GET', {action: 'full_sync', entities: 'station_service_visits,employees_transactions'});
  assert.equal(fullSync.data.stationServiceVisits[0].completedAt, value);
  assert.equal(fullSync.data.employeeTransactions[1].operationDate, value);
  assert.deepEqual(complete(a, {...payload, completedAt: value}), first);
});

test('payout local Moscow time and explicit UTC normalize to the same stored instant', () => {
  const a = app();
  const data = {id: 'payment-test', employeeId: 2, type: 'PAYMENT', amount: 500,
    operationDate: '2026-10-19T02:44:42.399', createdAt: '2026-10-18T23:44:42.399Z',
    updatedAt: '2026-10-19T02:44:42.399+03:00'};
  assert.equal(a.call('POST', {action: 'upsert', entity: 'employees_transactions', data}).success, true);
  const saved = a.sheets.employees_transactions.object(3);
  for (const field of ['operationDate', 'createdAt', 'updatedAt']) {
    assert.equal(saved[field].toISOString(), '2026-10-18T23:44:42.399Z');
  }
  assert.deepEqual(a.sheets.employees_transactions.formats.map(f => f.pattern),
    ['yyyy-mm-dd hh:mm:ss', 'yyyy-mm-dd hh:mm:ss', 'yyyy-mm-dd hh:mm:ss']);
  assert.equal(a.call('POST', {action: 'upsert', entity: 'employees_transactions', data: {
    ...data, updatedAt: '2026-10-19T02:44:42.398', amount: 1
  }}).error.status, 409);
});
