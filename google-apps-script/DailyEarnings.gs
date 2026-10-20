const DAILY_EARNINGS_CONFIG = {
  timezone: CONFIG.TIMEZONE,
  earningsPerSale: 15,
  triggerHandler: 'recordYesterdayDailyEarnings',
};

function recordYesterdayDailyEarnings() {
  const yesterday = shiftDailyEarningsDate_(getDailyEarningsToday_(), -1);
  processDailyEarningsDates_([yesterday], true);
}

function installDailyEarningsTrigger() {
  ScriptApp.getProjectTriggers()
    .filter(function(trigger) {
      return trigger.getHandlerFunction() === DAILY_EARNINGS_CONFIG.triggerHandler;
    })
    .forEach(function(trigger) {
      ScriptApp.deleteTrigger(trigger);
    });

  ScriptApp.newTrigger(DAILY_EARNINGS_CONFIG.triggerHandler)
    .timeBased()
    .atHour(2)
    .nearMinute(0)
    .everyDays(1)
    .inTimezone(DAILY_EARNINGS_CONFIG.timezone)
    .create();

  Logger.log('Daily earnings trigger installed for approximately 02:00 Europe/Moscow.');
}

function processDailyEarningsDates_(dates, refreshExistingDailyRows, optContext) {
  const context = optContext || loadDailyEarningsContext_();
  let rowsWritten = 0;

  dates.forEach(function(date) {
    const employees = getEmployeesForDailyEarnings_(date, context);
    if (employees.length === 0) return;

    if (CONFIG.VENDISTA.REQUEST_DELAY_MS > 0) {
      Utilities.sleep(CONFIG.VENDISTA.REQUEST_DELAY_MS);
    }
    const report = loadVendistaSalesForDate_(date);
    const rowsToUpsert = [];
    employees.forEach(function(item) {
      const dailyId = 'AUTO-EARNING-' + item.employee.id + '-' + date;
      const existingDailyRow = context.transactionsById[dailyId];

      if (existingDailyRow && !refreshExistingDailyRows) return;
      if (hasManualEarningCoveringDate_(context.transactions, item.employee.id, date, dailyId)) {
        Logger.log('Skipped ' + item.employee.name + ' on ' + date + ': covered by an existing earning.');
        return;
      }

      const salesCount = item.terminalIds.reduce(function(total, terminalId) {
        return total + (report.salesByTerminal[String(terminalId)] || 0);
      }, 0);
      const now = Utilities.formatDate(new Date(), DAILY_EARNINGS_CONFIG.timezone, "yyyy-MM-dd'T'HH:mm:ss");

      rowsToUpsert.push({
        id: dailyId,
        employeeId: Number(item.employee.id),
        type: 'EARNING',
        operationDate: date + 'T00:00:00',
        periodFrom: date,
        periodTo: date,
        salesCount: salesCount,
        amount: salesCount * DAILY_EARNINGS_CONFIG.earningsPerSale,
        comment: 'Автоматическое начисление по продажам Vendista',
        createdAt: existingDailyRow && existingDailyRow.createdAt
          ? existingDailyRow.createdAt
          : now,
        updatedAt: now,
      });
    });

    if (rowsToUpsert.length > 0) {
      upsertEntities(SHEETS.EMPLOYEES_TRANSACTIONS.name, rowsToUpsert);
      rowsWritten += rowsToUpsert.length;
      Logger.log('Daily earning rows saved for ' + date + ': ' + rowsToUpsert.length);
    }
  });

  if (rowsWritten === 0) {
    Logger.log('No daily earnings rows to write.');
    return;
  }

  Logger.log('Daily earning rows inserted/updated: ' + rowsWritten);
}

function loadDailyEarningsContext_() {
  const employees = readSheet(SHEETS.EMPLOYEES);
  const assignments = readSheet(SHEETS.EMPLOYEE_STATION_ASSIGNMENTS);
  const stations = readSheet(SHEETS.COFFEE_STATION);
  const transactions = readSheet(SHEETS.EMPLOYEES_TRANSACTIONS);
  const stationsById = {};
  const transactionsById = {};

  stations.forEach(function(station) {
    stationsById[String(station.id)] = station;
  });
  transactions.forEach(function(transaction) {
    transactionsById[String(transaction.id)] = transaction;
  });

  return {
    employees: employees,
    assignments: assignments,
    stationsById: stationsById,
    transactions: transactions,
    transactionsById: transactionsById,
  };
}

