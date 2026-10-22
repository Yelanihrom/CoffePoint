package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.preferences.GoogleSheetsImportPreferences
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toSettingsEntity
import ru.coffeepoint.core.data.mapper.toStationDetail
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult
import ru.coffeepoint.core.sync.google.GoogleSheetsImportService
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncDefaults
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.core.sync.google.GoogleSheetsStationSyncService
import ru.coffeepoint.core.sync.google.GoogleSheetsServiceStartImporter
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
    private val googleSheetsImportPreferences = GoogleSheetsImportPreferences(appContext)

    private val stationDao = db.coffeeStationDao()
    private val inventoryDao = db.stationInventoryDao()
    private val settingsDao = db.stationSettingsDao()
    private val googleSheetsSyncManager = GoogleSheetsSyncManager()
    private val googleSheetsImportService = GoogleSheetsImportService(
        stationDao = stationDao,
        inventoryDao = inventoryDao,
        settingsDao = settingsDao,
        supplyCatalogDao = db.supplyCatalogDao(),
        warehouseStockDao = db.warehouseStockDao(),
        employeeDao = db.employeeDao(),
        employeeStationAssignmentDao = db.employeeStationAssignmentDao(),
        employeeTransactionDao = db.employeeTransactionDao()
    )
    private val googleSheetsStationSyncService = GoogleSheetsStationSyncService(
        stationDao = stationDao,
        inventoryDao = inventoryDao,
        settingsDao = settingsDao
    )

    suspend fun refreshBeforeService(stationId: Int, includeWarehouse: Boolean): GoogleSheetsImportResult =
        GoogleSheetsServiceStartImporter(db).refresh(stationId, includeWarehouse)

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
        val result = googleSheetsImportService.importFullSync(
            includeSupplyCatalog = false,
            includeEmployeeData = false
        )

        if (result == GoogleSheetsImportResult.Success) {
            googleSheetsImportPreferences.saveLastImportTime(
                System.currentTimeMillis()
            )
        }

        return result
    }

    suspend fun saveStation(
        detail: StationDetail,
        syncImmediately: Boolean = false,
        serviceFieldsOnly: Boolean = false,
        supplyServiceFieldsOnly: Boolean = false,
        updateMixersServiceDate: Boolean = false,
        updateLiquidWasteContainerServiceDate: Boolean = false
    ): Boolean {

        val current = getStation(detail.station.id)
        val updatedAt = currentTimestamp()
        var hasChanges = false

        if (current == null || current.station.hasContentChanges(detail.station)) {
            val pendingFullStationSync = when {
                serviceFieldsOnly || supplyServiceFieldsOnly -> current?.station?.let {
                    it.pendingFullStationSync ||
                        (!it.isSynced && !it.pendingServiceOnly && !it.pendingSupplyServiceOnly)
                } ?: false
                else -> true
            }
            stationDao.insert(
                detail.copy(
                    station = detail.station.copy(
                        updatedAt = if (serviceFieldsOnly || supplyServiceFieldsOnly) {
                            current?.station?.updatedAt.orEmpty()
                        } else {
                            updatedAt
                        },
                        isSynced = false,
                        syncError = false,
                        syncErrorType = GoogleSheetsSyncDefaults.noErrorType,
                        pendingServiceOnly = serviceFieldsOnly ||
                            current?.station?.pendingServiceOnly == true,
                        pendingFullStationSync = pendingFullStationSync,
                        pendingSupplyServiceOnly = supplyServiceFieldsOnly ||
                            current?.station?.pendingSupplyServiceOnly == true,
                        pendingMixersServiceDate =
                            (supplyServiceFieldsOnly && updateMixersServiceDate) ||
                                current?.station?.pendingMixersServiceDate == true,
                        pendingLiquidWasteContainerServiceDate =
                            (supplyServiceFieldsOnly && updateLiquidWasteContainerServiceDate) ||
                                current?.station?.pendingLiquidWasteContainerServiceDate == true
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

        if (serviceFieldsOnly && syncImmediately) {
            return googleSheetsStationSyncService.syncOperatorServiceFields(
                detail.station.id
            )
        } else if (supplyServiceFieldsOnly && syncImmediately) {
            return googleSheetsStationSyncService.syncSupplyServiceFields(
                detail.station.id
            )
        } else if (hasChanges && syncImmediately) {
            return googleSheetsStationSyncService.syncStation(detail.station.id)
        } else if (!hasChanges && syncImmediately) {
            return googleSheetsStationSyncService.syncStation(detail.station.id)
        } else if (hasChanges) {
            googleSheetsSyncManager.enqueueBackgroundSync(appContext)
        }

        return true

    }

    suspend fun syncPendingGoogleSheets(): GoogleSheetsEntitySyncResult {
        return googleSheetsStationSyncService.syncPendingStations()
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
                    salesAfterService = 0
                )
            ),
            syncImmediately = true,
            serviceFieldsOnly = true
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
            syncImmediately = true,
            supplyServiceFieldsOnly = true,
            updateMixersServiceDate = updateMixers,
            updateLiquidWasteContainerServiceDate = updateLiquidWasteContainer
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
        syncErrorType = "",
        pendingServiceOnly = false,
        pendingFullStationSync = false,
        pendingSupplyServiceOnly = false,
        pendingMixersServiceDate = false,
        pendingLiquidWasteContainerServiceDate = false
    ) != other.copy(
        updatedAt = "",
        isSynced = true,
        syncError = false,
        syncErrorType = "",
        pendingServiceOnly = false,
        pendingFullStationSync = false,
        pendingSupplyServiceOnly = false,
        pendingMixersServiceDate = false,
        pendingLiquidWasteContainerServiceDate = false
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
