package ru.coffeepoint.core.sync.google

import android.util.Log
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeStationAssignmentEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.util.normalizeTime
import ru.coffeepoint.core.sync.google.dto.GoogleSheetsSyncRequestDto
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi
import ru.coffeepoint.core.sync.google.network.GoogleSheetsApiProvider
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import java.math.BigDecimal
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.CancellationException

class GoogleSheetsRemoteDataSource(
    private val api: GoogleSheetsSyncApi = GoogleSheetsApiProvider.syncApi,
    private val webAppUrl: String = GoogleSheetsSyncConfig.WEB_APP_URL,
    private val syncToken: String = GoogleSheetsSyncConfig.SYNC_TOKEN,
    private val reportFailure: (String, Exception) -> Unit = { tag, error ->
        Log.w("GoogleSheetsSync", "Google Sheets $tag sync failed", error)
    }
) {

    suspend fun syncOperatorServiceFields(
        station: CoffeeStation
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "operator service fields",
            block = {
                request(
                    action = "complete_service",
                    entity = "coffee_station",
                    data = mapOf(
                        "id" to station.id,
                        "lastServiceDate" to station.lastServiceDate,
                        "salesAfterService" to station.salesAfterService,
                        "salesUpdatedAt" to station.lastServiceDate
                    )
                )
            }
        )
    }

    suspend fun syncSupplyServiceFields(
        station: CoffeeStation,
        updateMixers: Boolean,
        updateLiquidWasteContainer: Boolean
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }
        if (!updateMixers && !updateLiquidWasteContainer) {
            return GoogleSheetsEntitySyncResult.skipped()
        }

        val data = buildMap<String, Any> {
            put("id", station.id)
            if (updateMixers) put("mixersServiceDate", station.mixersServiceDate)
            if (updateLiquidWasteContainer) {
                put("liquidWasteContainerServiceDate", station.liquidWasteContainerServiceDate)
            }
        }

        return syncEntity(
            tag = "supply service fields",
            block = {
                request(
                    action = "complete_service",
                    entity = "coffee_station",
                    data = data
                )
            }
        )
    }

    suspend fun syncStationDetail(
        detail: StationDetail,
        includeStation: Boolean,
        includeInventory: Boolean,
        includeSettings: Boolean
    ): GoogleSheetsSyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsSyncResult()
        }

        var stationResult = GoogleSheetsEntitySyncResult.skipped()
        var inventoryResult = GoogleSheetsEntitySyncResult.skipped()
        var settingsResult = GoogleSheetsEntitySyncResult.skipped()

        if (includeStation) {
            stationResult = syncEntity(
                tag = "station",
                block = {
                upsert(
                    entity = "coffee_station",
                    data = detail.toStationData()
                )
                }
            )
        }

        if (includeInventory) {
            inventoryResult = syncEntity(
                tag = "inventory",
                block = {
                upsert(
                    entity = "station_inventory",
                    data = detail.toInventoryData()
                )
                }
            )
        }

        if (includeSettings) {
            settingsResult = syncEntity(
                tag = "settings",
                block = {
                upsert(
                    entity = "station_settings",
                    data = detail.toSettingsData()
                )
                }
            )
        }

        return GoogleSheetsSyncResult(
            station = stationResult,
            inventory = inventoryResult,
            settings = settingsResult
        )
    }

    suspend fun syncStationDetail(detail: StationDetail): GoogleSheetsSyncResult {
        return syncStationDetail(
            detail = detail,
            includeStation = true,
            includeInventory = true,
            includeSettings = true
        )
    }

    suspend fun loadServiceStart(stationId: Int, includeWarehouse: Boolean): ServiceStartSnapshot {
        check(GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) { "Google Sheets unavailable" }
        val response = api.fullSync(
            url = webAppUrl,
            action = "full_sync",
            token = syncToken,
            entities = if (includeWarehouse) "coffee_station,station_inventory,warehouse_stock"
                else "coffee_station,station_inventory",
            stationId = stationId
        )
        check(response.success) { response.error?.message ?: "Service start refresh failed" }
        val data = checkNotNull(response.data) { "Missing service start data" }
        val station = data.stations.single { it.intValue("id") == stationId }.toStation()
        val inventoryRow = data.inventories.single { it.intValue("stationId") == stationId }
        check(listOf("cups", "lids", "coffee", "milk", "chocolate", "topping", "sugar", "stirrers", "garbageBags")
            .all { inventoryRow[it] != null }) { "Incomplete station inventory" }
        val inventory = inventoryRow.toInventory()
        return ServiceStartSnapshot(station, inventory,
            if (includeWarehouse) data.warehouseStock.map { it.toWarehouseStockEntity() } else emptyList())
    }

    suspend fun loadFullSync(
        includeSettings: Boolean = true
    ): List<StationDetail> {
        return loadFullSyncBundle(
            includeSettings = includeSettings,
            includeSupplyCatalog = false
        ).stations
    }

    suspend fun loadFullSyncBundle(
        includeStations: Boolean = true,
        includeSettings: Boolean = true,
        includeSupplyCatalog: Boolean = true,
        includeWarehouseStock: Boolean = true,
        includeEmployeeData: Boolean = true
    ): GoogleSheetsFullSyncBundle {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsFullSyncBundle()
        }

        val response = api.fullSync(
            url = webAppUrl,
            action = "full_sync",
            token = syncToken,
            entities = if (
                includeStations &&
                includeSettings &&
                includeSupplyCatalog &&
                includeWarehouseStock &&
                includeEmployeeData
            ) {
                null
            } else {
                buildList {
                    if (includeStations) {
                        add("coffee_station")
                        add("station_inventory")
                        if (includeSettings) add("station_settings")
                    }
                    if (includeSupplyCatalog) add("supply_catalog")
                    if (includeWarehouseStock) add("warehouse_stock")
                    if (includeEmployeeData) {
                        add("employees")
                        add("employee_station_assignments")
                        add("employees_transactions")
                    }
                }.joinToString(",")
            }
        )

        if (!response.success) {
            error(response.error?.message ?: "Google Sheets full sync error")
        }

        val data = response.data ?: return GoogleSheetsFullSyncBundle()
        val inventoriesByStationId = data.inventories.associateBy {
            it.intValue("stationId")
        }
        val settingsByStationId = data.settings.associateBy {
            it.intValue("stationId")
        }

        val stations = if (includeStations) {
            data.stations.mapNotNull { stationRow ->
                val stationId = stationRow.intValue("id")
                val inventoryRow = inventoriesByStationId[stationId] ?: return@mapNotNull null

                StationDetail(
                    station = stationRow.toStation(),
                    inventory = inventoryRow.toInventory(),
                    settings = if (includeSettings) {
                        settingsByStationId[stationId]?.toSettings()
                            ?: return@mapNotNull null
                    } else {
                        StationSettings(
                            cupsMax = 15,
                            lidsMax = 6,
                            coffeeMax = 6,
                            milkMax = 10,
                            chocolateMax = 10,
                            toppingMax = 10
                        )
                    }
                )
            }
        } else {
            emptyList()
        }

        return GoogleSheetsFullSyncBundle(
            stations = stations,
            supplyCatalog = data.supplyCatalog.map { row ->
                row.toSupplyCatalogEntity()
            },
            warehouseStock = data.warehouseStock.map { row ->
                row.toWarehouseStockEntity()
            },
            employees = data.employees.map { row ->
                row.toEmployeeEntity()
            },
            employeeStationAssignments = data.employeeStationAssignments.map { row ->
                row.toEmployeeStationAssignmentEntity()
            },
            employeeTransactions = data.employeeTransactions.map { row ->
                row.toEmployeeTransactionEntity()
            }
        )
    }

    suspend fun loadSupplyCatalog(): List<SupplyCatalogEntity> {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return emptyList()
        }

        val response = api.fullSync(
            url = webAppUrl,
            action = "full_sync",
            token = syncToken,
            entities = "supply_catalog"
        )

        if (!response.success) {
            error(response.error?.message ?: "Google Sheets supply catalog sync error")
        }

        return response.data?.supplyCatalog.orEmpty().map { row ->
            row.toSupplyCatalogEntity()
        }
    }

    suspend fun syncSupplyCatalogOption(
        option: SupplyCatalogEntity
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "supply catalog",
            block = {
            upsert(
                entity = "supply_catalog",
                data = option.toSupplyCatalogData(currentTimestamp())
            )
            }
        )
    }

    suspend fun upsertSupplyCatalogOption(
        option: SupplyCatalogEntity,
        updatedAt: String
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "supply catalog upsert",
            block = {
                upsert(
                    entity = "supply_catalog",
                    data = option.toSupplyCatalogData(updatedAt)
                )
            }
        )
    }

    suspend fun deleteSupplyCatalogOption(
        optionId: Int
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "supply catalog delete",
            block = {
                request(
                    action = "delete",
                    entity = "supply_catalog",
                    data = mapOf(
                        "id" to optionId
                    )
                )
            }
        )
    }

    suspend fun syncWarehouseStock(
        items: List<WarehouseStockEntity>
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "warehouse stock",
            block = {
                if (items.isNotEmpty()) {
                    request(
                        action = "upsert_batch",
                        entity = "warehouse_stock",
                        data = mapOf(
                            "items" to items.map { item ->
                                item.toWarehouseStockData()
                            }
                        )
                    )
                }
            }
        )
    }

    suspend fun syncEmployeesAndAssignments(
        employees: List<EmployeeEntity>,
        assignments: List<EmployeeStationAssignmentEntity>
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "employees and assignments",
            block = {
                employees.forEach { employee ->
                    upsert(
                        entity = "employees",
                        data = employee.toEmployeeData()
                    )
                }
                assignments.forEach { assignment ->
                    upsert(
                        entity = "employee_station_assignments",
                        data = assignment.toEmployeeStationAssignmentData()
                    )
                }
            }
        )
    }

    suspend fun syncEmployeeTransactions(
        transactions: List<EmployeeTransactionEntity>
    ): GoogleSheetsEntitySyncResult {
        if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
            return GoogleSheetsEntitySyncResult.error(
                GoogleSheetsSyncErrorType.CONNECTION
            )
        }

        return syncEntity(
            tag = "employee transactions",
            block = {
                transactions.forEach { transaction ->
                    upsert(
                        entity = "employees_transactions",
                        data = transaction.toEmployeeTransactionData()
                    )
                }
            }
        )
    }

    private suspend fun syncEntity(
        tag: String,
        block: suspend () -> Unit
    ): GoogleSheetsEntitySyncResult {
        return try {
            block()
            GoogleSheetsEntitySyncResult.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            reportFailure(tag, error)
            GoogleSheetsEntitySyncResult.error(error.toSyncErrorType())
        }
    }

    private suspend fun upsert(
        entity: String,
        data: Map<String, Any?>
    ) {
        request(
            action = "upsert",
            entity = entity,
            data = data
        )
    }

    private suspend fun request(
        action: String,
        entity: String,
        data: Map<String, Any?>
    ) {
        val response = api.upsert(
            url = webAppUrl,
            request = GoogleSheetsSyncRequestDto(
                action = action,
                token = syncToken,
                entity = entity,
                data = data
            )
        )

        if (!response.success) {
            throw GoogleSheetsSyncException(
                status = response.error?.status,
                syncMessage = response.error?.message ?: "Google Sheets sync error"
            )
        }
    }

    private fun StationDetail.toStationData(): Map<String, Any?> {
        return mapOf(
            "id" to station.id,
            "name" to station.name,
            "terminalId" to station.terminalId,
            "address" to station.address,
            "visitDays" to station.visitDays,
            "visitTimeFrom" to normalizeTime(station.visitTimeFrom).ifBlank { "09:00" },
            "visitTimeTo" to normalizeTime(station.visitTimeTo).ifBlank { "18:00" },
            "lastServiceDate" to station.lastServiceDate,
            "rentPaidUntil" to station.rentPaidUntil,
            "excludeFromSupplyPlanning" to station.excludeFromSupplyPlanning,
            "excludeFromOperatorEarnings" to station.excludeFromOperatorEarnings,
            "liquidWasteContainerServiceDate" to station.liquidWasteContainerServiceDate,
            "brewUnitServiceDate" to station.brewUnitServiceDate,
            "coffeeGrinderServiceDate" to station.coffeeGrinderServiceDate,
            "mixersServiceDate" to station.mixersServiceDate,
            "expansionTankServiceDate" to station.expansionTankServiceDate,
            "exhaustServiceDate" to station.exhaustServiceDate,
            "machinePhotoPath" to station.machinePhotoPath,
            "overviewPhotoPath" to station.overviewPhotoPath,
            "updatedAt" to station.updatedAt.requireSyncTimestamp("coffee_station")
        )
    }

    private fun StationDetail.toInventoryData(): Map<String, Any?> {
        return mapOf(
            "stationId" to station.id,
            "cups" to inventory.cups,
            "lids" to inventory.lids,
            "coffee" to inventory.coffee,
            "milk" to inventory.milk,
            "chocolate" to inventory.chocolate,
            "topping" to inventory.topping,
            "sugar" to inventory.sugar,
            "stirrers" to inventory.stirrers,
            "garbageBags" to inventory.garbageBags,
            "updatedAt" to inventory.updatedAt.requireSyncTimestamp("station_inventory")
        )
    }

    private fun StationDetail.toSettingsData(): Map<String, Any?> {
        return mapOf(
            "stationId" to station.id,
            "cupsMin" to settings.cupsMin,
            "cupsMax" to settings.cupsMax,
            "lidsMin" to settings.lidsMin,
            "lidsMax" to settings.lidsMax,
            "coffeeMin" to settings.coffeeMin,
            "coffeeMax" to settings.coffeeMax,
            "milkMin" to settings.milkMin,
            "milkMax" to settings.milkMax,
            "chocolateMin" to settings.chocolateMin,
            "chocolateMax" to settings.chocolateMax,
            "toppingMin" to settings.toppingMin,
            "toppingMax" to settings.toppingMax,
            "sugarMin" to settings.sugarMin,
            "sugarMax" to settings.sugarMax,
            "stirrersMin" to settings.stirrersMin,
            "stirrersMax" to settings.stirrersMax,
            "garbageBagsMin" to settings.garbageBagsMin,
            "garbageBagsMax" to settings.garbageBagsMax,
            "updatedAt" to settings.updatedAt.requireSyncTimestamp("station_settings")
        )
    }

    private fun String.requireSyncTimestamp(entity: String): String {
        check(isNotBlank()) {
            "Missing local updatedAt for $entity"
        }

        return this
    }

    private fun SupplyCatalogEntity.toSupplyCatalogData(
        updatedAt: String
    ): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "itemType" to itemType,
            "title" to title,
            "supplier" to supplier,
            "article" to article,
            "url" to url,
            "maxPrice" to maxPrice,
            "isActive" to isActive,
            "priority" to priority,
            "comment" to comment,
            "updatedAt" to updatedAt
        )
    }

    private fun WarehouseStockEntity.toWarehouseStockData(): Map<String, Any?> {
        return mapOf(
            "itemType" to itemType,
            "quantity" to quantity,
            "minQuantity" to minQuantity,
            "maxQuantity" to maxQuantity,
            "updatedAt" to updatedAt.requireSyncTimestamp("warehouse_stock")
        )
    }

    private fun EmployeeEntity.toEmployeeData(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "role" to role,
            "workStartDate" to workStartDate,
            "isActive" to isActive,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt.requireSyncTimestamp("employees")
        )
    }

    private fun EmployeeStationAssignmentEntity.toEmployeeStationAssignmentData(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "employeeId" to employeeId,
            "stationId" to stationId,
            "validFrom" to validFrom,
            "validTo" to validTo,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt.requireSyncTimestamp("employee_station_assignments")
        )
    }

    private fun EmployeeTransactionEntity.toEmployeeTransactionData(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "employeeId" to employeeId,
            "type" to type,
            "operationDate" to operationDate,
            "periodFrom" to periodFrom,
            "periodTo" to periodTo,
            "salesCount" to salesCount,
            "amount" to amount,
            "comment" to comment,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt.requireSyncTimestamp("employees_transactions")
        )
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
    }

    private fun Map<String, Any?>.toStation(): CoffeeStation {
        return CoffeeStation(
            id = intValue("id"),
            name = stringValue("name"),
            terminalId = longOrNull("terminalId"),
            address = stringValue("address"),
            visitDays = stringValue("visitDays").ifBlank { "1,2,3,4,5" },
            visitTimeFrom = timeValue("visitTimeFrom").ifBlank { "09:00" },
            visitTimeTo = timeValue("visitTimeTo").ifBlank { "18:00" },
            lastServiceDate = dateTimeValue("lastServiceDate"),
            salesAfterService = intValue("salesAfterService"),
            rentPaidUntil = dateValue("rentPaidUntil"),
            excludeFromSupplyPlanning = booleanValue("excludeFromSupplyPlanning"),
            excludeFromOperatorEarnings =
                booleanValue("excludeFromOperatorEarnings"),
            liquidWasteContainerServiceDate =
                dateValue("liquidWasteContainerServiceDate"),
            brewUnitServiceDate = dateValue("brewUnitServiceDate"),
            coffeeGrinderServiceDate = dateValue("coffeeGrinderServiceDate"),
            mixersServiceDate = dateValue("mixersServiceDate"),
            expansionTankServiceDate = dateValue("expansionTankServiceDate"),
            exhaustServiceDate = dateValue("exhaustServiceDate"),
            machinePhotoPath = stringValue("machinePhotoPath").ifBlank { null },
            overviewPhotoPath = stringValue("overviewPhotoPath").ifBlank { null },
            updatedAt = timestampValue("updatedAt"),
            isSynced = true
        )
    }

    private fun Map<String, Any?>.toInventory(): StationInventory {
        return StationInventory(
            cups = intValue("cups"),
            lids = intValue("lids"),
            coffee = intValue("coffee"),
            milk = intValue("milk"),
            chocolate = intValue("chocolate"),
            topping = intValue("topping"),
            sugar = stockAmountValue(
                key = "sugar",
                empty = 0,
                low = 1,
                medium = 1,
                full = 2
            ),
            stirrers = stockAmountValue(
                key = "stirrers",
                empty = 0,
                low = 1,
                medium = 2,
                full = 4
            ),
            garbageBags = intValue("garbageBags", defaultValue = 2),
            updatedAt = timestampValue("updatedAt"),
            isSynced = true
        )
    }

    private fun Map<String, Any?>.toSettings(): StationSettings {
        val cupsMax = intValue("cupsMax")
        val lidsMax = intValue("lidsMax")
        val coffeeMax = intValue("coffeeMax")
        val milkMax = intValue("milkMax")
        val chocolateMax = intValue("chocolateMax")
        val toppingMax = intValue("toppingMax")
        val sugarMax = intValue("sugarMax", defaultValue = 2)
        val stirrersMax = intValue("stirrersMax", defaultValue = 4)
        val garbageBagsMax = intValue("garbageBagsMax", defaultValue = 2)
        val cupsMin = intValue("cupsMin", defaultValue = minOf(3, cupsMax)).coerceAtMost(cupsMax)
        val lidsMin = intValue("lidsMin", defaultValue = minOf(3, lidsMax)).coerceAtMost(lidsMax)
        val coffeeMin = intValue("coffeeMin", defaultValue = minOf(3, coffeeMax)).coerceAtMost(coffeeMax)
        val milkMin = intValue("milkMin", defaultValue = minOf(3, milkMax)).coerceAtMost(milkMax)
        val chocolateMin = intValue("chocolateMin", defaultValue = minOf(3, chocolateMax)).coerceAtMost(chocolateMax)
        val toppingMin = intValue("toppingMin", defaultValue = minOf(3, toppingMax)).coerceAtMost(toppingMax)
        val sugarMin = intValue("sugarMin", defaultValue = minOf(1, sugarMax)).coerceAtMost(sugarMax)
        val stirrersMin = intValue("stirrersMin", defaultValue = minOf(1, stirrersMax)).coerceAtMost(stirrersMax)
        val garbageBagsMin = intValue("garbageBagsMin", defaultValue = minOf(1, garbageBagsMax))
            .coerceAtMost(garbageBagsMax)

        return StationSettings(
            cupsMin = cupsMin,
            cupsMax = cupsMax,
            lidsMin = lidsMin,
            lidsMax = lidsMax,
            coffeeMin = coffeeMin,
            coffeeMax = coffeeMax,
            milkMin = milkMin,
            milkMax = milkMax,
            chocolateMin = chocolateMin,
            chocolateMax = chocolateMax,
            toppingMin = toppingMin,
            toppingMax = toppingMax,
            sugarMin = sugarMin,
            sugarMax = sugarMax,
            stirrersMin = stirrersMin,
            stirrersMax = stirrersMax,
            garbageBagsMin = garbageBagsMin,
            garbageBagsMax = garbageBagsMax,
            updatedAt = timestampValue("updatedAt"),
            isSynced = true
        )
    }

    private fun Map<String, Any?>.toSupplyCatalogEntity(): SupplyCatalogEntity {
        return SupplyCatalogEntity(
            id = intValue("id"),
            itemType = stringValue("itemType"),
            title = stringValue("title"),
            supplier = stringValue("supplier"),
            article = plainStringValue("article"),
            url = stringValue("url"),
            maxPrice = intValue("maxPrice"),
            isActive = booleanValue("isActive"),
            priority = intValue("priority", defaultValue = 1),
            comment = stringValue("comment"),
            updatedAt = dateTimeValue("updatedAt"),
            isSynced = true,
            syncError = false
        )
    }

    private fun Map<String, Any?>.toWarehouseStockEntity(): WarehouseStockEntity {
        return WarehouseStockEntity(
            itemType = stringValue("itemType"),
            quantity = intValue("quantity"),
            minQuantity = intValue("minQuantity"),
            maxQuantity = intValue("maxQuantity"),
            updatedAt = timestampValue("updatedAt")
        )
    }

    private fun Map<String, Any?>.toEmployeeEntity(): EmployeeEntity {
        return EmployeeEntity(
            id = intValue("id"),
            name = stringValue("name"),
            role = stringValue("role"),
            workStartDate = storageDateValue("workStartDate"),
            isActive = booleanValue("isActive"),
            createdAt = timestampValue("createdAt"),
            updatedAt = timestampValue("updatedAt"),
            isSynced = true,
            syncError = false
        )
    }

    private fun Map<String, Any?>.toEmployeeStationAssignmentEntity(): EmployeeStationAssignmentEntity {
        return EmployeeStationAssignmentEntity(
            id = plainStringValue("id"),
            employeeId = intValue("employeeId"),
            stationId = intValue("stationId"),
            validFrom = storageDateValue("validFrom"),
            validTo = nullableStorageDateValue("validTo"),
            createdAt = timestampValue("createdAt"),
            updatedAt = timestampValue("updatedAt"),
            isSynced = true,
            syncError = false
        )
    }

    private fun Map<String, Any?>.toEmployeeTransactionEntity(): EmployeeTransactionEntity {
        return EmployeeTransactionEntity(
            id = plainStringValue("id"),
            employeeId = intValue("employeeId"),
            type = stringValue("type"),
            operationDate = timestampValue("operationDate"),
            periodFrom = nullableStorageDateValue("periodFrom"),
            periodTo = nullableStorageDateValue("periodTo"),
            salesCount = nullableIntValue("salesCount"),
            amount = intValue("amount"),
            comment = stringValue("comment"),
            createdAt = timestampValue("createdAt"),
            updatedAt = timestampValue("updatedAt"),
            isSynced = true,
            syncError = false
        )
    }

    private fun Map<String, Any?>.stringValue(key: String): String {
        return this[key]?.toString().orEmpty()
    }

    private fun Map<String, Any?>.plainStringValue(key: String): String {
        return when (val value = this[key]) {
            is Number -> BigDecimal.valueOf(value.toDouble())
                .stripTrailingZeros()
                .toPlainString()

            else -> value?.toString().orEmpty()
        }
    }

    private fun Map<String, Any?>.intValue(
        key: String,
        defaultValue: Int = 0
    ): Int {
        return when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.toDoubleOrNull()?.toInt() ?: defaultValue
            else -> defaultValue
        }
    }

    private fun Map<String, Any?>.nullableIntValue(key: String): Int? {
        return when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> value.toDoubleOrNull()?.toInt()
            else -> null
        }
    }

    private fun Map<String, Any?>.longOrNull(key: String): Long? {
        return when (val value = this[key]) {
            is Number -> value.toLong()
            is String -> value.toDoubleOrNull()?.toLong()
            else -> null
        }
    }

    private fun Map<String, Any?>.booleanValue(key: String): Boolean {
        return when (val value = this[key]) {
            is Boolean -> value
            is Number -> value.toInt() != 0
            is String -> value.equals("true", ignoreCase = true) || value == "1"
            else -> false
        }
    }

    private fun Map<String, Any?>.stockAmountValue(
        key: String,
        empty: Int,
        low: Int,
        medium: Int,
        full: Int
    ): Int {

        return when (val value = this[key]) {
            is Number -> value.toInt()
            is String -> {
                value.toDoubleOrNull()?.toInt() ?: when (value.uppercase(Locale.getDefault())) {
                    "EMPTY" -> empty
                    "LOW" -> low
                    "MEDIUM" -> medium
                    "FULL" -> full
                    else -> full
                }
            }

            else -> full
        }
    }

    private fun Map<String, Any?>.dateValue(key: String): String {
        return formatGoogleDate(
            value = stringValue(key),
            outputPattern = "dd.MM.yyyy"
        )
    }

    private fun Map<String, Any?>.dateTimeValue(key: String): String {
        return formatGoogleDate(
            value = stringValue(key),
            outputPattern = "dd.MM.yyyy HH:mm"
        )
    }

    private fun Map<String, Any?>.storageDateValue(key: String): String {
        val value = stringValue(key)
        if (value.isBlank()) return ""

        return formatGoogleDate(
            value = value,
            outputPattern = "yyyy-MM-dd"
        ).ifBlank { value }
    }

    private fun Map<String, Any?>.nullableStorageDateValue(key: String): String? {
        return storageDateValue(key).ifBlank { null }
    }

    private fun Map<String, Any?>.timeValue(key: String): String {
        return normalizeTime(stringValue(key))
    }

    private fun Throwable.toSyncErrorType(): GoogleSheetsSyncErrorType {
        val message = message.orEmpty()

        return when {
            this is GoogleSheetsSyncException && status == 409 ->
                GoogleSheetsSyncErrorType.CONFLICT

            message.contains("Conflict:", ignoreCase = true) ->
                GoogleSheetsSyncErrorType.CONFLICT

            this is UnknownHostException ||
                this is SocketTimeoutException ||
                message.contains("Unable to resolve host", ignoreCase = true) ->
                GoogleSheetsSyncErrorType.CONNECTION

            else -> GoogleSheetsSyncErrorType.UNKNOWN
        }
    }

    private fun Map<String, Any?>.timestampValue(key: String): String {
        val value = stringValue(key)
        if (value.isBlank()) return ""

        val formatted = formatGoogleDate(
            value = value,
            outputPattern = "yyyy-MM-dd'T'HH:mm:ss"
        )

        return formatted.ifBlank { value }
    }

    private fun formatGoogleDate(
        value: String,
        outputPattern: String
    ): String {

        if (value.isBlank()) return ""

        val inputFormats = listOf(
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                Locale.getDefault()
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            },
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            ).apply {
                isLenient = false
            },
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).apply {
                isLenient = false
            },
            SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            ).apply {
                isLenient = false
            },
            SimpleDateFormat(
                "dd.MM.yyyy",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }
        )

        val date = inputFormats.firstNotNullOfOrNull { formatter ->
            runCatching {
                formatter.parse(value)
            }.getOrNull()
        } ?: return value

        return SimpleDateFormat(
            outputPattern,
            Locale.getDefault()
        ).apply {
            timeZone = GOOGLE_SHEETS_TIME_ZONE
        }.format(date)

    }

    private companion object {
        val GOOGLE_SHEETS_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Europe/Moscow")
    }

}

