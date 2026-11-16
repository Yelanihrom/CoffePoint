package ru.coffeepoint.core.sync.google

import android.util.Log
import ru.coffeepoint.core.data.local.dao.CoffeeStationDao
import ru.coffeepoint.core.data.local.dao.StationInventoryDao
import ru.coffeepoint.core.data.local.dao.StationSettingsDao
import ru.coffeepoint.core.data.local.dao.StationMaintenanceDao
import ru.coffeepoint.core.data.local.entity.StationMaintenanceEntity
import ru.coffeepoint.core.data.mapper.toStationDetail

class GoogleSheetsStationSyncService(
    private val stationDao: CoffeeStationDao,
    private val inventoryDao: StationInventoryDao,
    private val settingsDao: StationSettingsDao,
    private val maintenanceDao: StationMaintenanceDao,
    private val syncManager: GoogleSheetsSyncManager = GoogleSheetsSyncManager()
) {
    suspend fun syncPendingStations(): GoogleSheetsEntitySyncResult {
        val ids = (stationDao.getUnsynced().map { it.id } + inventoryDao.getUnsynced().map { it.stationId } +
            settingsDao.getUnsynced().map { it.stationId } + maintenanceDao.getUnsynced().map { it.stationId }).distinct()
        ids.forEach { syncStation(it) }
        val errors = stationDao.getUnsynced().map { it.syncErrorType } + inventoryDao.getUnsynced().map { it.syncErrorType } +
            settingsDao.getUnsynced().map { it.syncErrorType } + maintenanceDao.getUnsynced().map { it.syncErrorType }
        if (errors.isEmpty()) return GoogleSheetsEntitySyncResult.success()
        return GoogleSheetsEntitySyncResult.error(when {
            GoogleSheetsSyncErrorType.CONNECTION.storageValue in errors -> GoogleSheetsSyncErrorType.CONNECTION
            GoogleSheetsSyncErrorType.CONFLICT.storageValue in errors -> GoogleSheetsSyncErrorType.CONFLICT
            else -> GoogleSheetsSyncErrorType.UNKNOWN
        })
    }

    suspend fun syncOperatorServiceFields(stationId: Int): Boolean {
        val station = stationDao.getById(stationId) ?: return false
        val detail = toStationDetail(station, inventoryDao.getByStationId(stationId) ?: return false,
            settingsDao.getByStationId(stationId) ?: return false, maintenanceDao.getByStationId(stationId))
        val result = syncManager.syncOperatorServiceFields(detail.station)
        if (result.synced) stationDao.markServiceFieldsSynced(stationId)
        else markStationError(stationId, result)
        return result.synced
    }

    suspend fun syncSupplyServiceFields(stationId: Int): Boolean {
        val rows = maintenanceDao.getByStationId(stationId).filter { !it.isSynced }
        val result = if (rows.isEmpty()) GoogleSheetsEntitySyncResult.success() else syncManager.syncMaintenance(rows)
        updateMaintenanceSyncStatus(rows, result)
        if (result.synced) stationDao.markSupplyServiceFieldsSynced(stationId)
        else markStationError(stationId, result)
        return result.synced && maintenanceDao.getByStationId(stationId).none { !it.isSynced }
    }

    suspend fun syncStation(stationId: Int): Boolean {
        val station = stationDao.getById(stationId) ?: return false
        val inventory = inventoryDao.getByStationId(stationId) ?: return false
        val settings = settingsDao.getByStationId(stationId) ?: return false
        val rows = maintenanceDao.getByStationId(stationId)
        val pending = rows.filter { !it.isSynced }
        val detail = toStationDetail(station, inventory, settings, rows)
        if (station.isSynced && inventory.isSynced && settings.isSynced && pending.isEmpty()) return true
        Log.d(TAG, "Sync station=$stationId pendingNodes=${pending.size}")

        var stationResult = GoogleSheetsEntitySyncResult.success()
        if (!station.isSynced && station.pendingServiceOnly) {
            stationResult = syncManager.syncOperatorServiceFields(detail.station)
            if (stationResult.synced) stationDao.markServiceFieldsSynced(stationId)
        }
        val needsInfo = !station.isSynced && (station.pendingFullStationSync ||
            (!station.pendingServiceOnly && !station.pendingSupplyServiceOnly))
        if (stationResult.synced && needsInfo) {
            stationResult = syncManager.syncStationDetail(detail, true, false, false).station
        }
        // Station info and node acknowledgements have independent versions.
        val nodeResult = when {
            !stationResult.synced -> stationResult
            pending.isEmpty() -> GoogleSheetsEntitySyncResult.success()
            else -> syncManager.syncMaintenance(pending)
        }
        updateMaintenanceSyncStatus(pending, nodeResult)
        if (!station.isSynced) {
            if (stationResult.synced && nodeResult.synced) {
                stationDao.updateSyncStatus(stationId, true, false, "")
                stationDao.clearPendingServiceOnly(stationId)
                stationDao.clearPendingFullStationSync(stationId)
                stationDao.markSupplyServiceFieldsSynced(stationId)
            } else markStationError(stationId, if (!stationResult.synced) stationResult else nodeResult)
        }
        val related = syncManager.syncStationDetail(detail, false, !inventory.isSynced, !settings.isSynced)
        if (!inventory.isSynced) inventoryDao.updateSyncStatus(stationId, related.inventory.synced,
            !related.inventory.synced, related.inventory.errorType.storageValue)
        if (!settings.isSynced) settingsDao.updateSyncStatus(stationId, related.settings.synced,
            !related.settings.synced, related.settings.errorType.storageValue)
        return stationResult.synced && nodeResult.synced && (inventory.isSynced || related.inventory.synced) &&
            (settings.isSynced || related.settings.synced) && maintenanceDao.getByStationId(stationId).none { !it.isSynced }
    }

    private suspend fun markStationError(id: Int, result: GoogleSheetsEntitySyncResult) {
        stationDao.updateSyncStatus(id, false, true, result.errorType.storageValue)
    }

    private suspend fun updateMaintenanceSyncStatus(rows: List<StationMaintenanceEntity>, result: GoogleSheetsEntitySyncResult) {
        rows.forEach { row ->
            if (result.synced) maintenanceDao.acknowledge(row.stationId, row.nodeType, row.updatedAt, row.lastServiceDate)
            else maintenanceDao.markError(row.stationId, row.nodeType, row.updatedAt, row.lastServiceDate, result.errorType.storageValue)
        }
    }

    private companion object { const val TAG = "GoogleSheetsSync" }
}
