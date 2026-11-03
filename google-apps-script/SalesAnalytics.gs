function getSalesAnalytics(startDate, endDate) {
  const start = analyticsDate_(startDate);
  const end = endDate == null || endDate === '' ? null : analyticsDate_(endDate);
  const today = Utilities.formatDate(new Date(), CONFIG.TIMEZONE, 'yyyy-MM-dd');
  if (start > today || (end && (end < start || end > today))) {
    throw createHttpError(400, 'Invalid analytics period');
  }

  const stations = readSheet(SHEETS.COFFEE_STATION);
  const terminals = new Set();
  stations.forEach(function(station) {
    if (!station.terminalId) return;
    const terminal = String(station.terminalId);
    if (terminals.has(terminal)) {
      throw createHttpError(500, 'Duplicate station terminalId');
    }
    terminals.add(terminal);
  });

  const sales = terminals.size ? fetchVendistaSales_(
    start + ' 00:00:00', end ? end + ' 23:59:59' : null, 'sales analytics'
  ).salesByTerminal : {};
  const items = stations.map(function(station) {
    return {
      stationId: Number(station.id),
      name: String(station.name),
      terminalId: station.terminalId ? String(station.terminalId) : null,
      salesCount: station.terminalId ? (sales[String(station.terminalId)] || 0) : null,
    };
  });
  return {
    startDate: start,
    endDate: end,
    generatedAt: new Date().toISOString(),
    totalSales: items.reduce(function(total, item) { return total + (item.salesCount || 0); }, 0),
    stations: items,
  };
}

function analyticsDate_(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw createHttpError(400, 'Expected analytics date yyyy-MM-dd');
  }
  const date = new Date(value + 'T00:00:00Z');
  if (!Number.isFinite(date.getTime()) || date.toISOString().slice(0, 10) !== value) {
    throw createHttpError(400, 'Invalid analytics date');
  }
  return value;
}