data class GoogleSheetsSyncResult(
    val station: GoogleSheetsEntitySyncResult = GoogleSheetsEntitySyncResult.skipped(),
    val inventory: GoogleSheetsEntitySyncResult = GoogleSheetsEntitySyncResult.skipped(),
    val settings: GoogleSheetsEntitySyncResult = GoogleSheetsEntitySyncResult.skipped()
)

data class GoogleSheetsEntitySyncResult(
    val synced: Boolean,
    val errorType: GoogleSheetsSyncErrorType
) {
    companion object {
        fun skipped(): GoogleSheetsEntitySyncResult {
            return GoogleSheetsEntitySyncResult(
                synced = false,
                errorType = GoogleSheetsSyncErrorType.NONE
            )
        }

        fun success(): GoogleSheetsEntitySyncResult {
            return GoogleSheetsEntitySyncResult(
                synced = true,
                errorType = GoogleSheetsSyncErrorType.NONE
            )
        }

        fun error(
            errorType: GoogleSheetsSyncErrorType
        ): GoogleSheetsEntitySyncResult {
            return GoogleSheetsEntitySyncResult(
                synced = false,
                errorType = errorType
            )
        }
    }
}

private class GoogleSheetsSyncException(
    val status: Int?,
    val syncMessage: String
) : IllegalStateException(syncMessage)

data class GoogleSheetsFullSyncBundle(
    val stations: List<StationDetail> = emptyList(),
    val supplyCatalog: List<SupplyCatalogEntity> = emptyList(),
    val warehouseStock: List<WarehouseStockEntity> = emptyList(),
    val employees: List<EmployeeEntity> = emptyList(),
    val employeeStationAssignments: List<EmployeeStationAssignmentEntity> = emptyList(),
    val employeeTransactions: List<EmployeeTransactionEntity> = emptyList()
)
