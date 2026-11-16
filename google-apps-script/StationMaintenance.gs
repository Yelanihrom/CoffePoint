function updateStationMaintenance(data) {
  if (!Number.isSafeInteger(data.id) || data.id <= 0 || !Array.isArray(data.maintenance) ||
      !data.maintenance.length || data.maintenance.length > 6 ||
      Object.keys(data).some(key => !['id', 'maintenance'].includes(key))) {
    throw createHttpError(400, 'Invalid maintenance request');
  }
  const nodes = new Set();
  const changes = data.maintenance.map(item => {
    if (!item || !MAINTENANCE_SALES_CONFIG.nodes.includes(item.nodeType) || nodes.has(item.nodeType) ||
        typeof item.resetSales !== 'boolean' || typeof item.lastServiceDate !== 'string' ||
        typeof item.updatedAt !== 'string' || !/^\d{4}-\d{2}-\d{2}T.*Z$/.test(item.updatedAt)) {
      throw createHttpError(400, 'Invalid maintenance item');
    }
    nodes.add(item.nodeType);
    const version = new Date(item.updatedAt);
    if (isNaN(version.getTime()) || version.getTime() > Date.now() + 300000) {
      throw createHttpError(400, 'Invalid maintenance version');
    }
    let service = '';
    if (item.lastServiceDate) {
      try {
        if (item.resetSales) maintenanceSalesDateFrom_(item.lastServiceDate);
        const ru = item.lastServiceDate.match(/^(\d{2})\.(\d{2})\.(\d{4})$/);
        const value = ru ? ru[3] + '-' + ru[2] + '-' + ru[1] : item.lastServiceDate;
        service = normalizeEmployeeTimestamp_(value);
        if (ru && Utilities.formatDate(service, CONFIG.TIMEZONE, 'yyyy-MM-dd') !== value) throw new Error('Invalid date');
        if (!service || isNaN(service.getTime())) throw new Error('Invalid date');
      } catch (_) { throw createHttpError(400, 'Invalid maintenance date'); }
      if (service.getTime() > Date.now() + 300000) throw createHttpError(400, 'Future service date');
    } else if (item.resetSales) throw createHttpError(400, 'Missing maintenance date');
    return {stationId:data.id, nodeType:item.nodeType, lastServiceDate:service,
      salesAfterService:item.resetSales ? 0 : '', salesUpdatedAt:item.resetSales ? version : '', updatedAt:version,
      resetSales:item.resetSales};
  });
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    if (!readSheet(SHEETS.COFFEE_STATION).some(row => Number(row.id) === data.id)) {
      throw createHttpError(404, 'Station not found');
    }
    const config = SHEETS.STATION_MAINTENANCE;
    const sheet = getSheet(config.name);
    readHeaders(sheet, config.headers);
    const rows = sheet.getLastRow() > 1
      ? sheet.getRange(2, 1, sheet.getLastRow()-1, config.headers.length).getValues().map(cells => rowToObject(config.headers,cells)) : [];
    const keys = new Set();
    rows.forEach(row => {
      if (!row.stationId && !row.nodeType) return;
      const key = row.stationId + '/' + row.nodeType;
      if (keys.has(key)) throw createHttpError(409, 'Duplicate maintenance key');
      keys.add(key);
    });
    const requests = [];
    changes.forEach(change => {
      const index = rows.findIndex(row => Number(row.stationId) === data.id && row.nodeType === change.nodeType);
      const current = rows[index];
      if (current) {
        const oldVersion = parseSyncTimestamp(current.updatedAt);
        if (change.resetSales && parseSyncTimestamp(current.lastServiceDate)?.getTime() > change.lastServiceDate.getTime()) {
          throw createHttpError(409, 'Conflict: newer node service date');
        }
        const sameDate = String(current.lastServiceDate || '') === String(change.lastServiceDate || '') ||
          (parseSyncTimestamp(current.lastServiceDate)?.getTime() === parseSyncTimestamp(change.lastServiceDate)?.getTime());
        if (oldVersion && oldVersion.getTime() > change.updatedAt.getTime()) throw createHttpError(409, 'Conflict: newer node maintenance');
        if (oldVersion && oldVersion.getTime() === change.updatedAt.getTime()) {
          if (!sameDate) throw createHttpError(409, 'Conflict: reused maintenance version');
          return; // A retry must not reset a counter already recalculated by the trigger.
        }
        if (!change.resetSales) {
          change.salesAfterService = current.salesAfterService;
          change.salesUpdatedAt = current.salesUpdatedAt;
          if (change.salesAfterService !== '' && change.salesAfterService != null && !change.lastServiceDate) {
            throw createHttpError(400, 'Active counter requires service date');
          }
        }
      }
      const values = config.headers.map(key => ['lastServiceDate','salesUpdatedAt','updatedAt'].includes(key)
        ? (change[key] ? employeeTimestampCell_(change[key]) : {userEnteredValue:{stringValue:''}})
        : {userEnteredValue:typeof change[key] === 'number' ? {numberValue:change[key]} : {stringValue:change[key]}});
      if (current) {
        requests.push({updateCells:{range:{sheetId:sheet.getSheetId(),startRowIndex:index+1,endRowIndex:index+2,
          startColumnIndex:0,endColumnIndex:6}, rows:[{values}],fields:'userEnteredValue,userEnteredFormat.numberFormat'}});
      } else {
        requests.push({appendCells:{sheetId:sheet.getSheetId(),rows:[{values}],fields:'userEnteredValue,userEnteredFormat.numberFormat'}});
      }
    });
    if (requests.length) Sheets.Spreadsheets.batchUpdate({requests},CONFIG.SPREADSHEET_ID);
  } finally { lock.releaseLock(); }
}
