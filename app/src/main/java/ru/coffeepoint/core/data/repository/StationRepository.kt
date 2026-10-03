package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toSettingsEntity
import ru.coffeepoint.core.data.mapper.toStationDetail
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult
import ru.coffeepoint.core.sync.google.GoogleSheetsImportService
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncDefaults
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.core.sync.google.GoogleSheetsStationSyncService
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.station.model.TechServiceNode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StationRepository(
    context: Context
) {

    private val appContext = context.applicationContext
    private val db = DatabaseProvider.getDatabase(context)

    private val stationDao = db.coffeeStationDao()
    private val inventoryDao = db.stationInventoryDao()
    private val settingsDao = db.stationSettingsDao()
    private val googleSheetsSyncManager = GoogleSheetsSyncManager()
    private val googleSheetsImportService = GoogleSheetsImportService(
        stationDao = stationDao,
        inventoryDao = inventoryDao,
        settingsDao = settingsDao,
        supplyCatalogDao = db.supplyCatalogDao(),
        warehouseStockDao = db.warehouseStockDao()
    )
    private val googleSheetsStationSyncService = GoogleSheetsStationSyncService(
        stationDao = stationDao,
        inventoryDao = inventoryDao,
        settingsDao = settingsDao
    )

    suspend fun getStation(id: Int): StationDetail? {

        val station = stationDao.getById(id) ?: return null
        val inventory = inventoryDao.getByStationId(id) ?: return null
        val settings = settingsDao.getByStationId(id) ?: return null

        return toStationDetail(
            station,
            inventory,
            settings
        )

    }

    suspend fun getStations(): List<StationDetail> {

        val result = mutableListOf<StationDetail>()

        stationDao.getAll().forEach { station ->

            val inventory =
                inventoryDao.getByStationId(station.id) ?: return@forEach

            val settings =
                settingsDao.getByStationId(station.id) ?: return@forEach

            result += toStationDetail(
                station,
                inventory,
                settings
            )

        }

        return result

    }

    suspend fun importFromGoogleSheets(): Boolean {

        return importFromGoogleSheetsResult() == GoogleSheetsImportResult.Success

    }

    suspend fun importFromGoogleSheetsResult(): GoogleSheetsImportResult {
        return googleSheetsImportService.importFullSync()
    }

    suspend fun saveStation(
        detail: StationDetail,
        syncImmediately: Boolean = false
    ): Boolean {

        val current = getStation(detail.station.id)
        val updatedAt = currentTimestamp()
        var hasChanges = false

        if (current == null || current.station.hasContentChanges(detail.station)) {
            stationDao.insert(
                detail.copy(
                    station = detail.station.copy(
                        updatedAt = updatedAt,
                        isSynced = false,
                        syncError = false,
                        syncErrorType = GoogleSheetsSyncDefaults.noErrorType
                    )
                ).toStationEntity()
            )
            hasChanges = true
        }

        if (current == null || current.inventory.hasContentChanges(detail.inventory)) {
            inventoryDao.insert(
                detail.copy(
                    inventory = detail.inventory.copy(
                        updatedAt = updatedAt,
                        isSynced = false,
                        syncError = false,
                        syncErrorType = GoogleSheetsSyncDefaults.noErrorType
                    )
                ).toInventoryEntity()
            )
            hasChanges = true
        }

        if (current == null || current.settings.hasContentChanges(detail.settings)) {
            settingsDao.insert(
                detail.copy(
                    settings = detail.settings.copy(
                        updatedAt = updatedAt,
                        isSynced = false,
                        syncError = false,
                        syncErrorType = GoogleSheetsSyncDefaults.noErrorType
                    )
                ).toSettingsEntity()
            )
            hasChanges = true
        }

        if (hasChanges && syncImmediately) {
            return googleSheetsStationSyncService.syncStation(detail.station.id)
        } else if (!hasChanges && syncImmediately) {
            return googleSheetsStationSyncService.syncStation(detail.station.id)
        } else if (hasChanges) {
            googleSheetsSyncManager.enqueueBackgroundSync(appContext)
        }

        return true

    }

    suspend fun syncPendingGoogleSheets() {
        googleSheetsStationSyncService.syncPendingStations()
    }

    suspend fun resolveConflictBlocksForImport() {
        val conflict = GoogleSheetsSyncErrorType.CONFLICT.storageValue

        stationDao.markConflictsAsResolvedForImport(conflict)
        inventoryDao.markConflictsAsResolvedForImport(conflict)
        settingsDao.markConflictsAsResolvedForImport(conflict)
        db.supplyCatalogDao().markConflictsAsResolvedForImport(conflict)
        db.warehouseStockDao().markConflictsAsResolvedForImport(conflict)
    }

    suspend fun syncStationNow(
        stationId: Int
    ): Boolean {
        return googleSheetsStationSyncService.syncStation(stationId)
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    suspend fun addStation(
        detail: StationDetail
    ): StationSaveResult {

        val newId = (stationDao.getMaxId() ?: 0) + 1

        val synced = saveStation(

            detail.copy(

                station = detail.station.copy(
                    id = newId
                )

            ),
            syncImmediately = true

        )

        return StationSaveResult(
            stationId = newId,
            synced = synced
        )

    }

    suspend fun deleteStation(
        stationId: Int
    ) {

        inventoryDao.deleteByStationId(stationId)

        settingsDao.deleteByStationId(stationId)

        stationDao.deleteById(stationId)

    }

    suspend fun finishService(
        stationId: Int,
        machinePhotoPath: String?,
        overviewPhotoPath: String?,
        lastServiceDate: String
    ): Boolean {

        val detail = getStation(stationId) ?: return false

        return saveStation(
            detail = detail.copy(
                station = detail.station.copy(
                    lastServiceDate = lastServiceDate,
                    salesAfterService = 0,
                    machinePhotoPath = machinePhotoPath,
                    overviewPhotoPath = overviewPhotoPath
                )
            ),
            syncImmediately = true
        )

    }

    suspend fun updateSalesAfterService(
        stationId: Int,
        salesAfterService: Int
    ) {

        val detail = getStation(stationId) ?: return
        val currentSales = detail.station.salesAfterService

        if (currentSales == salesAfterService) return

        stationDao.insert(
            detail.copy(
                station = detail.station.copy(
                    salesAfterService = salesAfterService
                )
            )
                .toStationEntity()
        )

    }

    suspend fun updateRentPaidUntil(
        stationId: Int,
        rentPaidUntil: String
    ): Boolean {

        val detail = getStation(stationId) ?: return false

        return saveStation(
            detail = detail.copy(
                station = detail.station.copy(
                    rentPaidUntil = rentPaidUntil
                )
            ),
            syncImmediately = true
        )

    }

    suspend fun updateTechServiceDate(
        stationId: Int,
        node: TechServiceNode,
        serviceDate: String
    ): Boolean {

        val detail = getStation(stationId) ?: return false

        val station = when (node) {
            TechServiceNode.LIQUID_WASTE_CONTAINER ->
                detail.station.copy(
                    liquidWasteContainerServiceDate = serviceDate
                )

            TechServiceNode.BREW_UNIT ->
                detail.station.copy(
                    brewUnitServiceDate = serviceDate
                )

            TechServiceNode.COFFEE_GRINDER ->
                detail.station.copy(
                    coffeeGrinderServiceDate = serviceDate
                )

            TechServiceNode.MIXERS ->
                detail.station.copy(
                    mixersServiceDate = serviceDate
                )

            TechServiceNode.EXPANSION_TANK ->
                detail.station.copy(
                    expansionTankServiceDate = serviceDate
                )

            TechServiceNode.EXHAUST ->
                detail.station.copy(
                    exhaustServiceDate = serviceDate
                )
        }

        return saveStation(
            detail = detail.copy(
                station = station
            ),
            syncImmediately = true
        )

    }

    suspend fun updateSupplyOperatorServiceDates(
        stationId: Int,
        serviceDate: String,
        updateMixers: Boolean,
        updateLiquidWasteContainer: Boolean
    ): Boolean {

        val detail = getStation(stationId) ?: return false

        if (!updateMixers && !updateLiquidWasteContainer) {
            return true
        }

        return saveStation(
            detail = detail.copy(
                station = detail.station.copy(
                    mixersServiceDate = if (updateMixers) {
                        serviceDate
                    } else {
                        detail.station.mixersServiceDate
                    },
                    liquidWasteContainerServiceDate = if (updateLiquidWasteContainer) {
                        serviceDate
                    } else {
                        detail.station.liquidWasteContainerServiceDate
                    }
                )
            ),
            syncImmediately = true
        )

    }

}

data class StationSaveResult(
    val stationId: Int,
    val synced: Boolean
)

private fun CoffeeStation.hasContentChanges(
    other: CoffeeStation
): Boolean {
    return copy(
        salesAfterService = other.salesAfterService,
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = ""
    ) != other.copy(
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = ""
    )
}

private fun StationInventory.hasContentChanges(
    other: StationInventory
): Boolean {
    return copy(
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = ""
    ) != other.copy(
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = ""
    )
}

private fun StationSettings.hasContentChanges(
    other: StationSettings
): Boolean {
    return copy(
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = ""
    ) != other.copy(
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = ""
    )
}
