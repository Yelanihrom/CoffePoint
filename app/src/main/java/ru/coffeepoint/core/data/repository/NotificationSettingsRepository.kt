package ru.coffeepoint.core.data.repository

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.coffeepoint.core.notification.NotificationSettings
import ru.coffeepoint.core.notification.NotificationSettingsRow
import ru.coffeepoint.core.sync.google.*

class NotificationSettingsRepository(
    private val settings: NotificationSettings,
    private val load: suspend () -> List<NotificationSettingsRow>,
    private val send: suspend (NotificationSettingsRow) -> GoogleSheetsEntitySyncResult,
    private val enabled: Boolean = true
) {
    constructor(context: Context, manager: GoogleSheetsSyncManager = GoogleSheetsSyncManager()) : this(
        NotificationSettings(context), manager::loadNotificationSettings, manager::syncNotificationSettings
    )

    suspend fun refresh() {
        if (enabled) withContext(Dispatchers.IO) { mutex.withLock { settings.importRows(load()) } }
    }

    suspend fun syncPending(): GoogleSheetsEntitySyncResult {
        if (!enabled) return GoogleSheetsEntitySyncResult.success()
        return withContext(Dispatchers.IO) { mutex.withLock {
            try {
                var failure: GoogleSheetsEntitySyncResult? = null
                for (snapshot in settings.pending()) {
                    var row = snapshot
                    var result = send(row)
                    if (!result.synced && result.errorType == GoogleSheetsSyncErrorType.CONFLICT) {
                        settings.importRows(load())
                        row = settings.pending().firstOrNull { it.role == snapshot.role } ?: continue
                        result = send(row)
                    }
                    if (result.synced) settings.acknowledge(row)
                    else if (failure == null || result.errorType == GoogleSheetsSyncErrorType.CONNECTION) failure = result
                }
                failure ?: GoogleSheetsEntitySyncResult.success()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                GoogleSheetsEntitySyncResult.error(if (error is java.io.IOException)
                    GoogleSheetsSyncErrorType.CONNECTION else GoogleSheetsSyncErrorType.UNKNOWN)
            }
        } }
    }

    private companion object { val mutex = Mutex() }
}
