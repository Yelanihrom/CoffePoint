function assertAuthorized(e) {
  const token = getRequestToken(e);
  const expectedToken = PropertiesService.getScriptProperties()
    .getProperty(CONFIG.SCRIPT_PROPERTY_KEYS.SYNC_TOKEN);

  if (!expectedToken) {
    throw createHttpError(500, 'SYNC_TOKEN Script Property is not configured');
  }

  if (token !== expectedToken) {
    throw createHttpError(401, 'Unauthorized');
  }
}

function getRequestToken(e) {
  const parameterToken = e && e.parameter && e.parameter.token;
  const body = parseBody(e);

  return parameterToken || body.token || '';
}
