const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const scriptRoot = path.resolve(__dirname, '..');
const fixtureRoot = path.resolve(scriptRoot, '../app/src/test/resources/contracts');
const schema = JSON.parse(fs.readFileSync(path.join(fixtureRoot, 'sheet-schema.json'), 'utf8'));
const fullSync = JSON.parse(fs.readFileSync(path.join(fixtureRoot, 'full-sync.json'), 'utf8'));

class MemorySheet {
  constructor(name, headers) {
    this.name = name;
    this.rows = [[...headers]];
    this.formats = [];
  }
  getName() { return this.name; }
  getSheetId() { return Object.keys(schema).indexOf(this.name); }
  getLastRow() { return this.rows.length; }
  setFrozenRows() {}
  appendRow(values) { this.rows.push([...values]); }
  getRange(row, column, height = 1, width = 1) {
    if (row < 1 || column < 1 || height < 1 || width < 1) throw new Error('Invalid range');
    const sheet = this;
    return {
      getValue() { return this.getValues()[0][0]; },
      getValues() {
        return Array.from({length: height}, (_, y) =>
          Array.from({length: width}, (_, x) => sheet.rows[row + y - 1]?.[column + x - 1] ?? ''));
      },
      setValue(value) { return this.setValues([[value]]); },
      setValues(values) {
        if (values.length !== height || values.some(r => r.length !== width)) {
          throw new Error('Range dimensions do not match values');
        }
        values.forEach((valuesRow, y) => {
          const target = sheet.rows[row + y - 1] ?? (sheet.rows[row + y - 1] = []);
          valuesRow.forEach((value, x) => { target[column + x - 1] = value; });
        });
        return this;
      },
      setNumberFormat(pattern) {
        sheet.formats.push({row, column, height, width, pattern});
        return this;
      },
    };
  }
  seed(objects) {
    this.rows = [this.rows[0], ...objects.map(object => this.rows[0].map(key => object[key] ?? ''))];
  }
  object(row = 2) {
    return Object.fromEntries(this.rows[0].map((header, index) => [header, this.rows[row - 1][index]]));
  }
}

function createHarness(properties = {SYNC_TOKEN: 'test-token', VENDISTA_TOKEN: 'test-vendista-token'}) {
  const sheets = Object.fromEntries(Object.entries(schema).map(([name, definition]) => {
    const sheet = new MemorySheet(name, definition.headers);
    sheet.seed(fullSync.data[definition.responseKey]);
    return [name, sheet];
  }));
  const lock = {held: false, releases: 0,
    waitLock() { this.held = true; },
    releaseLock() { this.held = false; this.releases += 1; }};
  const requests = [];
  const responses = [];
  const sleeps = [];
  const atomicBatches = [];
  const context = vm.createContext({
    PropertiesService: {getScriptProperties: () => ({getProperty: key => properties[key] ?? null})},
    ContentService: {MimeType: {JSON: 'application/json'}, createTextOutput: text => ({
      text, setMimeType() { return this; },
    })},
    SpreadsheetApp: {openById: () => ({getSheetByName: name => sheets[name] ?? null})},
    Sheets: {Spreadsheets: {batchUpdate(body) {
      if (context.failAtomicBatch) throw new Error('Injected Sheets API failure');
      const prepared = body.requests.map(request => {
        const value = request.appendCells;
        const sheet = Object.values(sheets).find(s => s.getSheetId() === value.sheetId);
        if (!sheet) throw new Error('Missing sheet');
        return {sheet, rows: value.rows.map(row => row.values.map(cell => {
          const raw = cell.userEnteredValue.numberValue ?? cell.userEnteredValue.stringValue;
          return cell.userEnteredFormat?.numberFormat?.type === 'DATE_TIME'
            ? new Date(Math.round((raw - 25569) * 86400000) - 3 * 3600000) : raw;
        }))};
      });
      prepared.forEach(({sheet, rows}) => rows.forEach(row => sheet.appendRow(row)));
      atomicBatches.push(body);
    }}},
    LockService: {getScriptLock: () => lock},
    Logger: {log() {}},
    Utilities: {getUuid: () => require('node:crypto').randomUUID(), sleep: delay => sleeps.push(delay),
      parseDate(text, timeZone) {
        if (timeZone !== 'Europe/Moscow') throw new Error('Unexpected timezone');
        return new Date(text + (text.includes('T') ? '+03:00' : 'T00:00:00+03:00'));
      },
      formatDate(date, timeZone, pattern) {
        if (pattern === 'Z' && timeZone === 'Europe/Moscow') return '+0300';
        if (pattern !== 'yyyy-MM-dd') throw new Error('Unexpected date pattern in test: ' + pattern);
        return new Intl.DateTimeFormat('en-CA', {timeZone, year: 'numeric', month: '2-digit', day: '2-digit'})
          .format(date);
      }},
    UrlFetchApp: {fetch: (url, options) => {
      requests.push({url, options});
      const next = responses.shift();
      if (!next) throw new Error('Unexpected external request in test');
      return {getResponseCode: () => next.status, getContentText: () => JSON.stringify(next.body)};
    }},
  });
  for (const name of ['Config.gs', 'Response.gs', 'Router.gs', 'Auth.gs', 'SheetsRepository.gs',
    'StationSales.gs', 'VendistaClient.gs', 'SalesAnalytics.gs', 'SupplyServiceVisits.gs']) {
    vm.runInContext(fs.readFileSync(path.join(scriptRoot, name), 'utf8'), context, {filename: name});
  }
  const config = vm.runInContext('({CONFIG, SHEETS})', context);
  function call(method, request, token = 'test-token') {
    const event = method === 'GET'
      ? {parameter: {...request, token}}
      : {parameter: {}, postData: {contents: JSON.stringify({...request, token})}};
    return JSON.parse(context.handleRequest(event, method).text);
  }
  return {context, config, sheets, lock, call, requests, responses, sleeps, atomicBatches};
}

module.exports = {createHarness, schema, fullSync};
