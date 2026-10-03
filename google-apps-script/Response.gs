function jsonResponse(payload) {
  return ContentService
    .createTextOutput(JSON.stringify(payload))
    .setMimeType(ContentService.MimeType.JSON);
}

function errorResponse(error) {
  const status = error.status || 500;
  const message = error.message || 'Internal error';

  return jsonResponse({
    success: false,
    error: {
      status: status,
      message: message,
    },
  });
}

function createHttpError(status, message) {
  const error = new Error(message);
  error.status = status;
  return error;
}
