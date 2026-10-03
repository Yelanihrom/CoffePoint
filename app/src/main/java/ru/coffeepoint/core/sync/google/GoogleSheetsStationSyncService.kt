package ru.coffeepoint.core.sync.google

import android.util.Log
import ru.coffeepoint.core.data.local.dao.CoffeeStationDao
import ru.coffeepoint.core.data.local.dao.StationInventoryDao
import ru.coffeepoint.core.data.local.dao.StationSettingsDao
import ru.coffeepoint.core.data.mapper.toStationDetail

class GoogleSheetsStationSyncService(
    private val stationDao: CoffeeStationDao,
    private val inventoryDao: StationInventoryDao,
    private val settingsDao: StationSettingsDao,
    private val syncManager: GoogleSheetsSyncManager = GoogleSheetsSyncManager()
) {

    suspend fun syncPendingStations() {
        val stationIds = pendingStationIds()

        if (stationIds.isEmpty()) {
            Log.d(
                TAG,
                "Google Sheets sync skipped: no pending changes"
            )
            return
        }

        Log.d(
            TAG,
            "Google Sheets sync pending stations: ${stationIds.joinToString()}"
        )

        stationIds.forEach { stationId ->
            syncStation(stationId)
        }
    }

    suspend fun syncStation(stationId: Int): Boolean {
        val station = stationDao.getById(stationId) ?: return false
        val inventory = inventoryDao.getByStationId(stationId) ?: return false
        val settings = settingsDao.getByStationId(stationId) ?: return false
        val detail = toStationDetail(
            station,
            inventory,
            settings
        )

        if (detail.station.isSynced &&
            detail.inventory.isSynced &&
            detail.settings.isSynced
        ) {
            return true
        }

        Log.d(
            TAG,
            "Google Sheets sync station=$stationId station=${!detail.station.isSynced} inventory=${!detail.inventory.isSynced} settings=${!detail.settings.isSynced}"
        )

        val result = syncManager.syncStationDetail(
            detail = detail,
            includeStation = !detail.station.isSynced,
            includeInventory = !detail.inventory.isSynced,
            includeSettings = !detail.settings.isSynced
        )

        updateStationSyncStatus(
            stationId = stationId,
            isPending = !detail.station.isSynced,
            result = result.station
        )
        updateInventorySyncStatus(
            stationId = stationId,
            isPending = !detail.inventory.isSynced,
            result = result.inventory
        )
        updateSettingsSyncStatus(
            stationId = stationId,
            isPending = !detail.settings.isSynced,
            result = result.settings
        )

        return (
            (detail.station.isSynced || result.station.synced) &&
                (detail.inventory.isSynced || result.inventory.synced) &&
                (detail.settings.isSynced || result.settings.synced)
            )
    }

    private suspend fun pendingStationIds(): List<Int> {
        val unsyncedStationIds = stationDao.getUnsynced()
            .map { it.id }
        val unsyncedInventoryIds = inventoryDao.getUnsynced()
            .map { it.stationId }
        val unsyncedSettingsIds = settingsDao.getUnsynced()
            .map { it.stationId }

        return (
            unsyncedStationIds +
                unsyncedInventoryIds +
                unsyncedSettingsIds
            ).distinct()
    }

    private suspend fun updateStationSyncStatus(
        stationId: Int,
        isPending: Boolean,
        result: GoogleSheetsEntitySyncResult
    ) {
        if (result.synced) {
            stationDao.updateSyncStatus(
                id = stationId,
                isSynced = true,
                syncError = false,
                syncErrorType = GoogleSheetsSyncErrorType.NONE.storageValue
            )
        } else if (isPending) {
            stationDao.updateSyncStatus(
                id = stationId,
                isSynced = false,
                syncError = true,
                syncErrorType = result.errorType.storageValue
            )
        }
    }

    private suspend fun updateInventorySyncStatus(
        stationId: Int,
        isPending: Boolean,
        result: GoogleSheetsEntitySyncResult
    ) {
        if (result.synced) {
            inventoryDao.updateSyncStatus(
                stationId = stationId,
                isSynced = true,
                syncError = false,
                syncErrorType = GoogleSheetsSyncErrorType.NONE.storageValue
            )
        } else if (isPending) {
            inventoryDao.updateSyncStatus(
                stationId = stationId,
                isSynced = false,
                syncError = true,
                syncErrorType = result.errorType.storageValue
            )
        }
    }

    private suspend fun updateSettingsSyncStatus(
        stationId: Int,
        isPending: Boolean,
        result: GoogleSheetsEntitySyncResult
    ) {
        if (result.synced) {
            settingsDao.updateSyncStatus(
                stationId = stationId,
                isSynced = true,
                syncError = false,
                syncErrorType = GoogleSheetsSyncErrorType.NONE.storageValue
            )
        } else if (isPending) {
            settingsDao.updateSyncStatus(
                stationId = stationId,
                isSynced = false,
                syncError = true,
                syncErrorType = result.errorType.storageValue
            )
        }
    }

    private companion object {
        const val TAG = "GoogleSheetsSync"
    }

}
