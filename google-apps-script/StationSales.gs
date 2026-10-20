const STATION_SALES_CONFIG = {
  triggerHandler: 'refreshStationSalesEveryTenMinutes',
};

function refreshStationSalesEveryTenMinutes() {
  const lock = LockService.getUserLock();
  if (!lock.tryLock(1000)) {
    Logger.log('Station sales refresh skipped: another refresh is running.');
    return;
  }

  try {
    const stations = readSheet(SHEETS.COFFEE_STATION);
    const eligibleStations = stations.filter(function(station) {
      return station.id && station.terminalId && station.lastServiceDate;
    });
    const dateFromByStationId = {};
    const uniqueDateFromValues = [];
    const seenDateFromValues = {};

    eligibleStations.forEach(function(station) {
      const dateFrom = stationSalesApiDateFrom_(station.lastServiceDate);
      dateFromByStationId[String(station.id)] = dateFrom;
      if (!seenDateFromValues[dateFrom]) {
        seenDateFromValues[dateFrom] = true;
        uniqueDateFromValues.push(dateFrom);
      }
    });

    const reportsByDateFrom = {};
    uniqueDateFromValues.forEach(function(dateFrom, index) {
      if (index > 0 && CONFIG.VENDISTA.REQUEST_DELAY_MS > 0) {
        Utilities.sleep(CONFIG.VENDISTA.REQUEST_DELAY_MS);
      }
      reportsByDateFrom[dateFrom] = fetchVendistaSales_(
        dateFrom,
        null,
        'station sales since ' + dateFrom
      ).salesByTerminal;
    });

    const updatedAt = Utilities.formatDate(
      new Date(),
      CONFIG.TIMEZONE,
      "yyyy-MM-dd'T'HH:mm:ss"
    );
    const rows = eligibleStations.map(function(station) {
      const dateFrom = dateFromByStationId[String(station.id)];
      const report = reportsByDateFrom[dateFrom];

      return {
        id: station.id,
        salesAfterService: report[String(station.terminalId)] || 0,
        salesUpdatedAt: updatedAt,
        expectedLastServiceDate: dateFrom,
      };
    });

    const updatedCount = rows.reduce(function(count, row) {
      return updateStationSalesFields_(row) ? count + 1 : count;
    }, 0);

    Logger.log(
      'Station sales updated for ' + updatedCount + ' of ' + rows.length +
      ' stations with ' + uniqueDateFromValues.length + ' Vendista requests.'
    );
  } finally {
    lock.releaseLock();
  }
}

function installEveryTenMinutesStationSalesTrigger() {
  ScriptApp.getProjectTriggers()
    .filter(function(trigger) {
      const handler = trigger.getHandlerFunction();
      return handler === 'refreshStationSalesHourly' ||
        handler === STATION_SALES_CONFIG.triggerHandler;
    })
    .forEach(function(trigger) {
      ScriptApp.deleteTrigger(trigger);
    });

  ScriptApp.newTrigger(STATION_SALES_CONFIG.triggerHandler)
    .timeBased()
    .everyMinutes(10)
    .create();

  Logger.log('Station sales trigger installed for every 10 minutes.');
}

function updateStationSalesFields_(sales) {
  const lock = LockService.getScriptLock();
  lock.waitLock(30000);

  try {
    const sheet = getSheet(SHEETS.COFFEE_STATION.name);
    const headers = readHeaders(sheet, SHEETS.COFFEE_STATION.headers);
    const rowIndex = findRowByKey(sheet, headers, 'id', sales.id);
    if (!rowIndex) return false;

    const serviceDateColumn = headers.indexOf('lastServiceDate') + 1;
    const salesColumn = headers.indexOf('salesAfterService') + 1;
    const currentServiceDate = sheet.getRange(rowIndex, serviceDateColumn).getValue();
    const currentDateFrom = stationSalesApiDateFrom_(currentServiceDate);

    if (currentDateFrom !== sales.expectedLastServiceDate) {
      Logger.log('Skipped stale sales result for station ' + sales.id);
      return false;
    }

      sheet.getRange(rowIndex, salesColumn).setNumberFormat('0');
      sheet.getRange(rowIndex, salesColumn, 1, 2).setValues([[
      sales.salesAfterService,
      sales.salesUpdatedAt,
    ]]);
    return true;
  } finally {
    lock.releaseLock();
  }
}

function stationSalesApiDateFrom_(value) {
  if (Object.prototype.toString.call(value) === '[object Date]') {
    return Utilities.formatDate(value, CONFIG.TIMEZONE, 'yyyy-MM-dd HH:mm:ss');
  }

  const text = String(value).trim();
  const ruMatch = text.match(
    /^(\d{2})\.(\d{2})\.(\d{4})\s+(\d{2}):(\d{2})(?::(\d{2}))?$/
  );
  if (ruMatch) {
    return ruMatch[3] + '-' + ruMatch[2] + '-' + ruMatch[1] +
      ' ' + ruMatch[4] + ':' + ruMatch[5] + ':' + (ruMatch[6] || '00');
  }

  const isoMatch = text.match(
    /^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?/
  );
  if (isoMatch) {
    return isoMatch[1] + ' ' + isoMatch[2] + ':' + (isoMatch[3] || '00');
  }

  throw new Error('Unsupported station lastServiceDate: ' + text);
}
