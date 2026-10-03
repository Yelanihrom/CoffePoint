package ru.coffeepoint.feature.station.model

import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType

private const val CONNECTION_ERROR_TEXT =
    "Проверьте подключение к интернету или VPN"

private const val CONFLICT_ERROR_TEXT =
    "На сервере сохранены более свежие данные. Обратитесь к управляющему."

private const val UNKNOWN_ERROR_TEXT =
    "Не удалось отправить изменения. Обратитесь к управляющему."

fun StationDetail.isFullySynced(): Boolean {
    return station.isSynced &&
        inventory.isSynced &&
        settings.isSynced &&
        !hasSyncError()
}

fun StationDetail.hasSyncError(): Boolean {
    return station.syncError ||
        inventory.syncError ||
        settings.syncError
}

fun StationDetail.hasConflictSyncError(): Boolean {
    return syncErrorTypes().any { errorType ->
        errorType == GoogleSheetsSyncErrorType.CONFLICT.storageValue
    }
}

fun StationDetail.hasConnectionSyncError(): Boolean {
    return syncErrorTypes().any { errorType ->
        errorType == GoogleSheetsSyncErrorType.CONNECTION.storageValue
    }
}

fun StationDetail.syncErrorDialogMessage(): String {
    return when {
        hasConflictSyncError() -> CONFLICT_ERROR_TEXT
        hasConnectionSyncError() -> CONNECTION_ERROR_TEXT
        else -> UNKNOWN_ERROR_TEXT
    }
}

fun StationDetail.isSyncPending(): Boolean {
    return !hasSyncError() &&
        (!station.isSynced ||
            !inventory.isSynced ||
            !settings.isSynced)
}

fun StationDetail.syncPartTitles(): List<String> {
    return buildList {
        if (!station.isSynced || station.syncError) {
            add("Продажи/Обслуживание")
        }
        if (!inventory.isSynced || inventory.syncError) {
            add("Остатки")
        }
        if (!settings.isSynced || settings.syncError) {
            add("Лимиты остатков")
        }
    }
}

private fun StationDetail.syncErrorTypes(): List<String> {
    return listOf(
        station.syncErrorType,
        inventory.syncErrorType,
        settings.syncErrorType
    )
}
