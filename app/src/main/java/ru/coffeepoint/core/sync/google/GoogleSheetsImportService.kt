package ru.coffeepoint.core.sync.google

import androidx.room.withTransaction
import ru.coffeepoint.core.data.local.AppDatabase

import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toSettingsEntity
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.core.data.mapper.importMaintenance

class GoogleSheetsImportService(
    private val db: AppDatabase,
    private val syncManager: GoogleSheetsSyncManager = GoogleSheetsSyncManager()
) {
    private val stationDao = db.coffeeStationDao()
    private val inventoryDao = db.stationInventoryDao()
    private val settingsDao = db.stationSettingsDao()
    private val supplyCatalogDao = db.supplyCatalogDao()
    private val warehouseStockDao = db.warehouseStockDao()
    private val maintenanceDao = db.stationMaintenanceDao()

    suspend fun importFullSync(
        includeSupplyCatalog: Boolean = true,
        includeEmployeeData: Boolean = true
    ): GoogleSheetsImportResult {
        if (
            hasUnsyncedChanges(
                includeSupplyCatalog = includeSupplyCatalog
            )
        ) {
            return GoogleSheetsImportResult.SkippedPendingChanges
        }

        val remoteData = try {
            syncManager.loadFullSyncBundle(
                includeSupplyCatalog = includeSupplyCatalog,
                includeEmployeeData = includeEmployeeData
            )
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            return GoogleSheetsImportResult.Error(error)
        }

        val remoteStations = remoteData.stations
        if (remoteStations.isEmpty()) return GoogleSheetsImportResult.Empty

        try {
            db.withTransaction {
                if (hasUnsyncedChanges(includeSupplyCatalog)) return@withTransaction false
                if (includeEmployeeData) EmployeeDataImporter(db).import(remoteData)
                inventoryDao.deleteAll()
                settingsDao.deleteAll()
                val remoteIds = remoteStations.map { it.station.id }.toSet()
                stationDao.getAll().filter { it.id !in remoteIds }.forEach { stationDao.deleteById(it.id) }

                remoteStations.forEach { detail ->
                    stationDao.insert(detail.toStationEntity())
                    maintenanceDao.importMaintenance(detail.station.id, remoteData.stationMaintenance)
                    inventoryDao.insert(detail.toInventoryEntity())
                    settingsDao.insert(detail.toSettingsEntity())
                }

                if (includeSupplyCatalog) {
                    supplyCatalogDao.deleteAll()
                    supplyCatalogDao.insertAll(remoteData.supplyCatalog)
                }

                warehouseStockDao.insertAll(remoteData.warehouseStock)
                true
            }.let { applied -> if (!applied) return GoogleSheetsImportResult.SkippedPendingChanges }
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) { throw cancellation }
        catch (error: Exception) {
            return GoogleSheetsImportResult.Error(error)
        }

        return GoogleSheetsImportResult.Success
    }

    private suspend fun hasUnsyncedChanges(
        includeSupplyCatalog: Boolean
    ): Boolean {
        return stationDao.getUnsynced().isNotEmpty() ||
            maintenanceDao.getUnsynced().isNotEmpty() ||
            inventoryDao.getUnsynced().isNotEmpty() ||
            settingsDao.getUnsynced().isNotEmpty() ||
            (includeSupplyCatalog && supplyCatalogDao.getUnsynced().isNotEmpty()) ||
            warehouseStockDao.getUnsynced().isNotEmpty()
    }

}

sealed interface GoogleSheetsImportResult {

    data object Success : GoogleSheetsImportResult

    data object SkippedPendingChanges : GoogleSheetsImportResult

    data object Empty : GoogleSheetsImportResult

    data class Error(val cause: Throwable) : GoogleSheetsImportResult

}
