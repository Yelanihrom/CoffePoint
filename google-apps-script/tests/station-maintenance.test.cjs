const {test}=require('node:test');
const assert=require('node:assert/strict');
const {createHarness}=require('./support.cjs');
const change=(overrides={})=>({nodeType:'MIXERS',lastServiceDate:'2026-11-16T10:00:00.123Z',
  updatedAt:'2026-11-16T10:00:00.123Z',resetSales:true,...overrides});
const send=(app,items)=>app.call('POST',{action:'complete_service',entity:'station_maintenance',data:{id:7,maintenance:items}});

test('new node completion resets only the selected node and retry never resets accumulated sales',()=>{
  const app=createHarness();
  const before=JSON.stringify(app.sheets.coffee_station);
  assert.equal(send(app,[change()]).success,true);
  const mixer=app.sheets.station_maintenance.object();
  assert.equal(mixer.lastServiceDate.toISOString(),'2026-11-16T10:00:00.123Z');
  assert.equal(mixer.salesAfterService,0);
  const countCol=app.config.SHEETS.STATION_MAINTENANCE.headers.indexOf('salesAfterService');
  app.sheets.station_maintenance.rows[1][countCol]=42;
  assert.equal(send(app,[change()]).success,true);
  assert.equal(app.sheets.station_maintenance.object().salesAfterService,42);
  assert.equal(app.atomicBatches.length,1);
  assert.equal(JSON.stringify(app.sheets.coffee_station),before);
});

test('calendar choice preserves counter or resets it explicitly while allowing correction backwards',()=>{
  const app=createHarness();
  assert.equal(send(app,[change()]).success,true);
  assert.equal(send(app,[change({lastServiceDate:'01.10.2026',resetSales:false,updatedAt:'2026-11-16T10:01:00Z'})]).success,true);
  assert.equal(app.sheets.station_maintenance.object().salesAfterService,0);
  assert.equal(app.sheets.station_maintenance.object().salesUpdatedAt.toISOString(),'2026-11-16T10:00:00.123Z');
  assert.equal(send(app,[change({updatedAt:'2026-11-16T10:00:30Z'})]).error.status,409);
});

test('conflict in one node and failed batch preserve the complete prior state',()=>{
  const app=createHarness();
  assert.equal(send(app,[change()]).success,true);
  const before=JSON.stringify(app.sheets);
  assert.equal(send(app,[change({nodeType:'BREW_UNIT'}),change({updatedAt:'2026-11-16T09:59:00Z'})]).error.status,409);
  assert.equal(JSON.stringify(app.sheets),before);
  app.context.failAtomicBatch=true;
  assert.equal(send(app,[change({nodeType:'BREW_UNIT'})]).success,false);
  assert.equal(JSON.stringify(app.sheets),before);
});

test('missing rows append by compound key and invalid requests cannot mutate data',()=>{
  const app=createHarness();
  app.sheets.station_maintenance.seed([]);
  assert.equal(send(app,[change()]).success,true);
  assert.equal(app.sheets.station_maintenance.getLastRow(),2);
  for(const items of [[change(),change()],[change({lastServiceDate:'16.11.2026'})],
    [change({nodeType:'UNKNOWN'})],[change({updatedAt:''})]]) {
    const before=JSON.stringify(app.sheets);
    assert.equal(send(app,items).success,false);
    assert.equal(JSON.stringify(app.sheets),before);
  }
});
