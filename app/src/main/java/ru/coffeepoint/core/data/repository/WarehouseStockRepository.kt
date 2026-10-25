package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncDefaults
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WarehouseStockRepository(
    context: Context
) {

    private val dao = DatabaseProvider.getDatabase(context).warehouseStockDao()
    private val googleSheetsSyncManager = GoogleSheetsSyncManager()

    suspend fun getItems(): List<WarehouseStockItem> {
        ensureDefaults()

        val quantities = dao.getAll().associateBy { entity -> entity.itemType }

        return WarehouseItemType.entries.map { itemType ->
            val entity = quantities[itemType.code]

            WarehouseStockItem(
                itemType = itemType.code,
                title = itemType.title,
                quantity = entity?.quantity ?: 0,
                minQuantity = entity?.minQuantity ?: 0,
                maxQuantity = entity?.maxQuantity ?: 0,
                iconRes = itemType.iconRes
            )
        }
    }

    suspend fun getQuantityByType(): Map<String, Int> {
        ensureDefaults()

        return dao.getAll()
            .associate { entity -> entity.itemType to entity.quantity }
    }

    suspend fun updateQuantity(
        itemType: String,
        quantity: Int
    ) {
        dao.insert(
            WarehouseStockEntity(
                itemType = itemType,
                quantity = quantity.coerceAtLeast(0),
                minQuantity = 0,
                maxQuantity = 0,
                updatedAt = currentTimestamp(),
                isSynced = false,
                syncError = false,
                syncErrorType = GoogleSheetsSyncDefaults.noErrorType
            )
        )
    }

    suspend fun replaceAll(
        items: List<WarehouseStockEntity>
    ) {
        dao.insertAll(items)
    }

    suspend fun saveItemsNow(
        items: List<WarehouseStockItem>
    ): GoogleSheetsEntitySyncResult {
        val updatedAt = currentTimestamp()
        val entities = items.map { item ->
            WarehouseStockEntity(
                itemType = item.itemType,
                quantity = item.quantity.coerceAtLeast(0),
                minQuantity = item.minQuantity.coerceAtLeast(0),
                maxQuantity = item.maxQuantity.coerceAtLeast(0),
                updatedAt = updatedAt,
                isSynced = false,
                syncError = false,
                syncErrorType = GoogleSheetsSyncDefaults.noErrorType
            )
        }

        dao.insertAll(entities)

        val result = googleSheetsSyncManager.syncWarehouseStock(entities)
        updateSyncStatus(
            items = entities,
            result = result
        )

        return result
    }

    suspend fun syncPendingNow(): GoogleSheetsEntitySyncResult {
        val items = dao.getUnsynced()
        if (items.isEmpty()) return GoogleSheetsEntitySyncResult.success()
        val result = googleSheetsSyncManager.syncWarehouseStock(items)
        updateSyncStatus(items, result)
        return result
    }

    suspend fun syncCurrentNow(): GoogleSheetsEntitySyncResult {
        ensureDefaults()

        val items = dao.getUnsynced().ifEmpty {
            dao.getAll()
        }
        val result = googleSheetsSyncManager.syncWarehouseStock(items)
        updateSyncStatus(
            items = items,
            result = result
        )

        return result
    }

    suspend fun refreshFromGoogleSheetsNow(): GoogleSheetsEntitySyncResult {
        ensureDefaults()

        val unsyncedItems = dao.getUnsynced()
        if (unsyncedItems.isNotEmpty()) {
            val pendingSyncResult = googleSheetsSyncManager.syncWarehouseStock(unsyncedItems)
            updateSyncStatus(
                items = unsyncedItems,
                result = pendingSyncResult
            )

            if (!pendingSyncResult.synced) {
                return pendingSyncResult
            }
        }

        return runCatching {
            googleSheetsSyncManager.loadFullSyncBundle(
                includeStations = false,
                includeSettings = false,
                includeSupplyCatalog = false,
                includeWarehouseStock = true,
                includeEmployeeData = false
            ).warehouseStock
        }.fold(
            onSuccess = { remoteItems ->
                dao.insertAll(
                    remoteItems.map { item ->
                        item.copy(
                            isSynced = true,
                            syncError = false,
                            syncErrorType = GoogleSheetsSyncErrorType.NONE.storageValue
                        )
                    }
                )
                ensureDefaults()
                GoogleSheetsEntitySyncResult.success()
            },
            onFailure = { error ->
                GoogleSheetsEntitySyncResult.error(error.toSyncErrorType())
            }
        )
    }

    suspend fun syncPendingGoogleSheets(): GoogleSheetsEntitySyncResult {
        ensureDefaults()

        val unsyncedItems = dao.getUnsynced()
        if (unsyncedItems.isEmpty()) return GoogleSheetsEntitySyncResult.success()

        val result = googleSheetsSyncManager.syncWarehouseStock(unsyncedItems)
        updateSyncStatus(
            items = unsyncedItems,
            result = result
        )
        return result
    }

    private suspend fun ensureDefaults() {
        dao.insertMissing(
            WarehouseItemType.entries.map { itemType ->
                WarehouseStockEntity(
                    itemType = itemType.code,
                    quantity = 0,
                    minQuantity = 0,
                    maxQuantity = 0,
                    updatedAt = "",
                    isSynced = true,
                    syncError = false,
                    syncErrorType = GoogleSheetsSyncDefaults.noErrorType
                )
            }
        )
    }

    private suspend fun updateSyncStatus(
        items: List<WarehouseStockEntity>,
        result: GoogleSheetsEntitySyncResult
    ) {
        items.forEach { item ->
            if (result.synced) {
                dao.updateSyncStatus(
                    itemType = item.itemType,
                    isSynced = true,
                    syncError = false,
                    syncErrorType = GoogleSheetsSyncErrorType.NONE.storageValue
                )
            } else {
                dao.updateSyncStatus(
                    itemType = item.itemType,
                    isSynced = false,
                    syncError = true,
                    syncErrorType = result.errorType.storageValue
                )
            }
        }
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    private fun Throwable.toSyncErrorType(): GoogleSheetsSyncErrorType {
        val message = message.orEmpty()

        return when {
            this is UnknownHostException ||
                this is SocketTimeoutException ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                GoogleSheetsSyncErrorType.CONNECTION

            message.contains("Conflict:", ignoreCase = true) ->
                GoogleSheetsSyncErrorType.CONFLICT

            else -> GoogleSheetsSyncErrorType.UNKNOWN
        }
    }
}