function getEmployeesForDailyEarnings_(date, context) {
  const activeOperators = context.employees.filter(isActiveOperator_).filter(function(employee) {
    const workStartDate = dailyEarningsDateKey_(employee.workStartDate);
    return workStartDate && workStartDate <= date;
  });
  const assignedStationOwners = {};

  return activeOperators.map(function(employee) {
    const dailyId = 'AUTO-EARNING-' + employee.id + '-' + date;
    const hasExistingDailyRow = Boolean(context.transactionsById[dailyId]);
    const isCoveredByManualEarning = hasManualEarningCoveringDate_(
      context.transactions,
      employee.id,
      date,
      dailyId
    );
    if (isCoveredByManualEarning && !hasExistingDailyRow) return null;

    const employeeId = String(employee.id);
    const assignments = context.assignments.filter(function(assignment) {
      if (String(assignment.employeeId) !== employeeId) return false;

      const validFrom = dailyEarningsDateKey_(assignment.validFrom);
      const validTo = dailyEarningsDateKey_(assignment.validTo);
      return validFrom && validFrom <= date && (!validTo || validTo >= date);
    });

    const terminalIds = [];
    assignments.forEach(function(assignment) {
      const stationId = String(assignment.stationId);
      const station = context.stationsById[stationId];
      if (!station || isTrue_(station.excludeFromOperatorEarnings)) return;

      const terminalId = String(station.terminalId || '').trim();
      if (!terminalId) return;

      const existingOwner = assignedStationOwners[stationId];
      if (existingOwner && existingOwner !== employeeId) {
        throw new Error(
          'Station ' + stationId + ' is assigned to multiple operators on ' + date
        );
      }
      assignedStationOwners[stationId] = employeeId;
      terminalIds.push(terminalId);
    });

    const uniqueTerminalIds = Array.from(new Set(terminalIds));
    if (!hasExistingDailyRow && !isCoveredByManualEarning && uniqueTerminalIds.length === 0) {
      throw new Error('No eligible station assignment for operator ' + employeeId + ' on ' + date);
    }

    return {
      employee: employee,
      terminalIds: uniqueTerminalIds,
    };
  }).filter(function(item) {
    if (!item) return false;
    const dailyId = 'AUTO-EARNING-' + item.employee.id + '-' + date;
    return item.terminalIds.length > 0 || context.transactionsById[dailyId];
  });
}

function loadVendistaSalesForDate_(date) {
  const dateFrom = date + ' 00:00:00';
  const dateTo = date + ' 23:59:59';
  return fetchVendistaSales_(dateFrom, dateTo, date).salesByTerminal;
}

function hasManualEarningCoveringDate_(transactions, employeeId, date, dailyId) {
  return transactions.some(function(transaction) {
    if (String(transaction.employeeId) !== String(employeeId)) return false;
    if (String(transaction.type).toUpperCase() !== 'EARNING') return false;
    if (String(transaction.id) === dailyId) return false;

    const periodFrom = dailyEarningsDateKey_(transaction.periodFrom);
    const periodTo = dailyEarningsDateKey_(transaction.periodTo);
    return periodFrom && periodTo && periodFrom <= date && periodTo >= date;
  });
}

function isActiveOperator_(employee) {
  return String(employee.role).toUpperCase() === 'OPERATOR' && isTrue_(employee.isActive);
}

function isTrue_(value) {
  return value === true || String(value).toLowerCase() === 'true' || String(value) === '1';
}

function getDailyEarningsToday_() {
  return Utilities.formatDate(new Date(), DAILY_EARNINGS_CONFIG.timezone, 'yyyy-MM-dd');
}

function shiftDailyEarningsDate_(date, days) {
  const parts = date.split('-').map(Number);
  const shifted = new Date(Date.UTC(parts[0], parts[1] - 1, parts[2] + days));
  return Utilities.formatDate(shifted, 'UTC', 'yyyy-MM-dd');
}

function dailyEarningsDateKey_(value) {
  if (value === null || value === undefined || value === '') return '';

  if (Object.prototype.toString.call(value) === '[object Date]') {
    return Utilities.formatDate(value, DAILY_EARNINGS_CONFIG.timezone, 'yyyy-MM-dd');
  }

  const text = String(value).trim();
  const isoMatch = text.match(/^(\d{4}-\d{2}-\d{2})/);
  if (isoMatch) return isoMatch[1];

  const ruMatch = text.match(/^(\d{2})\.(\d{2})\.(\d{4})/);
  if (ruMatch) return ruMatch[3] + '-' + ruMatch[2] + '-' + ruMatch[1];

  return '';
}
