package ru.coffeepoint.core.sync.google

import android.content.Context
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeStationAssignmentEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.sync.google.worker.GoogleSheetsSyncScheduler
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleSheetsSyncManager(
    private val remoteDataSource: GoogleSheetsRemoteDataSource =
        GoogleSheetsRemoteDataSource()
) {

    suspend fun syncStationDetail(
        detail: StationDetail,
        includeStation: Boolean,
        includeInventory: Boolean,
        includeSettings: Boolean
    ): GoogleSheetsSyncResult {
        return remoteDataSource.syncStationDetail(
            detail = detail,
            includeStation = includeStation,
            includeInventory = includeInventory,
            includeSettings = includeSettings
        )
    }

    suspend fun loadFullSyncBundle(): GoogleSheetsFullSyncBundle {
        return remoteDataSource.loadFullSyncBundle()
    }

    fun enqueueBackgroundSync(context: Context) {
        GoogleSheetsSyncScheduler.enqueueSync(context)
    }

    suspend fun loadSupplyCatalog(): List<SupplyCatalogEntity> {
        return remoteDataSource.loadSupplyCatalog()
    }

    suspend fun upsertSupplyCatalogOption(
        option: SupplyCatalogEntity,
        updatedAt: String
    ): GoogleSheetsEntitySyncResult {
        return remoteDataSource.upsertSupplyCatalogOption(
            option = option,
            updatedAt = updatedAt
        )
    }

    suspend fun upsertSupplyCatalogItem(
        item: SupplyCatalogEntity
    ): GoogleSheetsSyncedEntity<SupplyCatalogEntity> {
        val updatedAt = currentTimestamp()
        val syncedItem = item.copy(
            updatedAt = updatedAt,
            isSynced = true,
            syncError = false,
            syncErrorType = ""
        )

        return GoogleSheetsSyncedEntity(
            entity = syncedItem,
            result = remoteDataSource.upsertSupplyCatalogOption(
                option = syncedItem,
                updatedAt = updatedAt
            )
        )
    }

    suspend fun deleteSupplyCatalogOption(
        optionId: Int
    ): GoogleSheetsEntitySyncResult {
        return remoteDataSource.deleteSupplyCatalogOption(optionId)
    }

    suspend fun syncWarehouseStock(
        items: List<WarehouseStockEntity>
    ): GoogleSheetsEntitySyncResult {
        return remoteDataSource.syncWarehouseStock(items)
    }

    suspend fun syncEmployeesAndAssignments(
        employees: List<EmployeeEntity>,
        assignments: List<EmployeeStationAssignmentEntity>
    ): GoogleSheetsEntitySyncResult {
        return remoteDataSource.syncEmployeesAndAssignments(
            employees = employees,
            assignments = assignments
        )
    }

    suspend fun syncEmployeeTransactions(
        transactions: List<EmployeeTransactionEntity>
    ): GoogleSheetsEntitySyncResult {
        return remoteDataSource.syncEmployeeTransactions(transactions)
    }

    suspend fun syncWarehouseStockItems(
        items: List<WarehouseStockItem>
    ): GoogleSheetsSyncedEntities<WarehouseStockEntity> {
        val updatedAt = currentTimestamp()
        val entities = items.map { item ->
            WarehouseStockEntity(
                itemType = item.itemType,
                quantity = item.quantity.coerceAtLeast(0),
                updatedAt = updatedAt
            )
        }

        return GoogleSheetsSyncedEntities(
            entities = entities,
            result = remoteDataSource.syncWarehouseStock(entities)
        )
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

}

data class GoogleSheetsSyncedEntity<T>(
    val entity: T,
    val result: GoogleSheetsEntitySyncResult
)

data class GoogleSheetsSyncedEntities<T>(
    val entities: List<T>,
    val result: GoogleSheetsEntitySyncResult
)
