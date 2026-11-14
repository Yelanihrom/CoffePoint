function getFullSyncData(entities, stationId) {
  const requestedEntities = parseEntities(entities);
  const hasStationFilter = stationId !== undefined && stationId !== null && stationId !== '';
  if (hasStationFilter && !/^[1-9]\d*$/.test(String(stationId))) {
    throw createHttpError(400, 'Invalid stationId');
  }
  const readRequested = (config, key) => {
    if (!requestedEntities.has(config.name)) return [];
    const rows = readSheet(config);
    return hasStationFilter && key
      ? rows.filter((row) => String(row[key]) === String(stationId))
      : rows;
  };

  return {
    stations: readRequested(SHEETS.COFFEE_STATION, 'id'),
    inventories: readRequested(SHEETS.STATION_INVENTORY, 'stationId'),
    settings: readRequested(SHEETS.STATION_SETTINGS, 'stationId'),
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
      : [],
    stationServiceVisits: readRequested(SHEETS.STATION_SERVICE_VISITS, 'stationId'),
    notificationSettings: requestedEntities.has(SHEETS.NOTIFICATION_SETTINGS.name)
      ? readNotificationSettings_() : []
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
      SHEETS.EMPLOYEES_TRANSACTIONS.name,
      SHEETS.STATION_SERVICE_VISITS.name,
      SHEETS.NOTIFICATION_SETTINGS.name
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
  upsertEntities(entityName, [data]);
}

function upsertEntities(entityName, dataItems) {
  if (!Array.isArray(dataItems) || dataItems.length === 0) {
    throw createHttpError(400, 'Missing batch items');
  }

  const lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    const sheetConfig = findSheetConfig(entityName);
    const sheet = getSheet(sheetConfig.name);
    const headers = ensureHeaders(sheet, sheetConfig.headers);
    const seenKeys = new Set();
    const preparedRows = dataItems.map((data) => {
      if (!data || typeof data !== 'object' || Array.isArray(data)) {
        throw createHttpError(400, 'Invalid batch item');
      }

      const keyValue = data[sheetConfig.key];
      if (keyValue === undefined || keyValue === null || keyValue === '') {
        throw createHttpError(400, 'Missing key field: ' + sheetConfig.key);
      }

      const normalizedKey = String(keyValue);
      if (entityName === SHEETS.EMPLOYEES_TRANSACTIONS.name &&
          data.sourceType === 'SUPPLY_SERVICE') {
        throw createHttpError(403, 'Supply earnings are managed by complete_supply_visit');
      }
      if (seenKeys.has(normalizedKey)) {
        throw createHttpError(400, 'Duplicate key in batch: ' + normalizedKey);
      }
      seenKeys.add(normalizedKey);

      data = Object.assign({}, data);
      if (sheetConfig.name === SHEETS.NOTIFICATION_SETTINGS.name) {
        if (typeof data.updatedAt !== 'string' ||
            !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,3})?Z$/.test(data.updatedAt) ||
            isNaN(Date.parse(data.updatedAt))) {
          throw createHttpError(400, 'Notification settings require ISO UTC updatedAt');
        }
        data.updatedAt = new Date(data.updatedAt);
      }
      employeeTimestampFields_(sheetConfig.name).forEach(function(field) {
        if (Object.prototype.hasOwnProperty.call(data, field)) {
          data[field] = normalizeEmployeeTimestamp_(data[field]);
        }
      });

      assertIncomingRowHasTimestamp(sheet, headers, data);
      const rowIndex = findRowByKey(sheet, headers, sheetConfig.key, keyValue);
      if (rowIndex) {
        assertIncomingRowIsNotOlder(sheet, headers, rowIndex, data);
      }

      const currentValues = rowIndex
        ? sheet.getRange(rowIndex, 1, 1, headers.length).getValues()[0]
        : headers.map(() => '');
      if (entityName === SHEETS.EMPLOYEES_TRANSACTIONS.name &&
          currentValues[headers.indexOf('sourceType')] === 'SUPPLY_SERVICE') {
        throw createHttpError(403, 'Supply earnings are managed by complete_supply_visit');
      }
      const values = headers.map((header, index) => {
        if (Object.prototype.hasOwnProperty.call(data, header)) {
          if (sheetConfig.name === SHEETS.WAREHOUSE_STOCK.name && header === 'updatedAt') {
            return normalizeWarehouseTimestamp_(data[header]);
          }
          return normalizeCellValue(data[header]);
        }
        if (!rowIndex && sheetConfig.name === SHEETS.COFFEE_STATION.name &&
            header === 'salesAfterService') {
          return 0;
        }
        return currentValues[index];
      });

      if (sheetConfig.name === SHEETS.COFFEE_STATION.name && rowIndex) {
        preserveNewerServiceDates_(headers, currentValues, values);
      }
      if (sheetConfig.name === SHEETS.NOTIFICATION_SETTINGS.name) {
        validateNotificationSettings_(rowToObject(headers, values), 400);
      }

      return { rowIndex: rowIndex, values: values };
    });

    preparedRows.forEach((row) => {
      if (row.rowIndex) {
        sheet.getRange(row.rowIndex, 1, 1, row.values.length)
          .setValues([row.values]);
      } else {
        sheet.appendRow(row.values);
      }
    });
    if (sheetConfig.name === SHEETS.WAREHOUSE_STOCK.name && sheet.getLastRow() > 1) {
      sheet.getRange(2, headers.indexOf('updatedAt') + 1, sheet.getLastRow() - 1, 1)
        .setNumberFormat('dd.MM.yyyy HH:mm');
    }
    if (sheet.getLastRow() > 1) {
      if (sheetConfig.name === SHEETS.NOTIFICATION_SETTINGS.name) {
        sheet.getRange(2, headers.indexOf('updatedAt') + 1, sheet.getLastRow() - 1, 1)
          .setNumberFormat('yyyy-mm-dd hh:mm:ss');
      }
      employeeTimestampFields_(sheetConfig.name).forEach(function(field) {
        sheet.getRange(2, headers.indexOf(field) + 1, sheet.getLastRow() - 1, 1)
          .setNumberFormat('yyyy-mm-dd hh:mm:ss');
      });
    }
  } finally {
    lock.releaseLock();
  }
}

