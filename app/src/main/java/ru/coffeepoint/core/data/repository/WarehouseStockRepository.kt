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
            WarehouseStockItem(
                itemType = itemType.code,
                title = itemType.title,
                quantity = quantities[itemType.code]?.quantity ?: 0,
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

    suspend fun applyWriteOffNow(
        writeOffItems: List<WarehouseStockWriteOff>
    ): GoogleSheetsEntitySyncResult {
        ensureDefaults()

        val writeOffByType = writeOffItems
            .filter { item -> item.amount > 0 }
            .associate { item -> item.itemType to item.amount }

        val updatedAt = currentTimestamp()
        val entities = dao.getAll().map { entity ->
            val amount = writeOffByType[entity.itemType] ?: 0
            entity.copy(
                quantity = (entity.quantity - amount).coerceAtLeast(0),
                updatedAt = if (amount > 0) updatedAt else entity.updatedAt,
                isSynced = if (amount > 0) false else entity.isSynced,
                syncError = if (amount > 0) false else entity.syncError,
                syncErrorType = if (amount > 0) {
                    GoogleSheetsSyncDefaults.noErrorType
                } else {
                    entity.syncErrorType
                }
            )
        }

        dao.insertAll(entities)

        val changedEntities = entities.filter { entity ->
            writeOffByType.containsKey(entity.itemType)
        }
        val result = googleSheetsSyncManager.syncWarehouseStock(changedEntities)
        updateSyncStatus(
            items = changedEntities,
            result = result
        )

        return result
    }

    suspend fun getShortages(
        writeOffItems: List<WarehouseStockWriteOff>
    ): List<WarehouseStockShortage> {
        ensureDefaults()

        val quantities = dao.getAll()
            .associate { entity -> entity.itemType to entity.quantity }

        return writeOffItems
            .filter { item -> item.amount > 0 }
            .mapNotNull { item ->
                val available = quantities[item.itemType] ?: 0

                if (available < item.amount) {
                    WarehouseStockShortage(
                        itemType = item.itemType,
                        required = item.amount,
                        available = available
                    )
                } else {
                    null
                }
            }
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

    suspend fun syncPendingGoogleSheets() {
        ensureDefaults()

        val unsyncedItems = dao.getUnsynced()
        if (unsyncedItems.isEmpty()) return

        val result = googleSheetsSyncManager.syncWarehouseStock(unsyncedItems)
        updateSyncStatus(
            items = unsyncedItems,
            result = result
        )
    }

    private suspend fun ensureDefaults() {
        dao.insertMissing(
            WarehouseItemType.entries.map { itemType ->
                WarehouseStockEntity(
                    itemType = itemType.code,
                    quantity = 0,
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
}

data class WarehouseStockWriteOff(
    val itemType: String,
    val amount: Int
)

data class WarehouseStockShortage(
    val itemType: String,
    val required: Int,
    val available: Int
)
