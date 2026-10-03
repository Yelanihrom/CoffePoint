function getFullSyncData(entities) {
  const requestedEntities = parseEntities(entities);

  return {
    stations: requestedEntities.has(SHEETS.COFFEE_STATION.name)
      ? readSheet(SHEETS.COFFEE_STATION)
      : [],
    inventories: requestedEntities.has(SHEETS.STATION_INVENTORY.name)
      ? readSheet(SHEETS.STATION_INVENTORY)
      : [],
    settings: requestedEntities.has(SHEETS.STATION_SETTINGS.name)
      ? readSheet(SHEETS.STATION_SETTINGS)
      : [],
    supplyCatalog: requestedEntities.has(SHEETS.SUPPLY_CATALOG.name)
      ? readSheet(SHEETS.SUPPLY_CATALOG)
      : [],
    warehouseStock: requestedEntities.has(SHEETS.WAREHOUSE_STOCK.name)
      ? readSheet(SHEETS.WAREHOUSE_STOCK)
      : [],
    employees: requestedEntities.has(SHEETS.EMPLOYEES.name)
      ? readSheet(SHEETS.EMPLOYEES)
      : [],
    employeeStationAssignments: requestedEntities.has(SHEETS.EMPLOYEE_STATION_ASSIGNMENTS.name)
      ? readSheet(SHEETS.EMPLOYEE_STATION_ASSIGNMENTS)
      : [],
    employeeTransactions: requestedEntities.has(SHEETS.EMPLOYEES_TRANSACTIONS.name)
      ? readSheet(SHEETS.EMPLOYEES_TRANSACTIONS)
      : []
  };
}

function parseEntities(entities) {
  if (!entities) {
    return new Set([
      SHEETS.COFFEE_STATION.name,
      SHEETS.STATION_INVENTORY.name,
      SHEETS.STATION_SETTINGS.name,
      SHEETS.SUPPLY_CATALOG.name,
      SHEETS.WAREHOUSE_STOCK.name,
      SHEETS.EMPLOYEES.name,
      SHEETS.EMPLOYEE_STATION_ASSIGNMENTS.name,
      SHEETS.EMPLOYEES_TRANSACTIONS.name
    ]);
  }

  return new Set(
    String(entities)
      .split(',')
      .map((entity) => entity.trim())
      .filter((entity) => entity)
  );
}

function upsertEntity(entityName, data) {
  const sheetConfig = findSheetConfig(entityName);
  const sheet = getSheet(sheetConfig.name);
  const headers = ensureHeaders(sheet, sheetConfig.headers);
  const keyValue = data[sheetConfig.key];

  if (keyValue === undefined || keyValue === null || keyValue === '') {
    throw createHttpError(400, 'Missing key field: ' + sheetConfig.key);
  }

  const rowIndex = findRowByKey(sheet, headers, sheetConfig.key, keyValue);
  assertIncomingRowHasTimestamp(sheet, headers, data);
  const values = headers.map((header) => normalizeCellValue(data[header]));

  if (rowIndex) {
    assertIncomingRowIsNotOlder(sheet, headers, rowIndex, data);
    sheet.getRange(rowIndex, 1, 1, values.length).setValues([values]);
  } else {
    sheet.appendRow(values);
  }
}

function assertIncomingRowHasTimestamp(sheet, headers, data) {
  const updatedAtColumn = headers.indexOf('updatedAt') + 1;

  if (!updatedAtColumn) return;

  if (!data.updatedAt) {
    throw createHttpError(
      400,
      'Missing updatedAt for ' + sheet.getName()
    );
  }
}

function assertIncomingRowIsNotOlder(sheet, headers, rowIndex, data) {
  const updatedAtColumn = headers.indexOf('updatedAt') + 1;

  if (!updatedAtColumn) return;

  const currentValue = sheet.getRange(rowIndex, updatedAtColumn).getValue();
  const currentTimestamp = parseSyncTimestamp(currentValue);
  const incomingTimestamp = parseSyncTimestamp(data.updatedAt);

  if (!currentTimestamp || !incomingTimestamp) return;

  if (currentTimestamp.getTime() > incomingTimestamp.getTime()) {
    throw createHttpError(
      409,
      'Conflict: remote row is newer for ' + sheet.getName()
    );
  }
}

