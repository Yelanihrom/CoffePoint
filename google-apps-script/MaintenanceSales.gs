const MAINTENANCE_SALES_CONFIG = {
  triggerHandler: 'refreshMaintenanceSalesDaily',
  nodes: ['MIXERS', 'LIQUID_WASTE_CONTAINER', 'BREW_UNIT', 'COFFEE_GRINDER', 'EXPANSION_TANK', 'EXHAUST'],
};

function installDailyMaintenanceSalesTrigger() {
  ScriptApp.getProjectTriggers()
    .filter(trigger => trigger.getHandlerFunction() === MAINTENANCE_SALES_CONFIG.triggerHandler)
    .forEach(trigger => ScriptApp.deleteTrigger(trigger));
  ScriptApp.newTrigger(MAINTENANCE_SALES_CONFIG.triggerHandler).timeBased()
    .atHour(2).nearMinute(0).everyDays(1).inTimezone(CONFIG.TIMEZONE).create();
  Logger.log('Maintenance sales trigger installed for approximately 02:00 Europe/Moscow.');
}

function refreshMaintenanceSalesDaily() {
  // Share the report lock with station sales to avoid concurrent request bursts.
  const lock = LockService.getUserLock();
  lock.waitLock(30000);
  try {
    const now = new Date();
    const dateTo = Utilities.formatDate(now, CONFIG.TIMEZONE, 'yyyy-MM-dd HH:mm:ss');
    const stations = new Map();
    readSheet(SHEETS.COFFEE_STATION).forEach(station => {
      if (stations.has(String(station.id))) throw new Error('Duplicate station ID');
      stations.set(String(station.id), station);
    });
    const seen = new Set();
    const groups = new Map();
    readSheet(SHEETS.STATION_MAINTENANCE).forEach(row => {
      const key = String(row.stationId) + '/' + row.nodeType;
      if (seen.has(key)) throw new Error('Duplicate maintenance key: ' + key);
      seen.add(key);
      if (row.salesAfterService === '' || row.salesAfterService == null) return;
      if (!['number', 'string'].includes(typeof row.salesAfterService) ||
          !Number.isSafeInteger(Number(row.salesAfterService)) || Number(row.salesAfterService) < 0 ||
          !MAINTENANCE_SALES_CONFIG.nodes.includes(row.nodeType)) {
        throw new Error('Invalid active maintenance counter: ' + key);
      }
      const station = stations.get(String(row.stationId));
      if (!station || !station.terminalId) return;
      const dateFrom = maintenanceSalesDateFrom_(row.lastServiceDate);
      if (dateFrom > dateTo) throw new Error('Future maintenance date: ' + key);
      if (!groups.has(dateFrom)) groups.set(dateFrom, []);
      groups.get(dateFrom).push({row, terminalId: station.terminalId});
    });
    // Resolve every report first: an upstream error must not save a partial refresh.
    const results = [];
    groups.forEach((items, dateFrom) => {
      if (results.length && CONFIG.VENDISTA.REQUEST_DELAY_MS > 0) Utilities.sleep(CONFIG.VENDISTA.REQUEST_DELAY_MS);
      const report = fetchVendistaSales_(dateFrom, dateTo, 'maintenance sales since ' + dateFrom).salesByTerminal;
      items.forEach(item => {
        const count = report[String(item.terminalId)] || 0;
        if (!Number.isSafeInteger(count) || count < 0) throw new Error('Invalid maintenance sales report');
        results.push({expected: item.row, count});
      });
    });
    const updatedAt = Utilities.formatDate(now, CONFIG.TIMEZONE, "yyyy-MM-dd'T'HH:mm:ss");
    const updated = writeMaintenanceSales_(results, updatedAt);
    Logger.log('Maintenance sales updated: ' + updated + '/' + results.length + '; reports: ' + groups.size);
    return {updated, requested: groups.size};
  } finally {
    lock.releaseLock();
  }
}

function maintenanceSalesDateFrom_(value) {
  let date;
  if (Object.prototype.toString.call(value) === '[object Date]') {
    date = value;
  } else {
    const text = String(value || '').trim();
    const calendar = text.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})/);
    if (calendar && (new Date(Date.UTC(Number(calendar[1]), Number(calendar[2]) - 1, Number(calendar[3])))
      .toISOString().slice(0, 10) !== calendar[1] + '-' + calendar[2] + '-' + calendar[3] ||
      Number(calendar[4]) > 23 || Number(calendar[5]) > 59 || Number(calendar[6]) > 59)) {
      throw new Error('Invalid maintenance timestamp');
    }
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/.test(text)) {
      date = new Date(text);
    } else {
      const ru = text.match(/^(\d{2})\.(\d{2})\.(\d{4}) (\d{2}:\d{2})(?::(\d{2}))?$/);
      const iso = text.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?$/);
      const normalized = ru ? ru[3] + '-' + ru[2] + '-' + ru[1] + 'T' + ru[4] + ':' + (ru[5] || '00')
        : iso ? iso[1] + 'T' + iso[2] + ':' + (iso[3] || '00') : '';
      if (!normalized) throw new Error('Maintenance counter requires an exact service timestamp');
      date = Utilities.parseDate(normalized, CONFIG.TIMEZONE, "yyyy-MM-dd'T'HH:mm:ss");
      if (Utilities.formatDate(date, CONFIG.TIMEZONE, "yyyy-MM-dd'T'HH:mm:ss") !== normalized) {
        throw new Error('Invalid maintenance timestamp');
      }
    }
  }
  if (isNaN(date.getTime())) throw new Error('Invalid maintenance timestamp');
  return Utilities.formatDate(date, CONFIG.TIMEZONE, 'yyyy-MM-dd HH:mm:ss');
}

function writeMaintenanceSales_(results, updatedAt) {
  if (!results.length) return 0;
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const sheet = getSheet(SHEETS.STATION_MAINTENANCE.name);
    const headers = readHeaders(sheet, SHEETS.STATION_MAINTENANCE.headers);
    const values = sheet.getRange(2, 1, Math.max(sheet.getLastRow() - 1, 1), headers.length).getValues();
    const rows = new Map();
    values.forEach((cells, index) => {
      const row = rowToObject(headers, cells);
      const key = String(row.stationId) + '/' + row.nodeType;
      if (rows.has(key)) throw new Error('Duplicate maintenance key during save');
      rows.set(key, {row, index: index + 2});
    });
    let updated = 0;
    results.forEach(result => {
      const expected = result.expected;
      const current = rows.get(String(expected.stationId) + '/' + expected.nodeType);
      if (!current || JSON.stringify(current.row) !== JSON.stringify(expected)) return;
      // Only the two calculated cells, never the service date or its version.
      sheet.getRange(current.index, headers.indexOf('salesAfterService') + 1).setNumberFormat('0');
      sheet.getRange(current.index, headers.indexOf('salesAfterService') + 1, 1, 2)
        .setValues([[result.count, updatedAt]]);
      updated++;
    });
    return updated;
  } finally {
    lock.releaseLock();
  }
}
