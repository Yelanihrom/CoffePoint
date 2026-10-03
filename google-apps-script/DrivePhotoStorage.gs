function uploadStationPhotos(data) {
  const stationId = data.stationId;

  if (!stationId) {
    throw createHttpError(400, 'Missing stationId');
  }

  const stationFolder = getOrCreateStationFolder(stationId);
  const result = {};

  if (data.machinePhotoBase64) {
    const file = replaceFile(
      stationFolder,
      'machine.jpg',
      data.machinePhotoBase64,
      'image/jpeg'
    );
    result.machinePhotoUrl = file.getUrl();
  }

  if (data.overviewPhotoBase64) {
    const file = replaceFile(
      stationFolder,
      'overview.jpg',
      data.overviewPhotoBase64,
      'image/jpeg'
    );
    result.overviewPhotoUrl = file.getUrl();
  }

  return result;
}

function getOrCreateStationFolder(stationId) {
  const rootFolder = DriveApp.getFolderById(CONFIG.ROOT_FOLDER_ID);
  const photosFolder = getOrCreateFolder(rootFolder, CONFIG.PHOTOS_FOLDER_NAME);

  return getOrCreateFolder(photosFolder, 'station_' + stationId);
}

function getOrCreateFolder(parentFolder, name) {
  const folders = parentFolder.getFoldersByName(name);

  if (folders.hasNext()) {
    return folders.next();
  }

  return parentFolder.createFolder(name);
}

function replaceFile(folder, name, base64Content, mimeType) {
  const files = folder.getFilesByName(name);

  while (files.hasNext()) {
    files.next().setTrashed(true);
  }

  const bytes = Utilities.base64Decode(base64Content);
  const blob = Utilities.newBlob(bytes, mimeType, name);
  const file = folder.createFile(blob);

  file.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);

  return file;
}