function updateStationServiceFields(data) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    const payload = data && typeof data === 'object' ? data : {};
    const sheetConfig = SHEETS.COFFEE_STATION;
    const sheet = getSheet(sheetConfig.name);
    const headers = readHeaders(sheet, sheetConfig.headers);
    const stationId = payload.id;
    const lastServiceDate = payload.lastServiceDate;
    const salesUpdatedAt = payload.salesUpdatedAt;
    const salesAfterService = payload.salesAfterService;
    const hasOperatorFields = Object.prototype.hasOwnProperty.call(payload, 'lastServiceDate') ||
      Object.prototype.hasOwnProperty.call(payload, 'salesAfterService') ||
      Object.prototype.hasOwnProperty.call(payload, 'salesUpdatedAt');
    const serviceFields = [
      'mixersServiceDate',
      'liquidWasteContainerServiceDate',
    ].filter(function(field) {
      return Object.prototype.hasOwnProperty.call(payload, field);
    });

    if (stationId === undefined || stationId === null || stationId === '') {
      throw createHttpError(400, 'Missing key field: id');
    }
    if (hasOperatorFields &&
        (!lastServiceDate || !salesUpdatedAt || salesAfterService === undefined)) {
      throw createHttpError(400, 'Missing service fields');
    }
    if (!hasOperatorFields && serviceFields.length === 0) {
      throw createHttpError(400, 'Missing service fields');
    }

    const rowIndex = findRowByKey(sheet, headers, 'id', stationId);
    if (!rowIndex) {
      throw createHttpError(404, 'Station not found: ' + stationId);
    }

    if (hasOperatorFields) {
      assertServiceDateIsNotOlder_(sheet, headers, rowIndex, 'lastServiceDate', lastServiceDate);
      const lastServiceDateColumn = headers.indexOf('lastServiceDate') + 1;
      sheet.getRange(rowIndex, lastServiceDateColumn, 1, 3).setValues([[
        lastServiceDate,
        salesAfterService,
        salesUpdatedAt,
      ]]);
    }

    serviceFields.forEach(function(field) {
      assertServiceDateIsNotOlder_(sheet, headers, rowIndex, field, payload[field]);
      const column = headers.indexOf(field) + 1;
      if (!column) {
        throw createHttpError(500, 'Missing service column: ' + field);
      }
      sheet.getRange(rowIndex, column).setValue(normalizeCellValue(payload[field]));
    });
  } finally {
    lock.releaseLock();
  }
}

function assertServiceDateIsNotOlder_(sheet, headers, rowIndex, field, incomingValue) {
  const column = headers.indexOf(field) + 1;
  if (!column) {
    throw createHttpError(500, 'Missing service column: ' + field);
  }

  const currentTimestamp = parseSyncTimestamp(sheet.getRange(rowIndex, column).getValue());
  const incomingTimestamp = parseSyncTimestamp(incomingValue);
  if (currentTimestamp && incomingTimestamp &&
      currentTimestamp.getTime() > incomingTimestamp.getTime()) {
    throw createHttpError(409, 'Conflict: station has a newer ' + field);
  }
}

