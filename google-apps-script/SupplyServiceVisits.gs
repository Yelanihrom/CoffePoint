function completeSupplyVisit(data) {
  if (!Number.isSafeInteger(CONFIG.SUPPLY_EARNINGS_PER_VISIT) || CONFIG.SUPPLY_EARNINGS_PER_VISIT <= 0) {
    throw createHttpError(500, 'Supply earnings rate is not configured');
  }
  const input = data || {};
  const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  if (input.earningId !== undefined && !uuidPattern.test(String(input.earningId))) {
    throw createHttpError(400, 'Invalid earningId');
  }
  if (typeof input.id !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(input.id) ||
      !Number.isSafeInteger(input.employeeId) || input.employeeId <= 0 ||
      !Number.isSafeInteger(input.stationId) || input.stationId <= 0 ||
      typeof input.completedAt !== 'string' ||
      !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?Z$/.test(input.completedAt)) {
    throw createHttpError(400, 'Invalid supply visit');
  }
  const completed = new Date(input.completedAt);
  if (isNaN(completed.getTime()) || completed.getTime() > Date.now() + 300000 ||
      completed.toISOString().slice(0, 19) !== input.completedAt.slice(0, 19)) {
    throw createHttpError(400, 'Invalid completedAt');
  }
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);
  try {
    const employee = readSheet(SHEETS.EMPLOYEES).find(row => Number(row.id) === input.employeeId);
    if (!employee || employee.role !== 'SUPPLY_OPERATOR' ||
        ![true, 1, 'true', 'TRUE', '1'].includes(employee.isActive)) {
      throw createHttpError(400, 'Active supplier not found');
    }
    if (!readSheet(SHEETS.COFFEE_STATION).some(row => Number(row.id) === input.stationId)) {
      throw createHttpError(404, 'Station not found');
    }
    const visits = readSheet(SHEETS.STATION_SERVICE_VISITS);
    const transactions = readSheet(SHEETS.EMPLOYEES_TRANSACTIONS);
    const existing = visits.filter(row => String(row.id) === input.id);
    if (existing.length > 1) throw createHttpError(409, 'Duplicate visit id');
    const timestamp = completed.toISOString();
    const now = new Date().toISOString();
    const visit = existing[0] || {
      id: input.id, employeeId: input.employeeId, stationId: input.stationId,
      completedAt: timestamp, createdAt: now, updatedAt: now,
    };
    if (Number(visit.employeeId) !== input.employeeId || Number(visit.stationId) !== input.stationId ||
        new Date(visit.completedAt).getTime() !== completed.getTime()) {
      throw createHttpError(409, 'Visit id belongs to another completion');
    }
    const linked = transactions.filter(row =>
      row.sourceType === 'SUPPLY_SERVICE' && String(row.sourceId) === input.id);
    if (linked.length > 1) throw createHttpError(409, 'Duplicate visit earning');
    const earningId = linked.length ? linked[0].id : (input.earningId || Utilities.getUuid());
    if (!uuidPattern.test(String(earningId)) || transactions.some(row =>
        String(row.id) === earningId && row !== linked[0])) {
      throw createHttpError(409, 'Earning id conflict');
    }
    const earning = linked[0] || {
      id: earningId, employeeId: input.employeeId, type: 'EARNING', operationDate: timestamp,
      periodFrom: '', periodTo: '', salesCount: '', amount: CONFIG.SUPPLY_EARNINGS_PER_VISIT,
      comment: '', createdAt: now, updatedAt: now, sourceType: 'SUPPLY_SERVICE', sourceId: input.id,
    };
    if (Number(earning.employeeId) !== input.employeeId ||
        earning.type !== 'EARNING' || earning.sourceType !== 'SUPPLY_SERVICE' ||
        String(earning.sourceId) !== input.id ||
        new Date(earning.operationDate).getTime() !== completed.getTime() ||
        !Number.isSafeInteger(Number(earning.amount)) || Number(earning.amount) <= 0) {
      throw createHttpError(409, 'Visit earning conflicts with existing transaction');
    }
    const requests = [];
    const append = (config, row) => {
      const sheet = getSheet(config.name);
      readHeaders(sheet, config.headers);
      requests.push({appendCells: {
        sheetId: sheet.getSheetId(), fields: 'userEnteredValue,userEnteredFormat.numberFormat',
        rows: [{values: config.headers.map(key => employeeTimestampFields_(config.name).includes(key)
          ? employeeTimestampCell_(row[key]) : {userEnteredValue:
            typeof row[key] === 'number' ? {numberValue: row[key]} : {stringValue: String(row[key] ?? '')}
          })}],
      }});
    };
    if (!existing.length) append(SHEETS.STATION_SERVICE_VISITS, visit);
    if (!linked.length) append(SHEETS.EMPLOYEES_TRANSACTIONS, earning);
    // One Sheets API batch commits both rows or neither. The script lock serializes retries.
    if (requests.length) Sheets.Spreadsheets.batchUpdate({requests: requests}, CONFIG.SPREADSHEET_ID);
    return {visit: visit, earning: earning};
  } finally {
    lock.releaseLock();
  }
}
