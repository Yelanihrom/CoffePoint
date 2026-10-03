package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncMessages
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupplyCatalogRepository(
    context: Context
) {

    private val supplyCatalogDao =
        DatabaseProvider.getDatabase(context).supplyCatalogDao()
    private val googleSheetsSyncManager = GoogleSheetsSyncManager()

    suspend fun getItems(): List<SupplyCatalogEntity> {
        return supplyCatalogDao.getAll()
    }

    suspend fun getItem(id: Int): SupplyCatalogEntity? {
        return supplyCatalogDao.getById(id)
    }

    suspend fun getNextItemId(): Int {
        return supplyCatalogDao.getNextId()
    }

    suspend fun saveItem(item: SupplyCatalogEntity) {
        val itemId = if (item.id > 0) {
            item.id
        } else {
            supplyCatalogDao.getNextId()
        }

        supplyCatalogDao.insert(
            item.copy(
                id = itemId,
                updatedAt = currentTimestamp(),
                isSynced = false,
                syncError = false,
                syncErrorType = ""
            )
        )
    }

    suspend fun upsertItemNow(item: SupplyCatalogEntity): GoogleSheetsEntitySyncResult {
        val sync = googleSheetsSyncManager.upsertSupplyCatalogItem(item)

        if (sync.result.synced) {
            supplyCatalogDao.insert(sync.entity)
        }

        return sync.result
    }

    suspend fun deleteItemNow(itemId: Int): GoogleSheetsEntitySyncResult {
        val result = googleSheetsSyncManager.deleteSupplyCatalogOption(itemId)

        if (result.synced) {
            supplyCatalogDao.deleteById(itemId)
        }

        return result
    }

    fun syncErrorMessage(errorType: GoogleSheetsSyncErrorType): String {
        return GoogleSheetsSyncMessages.errorMessage(errorType)
    }

    suspend fun refreshFromGoogleSheets(): Boolean {
        val remoteItems = runCatching {
            googleSheetsSyncManager.loadSupplyCatalog()
        }.getOrElse {
            return false
        }

        supplyCatalogDao.deleteAll()
        supplyCatalogDao.insertAll(remoteItems)

        return true
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

}