function preserveNewerServiceDates_(headers, currentValues, incomingValues) {
  [
    'lastServiceDate',
    'liquidWasteContainerServiceDate',
    'brewUnitServiceDate',
    'coffeeGrinderServiceDate',
    'mixersServiceDate',
    'expansionTankServiceDate',
    'exhaustServiceDate',
  ].forEach(function(field) {
    const index = headers.indexOf(field);
    if (index < 0) return;

    const currentTimestamp = parseSyncTimestamp(currentValues[index]);
    const incomingTimestamp = parseSyncTimestamp(incomingValues[index]);
    if (currentTimestamp &&
        (!incomingTimestamp || currentTimestamp.getTime() > incomingTimestamp.getTime())) {
      incomingValues[index] = currentValues[index];
    }
  });
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

  // Parse day-first dates before JavaScript can interpret them as month-first dates.
  const ruMatch = text.match(
    /^(\d{2})\.(\d{2})\.(\d{4})(?:\s+(\d{2}):(\d{2})(?::(\d{2}))?)?$/
  );

  if (ruMatch) {
    return new Date(
      Number(ruMatch[3]),
      Number(ruMatch[2]) - 1,
      Number(ruMatch[1]),
      Number(ruMatch[4] || 0),
      Number(ruMatch[5] || 0),
      Number(ruMatch[6] || 0)
    );
  }

  const normalized = text
    .replace(' ', 'T')
    .replace(/(T\d{2}:\d{2}:\d{2}\.\d{3})\d+(?=Z|[+-]\d{2}:\d{2}|$)/i, '$1');
  const parsed = new Date(normalized);
  return isNaN(parsed.getTime()) ? null : parsed;
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
    SHEETS.EMPLOYEES_TRANSACTIONS,
    SHEETS.NOTIFICATION_SETTINGS
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

function validateNotificationSettings_(row, status) {
  if (!['MANAGER', 'OPERATOR', 'SUPPLY_OPERATOR'].includes(row.role)) {
    throw createHttpError(status, 'Invalid notification role');
  }
  const fields = SHEETS.NOTIFICATION_SETTINGS.headers.filter(field => field.endsWith('Enabled'));
  fields.forEach(function(field) {
    if (typeof row[field] !== 'boolean') {
      throw createHttpError(status, 'Notification setting must be boolean: ' + field);
    }
    if (row[field] && ((row.role === 'OPERATOR' && field !== 'refillEnabled') ||
        (row.role === 'SUPPLY_OPERATOR' && field === 'refillEnabled'))) {
      throw createHttpError(status, 'Notification is unavailable for this role: ' + field);
    }
  });
}

function readNotificationSettings_() {
  const rows = readSheet(SHEETS.NOTIFICATION_SETTINGS);
  const roles = new Set();
  rows.forEach(function(row) {
    validateNotificationSettings_(row, 500);
    if (roles.has(row.role)) throw createHttpError(500, 'Duplicate notification role');
    roles.add(row.role);
  });
  return rows;
}

function normalizeWarehouseTimestamp_(value) {
  // Preserve the instant and explicit offset used by conflict checks and Android.
  if (typeof value !== 'string' || !/(?:Z|[+-]\d{2}:\d{2})$/i.test(value)) return value;
  const timestamp = parseSyncTimestamp(value);
  if (!timestamp) return value;
  return timestamp;
}

function normalizeCellValue(value) {
  if (value === undefined || value === null) return '';
  return value;
}

function employeeTimestampFields_(sheetName) {
  if (sheetName === SHEETS.EMPLOYEES_TRANSACTIONS.name) return ['operationDate', 'createdAt', 'updatedAt'];
  if (sheetName === SHEETS.STATION_SERVICE_VISITS.name) return ['completedAt', 'createdAt', 'updatedAt'];
  if (sheetName === SHEETS.EMPLOYEES.name || sheetName === SHEETS.EMPLOYEE_STATION_ASSIGNMENTS.name) {
    return ['createdAt', 'updatedAt'];
  }
  return [];
}

function normalizeEmployeeTimestamp_(value) {
  if (value === '' || value === null || value === undefined) return '';
  if (Object.prototype.toString.call(value) === '[object Date]' && !isNaN(value.getTime())) return value;
  const text = String(value).trim();
  let date;
  if (/(?:Z|[+-]\d{2}:\d{2})$/i.test(text)) {
    date = parseSyncTimestamp(text);
  } else {
    const normalized = text.replace(' ', 'T').replace(/\.(\d{1,3})$/, function(_, fraction) {
      return '.' + fraction.padEnd(3, '0');
    });
    const pattern = /^\d{4}-\d{2}-\d{2}$/.test(normalized) ? 'yyyy-MM-dd' :
      /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{3})?$/.test(normalized)
        ? "yyyy-MM-dd'T'HH:mm:ss" + (normalized.includes('.') ? '.SSS' : '') : null;
    if (pattern) date = Utilities.parseDate(normalized, CONFIG.TIMEZONE, pattern);
  }
  if (!date || isNaN(date.getTime())) throw createHttpError(400, 'Invalid employee timestamp');
  return date;
}

function employeeTimestampCell_(value) {
  const date = normalizeEmployeeTimestamp_(value);
  if (!date) return {userEnteredValue: {stringValue: ''}};
  const offset = Utilities.formatDate(date, CONFIG.TIMEZONE, 'Z');
  const offsetMinutes = (Number(offset.slice(1, 3)) * 60 + Number(offset.slice(3, 5))) *
    (offset[0] === '-' ? -1 : 1);
  // Sheets serials are local wall-clock dates; API responses continue to use UTC instants.
  return {
    userEnteredValue: {numberValue: (date.getTime() + offsetMinutes * 60000) / 86400000 + 25569},
    userEnteredFormat: {numberFormat: {type: 'DATE_TIME', pattern: 'yyyy-mm-dd hh:mm:ss'}},
  };
}
