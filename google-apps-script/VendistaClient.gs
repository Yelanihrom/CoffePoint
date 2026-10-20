function fetchVendistaSales_(dateFrom, dateTo, context) {
  const token = PropertiesService.getScriptProperties()
    .getProperty(CONFIG.SCRIPT_PROPERTY_KEYS.VENDISTA_TOKEN);
  if (!token) {
    throw new Error(
      'Missing Script Property: ' + CONFIG.SCRIPT_PROPERTY_KEYS.VENDISTA_TOKEN
    );
  }

  let url = CONFIG.VENDISTA.API_URL
    + '?token=' + encodeURIComponent(token)
    + '&DateFrom=' + encodeURIComponent(dateFrom);
  if (dateTo) {
    url += '&DateTo=' + encodeURIComponent(dateTo);
  }

  let response;
  let status;
  let body;
  for (let attempt = 0; attempt <= CONFIG.VENDISTA.MAX_RETRIES; attempt += 1) {
    response = UrlFetchApp.fetch(url, {
      method: 'get',
      muteHttpExceptions: true,
    });
    status = response.getResponseCode();
    body = response.getContentText();

    const retryable = status === 429 || status === 500 || status === 502
      || status === 503 || status === 504;
    if (!retryable || attempt === CONFIG.VENDISTA.MAX_RETRIES) break;

    const delayMs = CONFIG.VENDISTA.RETRY_DELAYS_MS[attempt];
    Logger.log(
      'Vendista HTTP ' + status + ' for ' + context +
      '; retrying in ' + delayMs + ' ms.'
    );
    Utilities.sleep(delayMs);
  }

  if (status !== 200) {
    throw new Error(
      'Vendista HTTP ' + status + ' for ' + context + ': ' + body.slice(0, 500)
    );
  }

  const result = JSON.parse(body);
  if (result.success !== true || !Array.isArray(result.items)) {
    throw new Error('Unexpected Vendista response for ' + context);
  }
  if (Number(result.items_count) > result.items.length) {
    throw new Error(
      'Vendista response appears paginated for ' + context +
      '; refusing to save partial sales.'
    );
  }

  const salesByTerminal = {};
  result.items.forEach(function(item) {
    salesByTerminal[String(item.terminal_id)] = Number(item.incoming_count) || 0;
  });

  return { salesByTerminal: salesByTerminal };
}
