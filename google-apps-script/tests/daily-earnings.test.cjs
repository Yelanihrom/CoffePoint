const assert = require('node:assert/strict');
const {test} = require('node:test');
const {createHarness} = require('./support.cjs');

function report(app, count = 12) {
  app.responses.push({status: 200, body: {success: true, items_count: 2,
    items: [{terminal_id: 258576, incoming_count: count}, {terminal_id: 999999, incoming_count: 100}]}});
}

function yesterday(app) {
  app.context.getDailyEarningsToday_ = () => '2026-11-16';
  app.context.recordYesterdayDailyEarnings();
}

function transactions(app) {
  return app.sheets.employees_transactions.rows.slice(1).map((_, i) => app.sheets.employees_transactions.object(i + 2));
}

test('daily trigger creates operator earning from the real Vendista report contract alongside supplier earnings', () => {
  const app = createHarness();
  app.sheets.employees.seed([
    {id: 1, name: 'Operator', role: 'OPERATOR', isActive: true, workStartDate: '2026-09-13'},
    {id: 2, name: 'Supplier', role: 'SUPPLY_OPERATOR', isActive: true},
  ]);
  const supplier = {id: 'supplier-uuid', employeeId: 2, type: 'EARNING', amount: 2000,
    operationDate: '2026-11-15T10:00:00Z', sourceType: 'SUPPLY_SERVICE', sourceId: 'visit-uuid'};
  app.sheets.employees_transactions.seed([supplier]);
  const before = JSON.stringify(app.sheets.employees_transactions.rows[1]);
  report(app);
  yesterday(app);
  const rows = transactions(app);
  assert.equal(rows.length, 2);
  const earning = rows.find(row => row.id === 'AUTO-EARNING-1-2026-11-15');
  assert.equal(earning.employeeId, 1);
  assert.equal(earning.type, 'EARNING');
  assert.equal(earning.salesCount, 12);
  assert.equal(earning.amount, 180);
  assert.equal(earning.periodFrom, '2026-11-15');
  assert.equal(earning.periodTo, '2026-11-15');
  assert.equal(JSON.stringify(app.sheets.employees_transactions.rows[1]), before);
  const url = new URL(app.requests[0].url);
  assert.equal(url.searchParams.get('DateFrom'), '2026-11-15 00:00:00');
  assert.equal(url.searchParams.get('DateTo'), '2026-11-15 23:59:59');
});

test('repeated daily trigger updates the same operator record without duplicates', () => {
  const app = createHarness();
  app.sheets.employees_transactions.seed([]);
  report(app);
  yesterday(app);
  report(app, 13);
  yesterday(app);
  const rows = transactions(app);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].id, 'AUTO-EARNING-1-2026-11-15');
  assert.equal(rows[0].amount, 195);
});

test('daily calculation writes nothing on upstream failure or incomplete report', () => {
  for (const response of [
    {status: 400, body: {success: false}},
    {status: 200, body: {success: true, items_count: 2, items: [{terminal_id: 258576, incoming_count: 12}]}},
  ]) {
    const app = createHarness();
    const before = JSON.stringify(app.sheets.employees_transactions.rows);
    app.responses.push(response);
    assert.throws(() => yesterday(app));
    assert.equal(JSON.stringify(app.sheets.employees_transactions.rows), before);
  }
});

test('zero sales produce a zero earning only for an eligible assigned operator', () => {
  const app = createHarness();
  app.sheets.employees_transactions.seed([]);
  app.responses.push({status: 200, body: {success: true, items: []}});
  yesterday(app);
  assert.equal(transactions(app)[0].amount, 0);
  assert.equal(transactions(app)[0].salesCount, 0);
});
