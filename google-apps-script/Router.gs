function handleRequest(e, method) {
  try {
    assertAuthorized(e);

    const request = buildRequest(e, method);
    const action = request.action;

    if (method === 'GET' && action === 'full_sync') {
      return jsonResponse({
        success: true,
        data: getFullSyncData(request.entities),
      });
    }

    if (method === 'POST' && action === 'upsert') {
      upsertEntity(request.entity, request.data);

      return jsonResponse({
        success: true,
      });
    }

    if (method === 'POST' && action === 'upload_photos') {
      const photoUrls = uploadStationPhotos(request.data);

      return jsonResponse({
        success: true,
        data: photoUrls,
      });
    }

    throw createHttpError(400, 'Unsupported action');
  } catch (error) {
    return errorResponse(error);
  }
}

function buildRequest(e, method) {
  if (method === 'GET') {
    return {
      action: e.parameter.action,
      entity: e.parameter.entity,
      entities: e.parameter.entities,
      data: {},
    };
  }

  return parseBody(e);
}

function parseBody(e) {
  const content = e && e.postData && e.postData.contents;

  if (!content) return {};

  return JSON.parse(content);
}
