const assert = require('node:assert/strict');
const {test} = require('node:test');
const {createHarness} = require('./support.cjs');

const node = (overrides = {}) => ({stationId:7, nodeType:'MIXERS', lastServiceDate:'2026-11-15T10:00:00',
  salesAfterService:0, salesUpdatedAt:'', updatedAt:'2026-11-15T10:00:00', ...overrides});
const report = (app, count) => app.responses.push({status:200, body:{success:true,
  items:[{terminal_id:258576, incoming_count:count}]}});

test('daily maintenance trigger targets Moscow 02 and replaces only its own schedules', () => {
  const app = createHarness();
  const triggers = ['refreshMaintenanceSalesDaily','recordYesterdayDailyEarnings','refreshStationSalesEveryTenMinutes']
    .map(handler => ({getHandlerFunction: () => handler}));
  const options = {};
  const builder = {};
  for (const name of ['timeBased','atHour','nearMinute','everyDays','inTimezone']) {
    builder[name] = value => {options[name] = value; return builder;};
  }
  builder.create = () => triggers.push({getHandlerFunction: () => options.handler});
  app.context.ScriptApp = {getProjectTriggers: () => [...triggers],
    deleteTrigger: trigger => triggers.splice(triggers.indexOf(trigger),1),
    newTrigger: handler => {options.handler = handler; return builder;}};
  app.context.installDailyMaintenanceSalesTrigger();
  app.context.installDailyMaintenanceSalesTrigger();
  assert.equal(triggers.length,3);
  assert.equal(options.atHour,2);
  assert.equal(options.nearMinute,0);
  assert.equal(options.everyDays,1);
  assert.equal(options.inTimezone,'Europe/Moscow');
});

test('blank counters remain untouched without Vendista calls', () => {
  const app = createHarness();
  app.sheets.station_maintenance.seed([node({salesAfterService:'',lastServiceDate:'13.09.2026'})]);
  const before = JSON.stringify(app.sheets);
  assert.equal(app.context.refreshMaintenanceSalesDaily().requested,0);
  assert.equal(app.requests.length,0);
  assert.equal(JSON.stringify(app.sheets),before);
});

test('groups shared service times and replaces totals on repeat without modifying other cells', () => {
  const app = createHarness();
  app.sheets.station_maintenance.seed([node(),node({nodeType:'BREW_UNIT'}),node({nodeType:'EXHAUST',salesAfterService:''})]);
  const stationBefore = JSON.stringify(app.sheets.coffee_station);
  report(app,25);
  assert.equal(app.context.refreshMaintenanceSalesDaily().updated,2);
  assert.equal(app.requests.length,1);
  assert.equal(new URL(app.requests[0].url).searchParams.get('DateFrom'),'2026-11-15 10:00:00');
  assert.ok(new URL(app.requests[0].url).searchParams.get('DateTo'));
  report(app,32);
  app.context.refreshMaintenanceSalesDaily();
  assert.equal(app.sheets.station_maintenance.object().salesAfterService,32);
  assert.equal(app.sheets.station_maintenance.object().lastServiceDate,node().lastServiceDate);
  assert.equal(app.sheets.station_maintenance.object().updatedAt,node().updatedAt);
  assert.equal(app.sheets.station_maintenance.object(4).salesAfterService,'');
  assert.equal(JSON.stringify(app.sheets.coffee_station),stationBefore);
});

test('failed second report does not write a partial set', () => {
  const app = createHarness();
  app.sheets.station_maintenance.seed([node(),node({nodeType:'BREW_UNIT',lastServiceDate:'2026-11-14T10:00:00'})]);
  const before = JSON.stringify(app.sheets);
  report(app,25);
  app.responses.push({status:403,body:{success:false}});
  assert.throws(() => app.context.refreshMaintenanceSalesDaily(),/Vendista HTTP/);
  assert.equal(JSON.stringify(app.sheets),before);
  assert.equal(app.lock.held,false);
});

test('service completed during report fetch is not overwritten', () => {
  const app = createHarness();
  app.sheets.station_maintenance.seed([node()]);
  const fetch = app.context.UrlFetchApp.fetch;
  app.context.UrlFetchApp.fetch = (...args) => {
    const result = fetch(...args);
    app.sheets.station_maintenance.seed([node({lastServiceDate:'2026-11-16T10:00:00',updatedAt:'new-version'})]);
    return result;
  };
  report(app,25);
  assert.equal(app.context.refreshMaintenanceSalesDaily().updated,0);
  assert.equal(app.sheets.station_maintenance.object().salesAfterService,0);
});

test('timestamps normalize Moscow local UTC and offset forms and reject date-only active rows', () => {
  const app = createHarness();
  for (const value of ['15.11.2026 10:00','2026-11-15T10:00:00','2026-11-15T07:00:00Z',
    '2026-11-15T10:00:00+03:00',new Date('2026-11-15T07:00:00Z')]) {
    assert.equal(app.context.maintenanceSalesDateFrom_(value),'2026-11-15 10:00:00');
  }
  assert.throws(() => app.context.maintenanceSalesDateFrom_('15.11.2026'),/exact service timestamp/);
  assert.throws(() => app.context.maintenanceSalesDateFrom_('31.02.2026 10:00'),/Invalid|time/i);
  assert.throws(() => app.context.maintenanceSalesDateFrom_('2026-02-30T10:00:00Z'),/Invalid/);
  for (const rows of [[node({salesAfterService:-1})],[node(),node()],[node({lastServiceDate:'2999-01-01T10:00:00'})]]) {
    app.sheets.station_maintenance.seed(rows);
    assert.throws(() => app.context.refreshMaintenanceSalesDaily());
  }
  assert.equal(app.requests.length,0);
});
