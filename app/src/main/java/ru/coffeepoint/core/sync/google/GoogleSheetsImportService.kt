package ru.coffeepoint.core.sync.google

import ru.coffeepoint.core.data.local.dao.CoffeeStationDao
import ru.coffeepoint.core.data.local.dao.EmployeeDao
import ru.coffeepoint.core.data.local.dao.EmployeeStationAssignmentDao
import ru.coffeepoint.core.data.local.dao.EmployeeTransactionDao
import ru.coffeepoint.core.data.local.dao.StationInventoryDao
import ru.coffeepoint.core.data.local.dao.StationSettingsDao
import ru.coffeepoint.core.data.local.dao.SupplyCatalogDao
import ru.coffeepoint.core.data.local.dao.WarehouseStockDao
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toSettingsEntity
import ru.coffeepoint.core.data.mapper.toStationDetail
import ru.coffeepoint.core.data.mapper.toStationEntity

class GoogleSheetsImportService(
    private val stationDao: CoffeeStationDao,
    private val inventoryDao: StationInventoryDao,
    private val settingsDao: StationSettingsDao,
    private val supplyCatalogDao: SupplyCatalogDao,
    private val warehouseStockDao: WarehouseStockDao,
    private val employeeDao: EmployeeDao,
    private val employeeStationAssignmentDao: EmployeeStationAssignmentDao,
    private val employeeTransactionDao: EmployeeTransactionDao,
    private val syncManager: GoogleSheetsSyncManager = GoogleSheetsSyncManager()
) {

    suspend fun importFullSync(): GoogleSheetsImportResult {
        if (hasUnsyncedChanges()) return GoogleSheetsImportResult.SkippedPendingChanges

        val remoteData = runCatching {
            syncManager.loadFullSyncBundle()
        }.getOrElse {
            return GoogleSheetsImportResult.Error
        }

        val remoteStations = remoteData.stations
        if (remoteStations.isEmpty()) return GoogleSheetsImportResult.Empty

        val localSalesByStationId = getStationDetails().associate { detail ->
            detail.station.id to detail.station.salesAfterService
        }

        supplyCatalogDao.deleteAll()
        inventoryDao.deleteAll()
        settingsDao.deleteAll()
        stationDao.deleteAll()

        remoteStations.forEach { detail ->
            val mergedDetail = detail.copy(
                station = detail.station.copy(
                    salesAfterService = localSalesByStationId[detail.station.id] ?: 0
                )
            )

            stationDao.insert(mergedDetail.toStationEntity())
            inventoryDao.insert(mergedDetail.toInventoryEntity())
            settingsDao.insert(mergedDetail.toSettingsEntity())
        }

        supplyCatalogDao.insertAll(remoteData.supplyCatalog)
        warehouseStockDao.insertAll(remoteData.warehouseStock)
        employeeDao.insertAll(remoteData.employees)
        employeeStationAssignmentDao.insertAll(remoteData.employeeStationAssignments)
        employeeTransactionDao.insertAll(remoteData.employeeTransactions)

        return GoogleSheetsImportResult.Success
    }

    private suspend fun hasUnsyncedChanges(): Boolean {
        return stationDao.getUnsynced().isNotEmpty() ||
            inventoryDao.getUnsynced().isNotEmpty() ||
            settingsDao.getUnsynced().isNotEmpty() ||
            supplyCatalogDao.getUnsynced().isNotEmpty() ||
            warehouseStockDao.getUnsynced().isNotEmpty()
    }

    private suspend fun getStationDetails() = stationDao.getAll().mapNotNull { station ->
        val inventory = inventoryDao.getByStationId(station.id)
            ?: return@mapNotNull null
        val settings = settingsDao.getByStationId(station.id)
            ?: return@mapNotNull null

        toStationDetail(
            station,
            inventory,
            settings
        )
    }

}

sealed interface GoogleSheetsImportResult {

    data object Success : GoogleSheetsImportResult

    data object SkippedPendingChanges : GoogleSheetsImportResult

    data object Empty : GoogleSheetsImportResult

    data object Error : GoogleSheetsImportResult

}