function parseSyncTimestamp(value) {
  if (!value) return null;

  if (Object.prototype.toString.call(value) === '[object Date]') {
    return value;
  }

  const text = String(value).trim();
  if (!text) return null;

  const normalized = text
    .replace(' ', 'T')
    .replace(/(\.\d{3})\d+/, '$1');

  const parsed = new Date(normalized);

  if (!isNaN(parsed.getTime())) {
    return parsed;
  }

  const ruMatch = text.match(
    /^(\d{2})\.(\d{2})\.(\d{4})(?:\s+(\d{2}):(\d{2})(?::(\d{2}))?)?$/
  );

  if (!ruMatch) return null;

  return new Date(
    Number(ruMatch[3]),
    Number(ruMatch[2]) - 1,
    Number(ruMatch[1]),
    Number(ruMatch[4] || 0),
    Number(ruMatch[5] || 0),
    Number(ruMatch[6] || 0)
  );
}

function readSheet(sheetConfig) {
  const sheet = getSheet(sheetConfig.name);
  const headers = readHeaders(sheet, sheetConfig.headers);
  const lastRow = sheet.getLastRow();

  if (lastRow <= 1) return [];

  const values = sheet
    .getRange(2, 1, lastRow - 1, headers.length)
    .getValues();

  return values
    .filter((row) => row.some((cell) => cell !== ''))
    .map((row) => rowToObject(headers, row));
}

function findSheetConfig(entityName) {
  const configs = [
    SHEETS.COFFEE_STATION,
    SHEETS.STATION_INVENTORY,
    SHEETS.STATION_SETTINGS,
    SHEETS.SUPPLY_CATALOG,
    SHEETS.WAREHOUSE_STOCK,
    SHEETS.EMPLOYEES,
    SHEETS.EMPLOYEE_STATION_ASSIGNMENTS,
    SHEETS.EMPLOYEES_TRANSACTIONS
  ];

  const config = configs.find((item) => item.name === entityName);

  if (!config) {
    throw createHttpError(400, 'Unknown entity: ' + entityName);
  }

  return config;
}

function getSheet(sheetName) {
  const spreadsheet = SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID);
  const sheet = spreadsheet.getSheetByName(sheetName);

  if (!sheet) {
    throw createHttpError(500, 'Sheet not found: ' + sheetName);
  }

  return sheet;
}

function ensureHeaders(sheet, expectedHeaders) {
  const range = sheet.getRange(1, 1, 1, expectedHeaders.length);
  const headers = range.getValues()[0];
  const isReady = expectedHeaders.every((header, index) => {
    return headers[index] === header;
  });

  if (!isReady) {
    range.setValues([expectedHeaders]);
    sheet.setFrozenRows(1);
  }

  return expectedHeaders;
}

function readHeaders(sheet, expectedHeaders) {
  const range = sheet.getRange(1, 1, 1, expectedHeaders.length);
  const headers = range.getValues()[0];
  const isReady = expectedHeaders.every((header, index) => {
    return headers[index] === header;
  });

  if (!isReady) {
    throw createHttpError(500, 'Sheet headers mismatch: ' + sheet.getName());
  }

  return expectedHeaders;
}

function findRowByKey(sheet, headers, key, value) {
  const keyColumn = headers.indexOf(key) + 1;
  const lastRow = sheet.getLastRow();

  if (!keyColumn || lastRow <= 1) return null;

  const values = sheet.getRange(2, keyColumn, lastRow - 1, 1).getValues();
  const normalizedValue = String(value);

  for (let index = 0; index < values.length; index += 1) {
    if (String(values[index][0]) === normalizedValue) {
      return index + 2;
    }
  }

  return null;
}

function rowToObject(headers, row) {
  return headers.reduce((result, header, index) => {
    result[header] = row[index];
    return result;
  }, {});
}

function normalizeCellValue(value) {
  if (value === undefined || value === null) return '';
  return value;
}
