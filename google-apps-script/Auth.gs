function assertAuthorized(e) {
  const token = getRequestToken(e);

  if (!CONFIG.SYNC_TOKEN || CONFIG.SYNC_TOKEN === 'REPLACE_WITH_SYNC_TOKEN') {
    throw createHttpError(500, 'SYNC_TOKEN is not configured');
  }

  if (token !== CONFIG.SYNC_TOKEN) {
    throw createHttpError(401, 'Unauthorized');
  }
}

function getRequestToken(e) {
  const parameterToken = e && e.parameter && e.parameter.token;
  const body = parseBody(e);

  return parameterToken || body.token || '';
}
