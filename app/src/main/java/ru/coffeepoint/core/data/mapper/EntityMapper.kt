package ru.coffeepoint.core.data.mapper

import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity
import ru.coffeepoint.core.data.local.entity.StationInventoryEntity
import ru.coffeepoint.core.data.local.entity.StationSettingsEntity
import ru.coffeepoint.core.util.normalizeTime
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings

fun StationDetail.toStationEntity() = CoffeeStationEntity(
    id = station.id,
    name = station.name,
    terminalId = station.terminalId,
    address = station.address,
    visitDays = station.visitDays,
    visitTimeFrom = normalizeTime(station.visitTimeFrom).ifBlank { "09:00" },
    visitTimeTo = normalizeTime(station.visitTimeTo).ifBlank { "18:00" },
    lastServiceDate = station.lastServiceDate,
    salesAfterService = station.salesAfterService,
    rentPaidUntil = station.rentPaidUntil,
    excludeFromSupplyPlanning = station.excludeFromSupplyPlanning,
    excludeFromOperatorEarnings = station.excludeFromOperatorEarnings,
    liquidWasteContainerServiceDate = station.liquidWasteContainerServiceDate,
    brewUnitServiceDate = station.brewUnitServiceDate,
    coffeeGrinderServiceDate = station.coffeeGrinderServiceDate,
    mixersServiceDate = station.mixersServiceDate,
    expansionTankServiceDate = station.expansionTankServiceDate,
    exhaustServiceDate = station.exhaustServiceDate,
    machinePhotoPath = station.machinePhotoPath,
    overviewPhotoPath = station.overviewPhotoPath,
    updatedAt = station.updatedAt,
    isSynced = station.isSynced,
    syncError = station.syncError,
    syncErrorType = station.syncErrorType
)

fun StationDetail.toInventoryEntity() = StationInventoryEntity(
    stationId = station.id,
    cups = inventory.cups,
    lids = inventory.lids,
    coffee = inventory.coffee,
    milk = inventory.milk,
    chocolate = inventory.chocolate,
    topping = inventory.topping,
    sugar = inventory.sugar,
    stirrers = inventory.stirrers,
    garbageBags = inventory.garbageBags,
    updatedAt = inventory.updatedAt,
    isSynced = inventory.isSynced,
    syncError = inventory.syncError,
    syncErrorType = inventory.syncErrorType
)

fun StationDetail.toSettingsEntity() = StationSettingsEntity(
    stationId = station.id,
    cupsMax = settings.cupsMax,
    lidsMax = settings.lidsMax,
    coffeeMax = settings.coffeeMax,
    milkMax = settings.milkMax,
    chocolateMax = settings.chocolateMax,
    toppingMax = settings.toppingMax,
    sugarMax = settings.sugarMax,
    stirrersMax = settings.stirrersMax,
    garbageBagsMax = settings.garbageBagsMax,
    updatedAt = settings.updatedAt,
    isSynced = settings.isSynced,
    syncError = settings.syncError,
    syncErrorType = settings.syncErrorType
)

fun toStationDetail(
    station: CoffeeStationEntity,
    inventory: StationInventoryEntity,
    settings: StationSettingsEntity
): StationDetail {

    return StationDetail(

        station = CoffeeStation(
            id = station.id,
            name = station.name,
            terminalId = station.terminalId,
            address = station.address,
            visitDays = station.visitDays,
            visitTimeFrom = normalizeTime(station.visitTimeFrom).ifBlank { "09:00" },
            visitTimeTo = normalizeTime(station.visitTimeTo).ifBlank { "18:00" },
            lastServiceDate = station.lastServiceDate,
            salesAfterService = station.salesAfterService,
            rentPaidUntil = station.rentPaidUntil,
            excludeFromSupplyPlanning = station.excludeFromSupplyPlanning,
            excludeFromOperatorEarnings = station.excludeFromOperatorEarnings,
            liquidWasteContainerServiceDate =
                station.liquidWasteContainerServiceDate,
            brewUnitServiceDate = station.brewUnitServiceDate,
            coffeeGrinderServiceDate = station.coffeeGrinderServiceDate,
            mixersServiceDate = station.mixersServiceDate,
            expansionTankServiceDate = station.expansionTankServiceDate,
            exhaustServiceDate = station.exhaustServiceDate,
            machinePhotoPath = station.machinePhotoPath,
            overviewPhotoPath = station.overviewPhotoPath,
            updatedAt = station.updatedAt,
            isSynced = station.isSynced,
            syncError = station.syncError,
            syncErrorType = station.syncErrorType
        ),

        inventory = StationInventory(
            cups = inventory.cups,
            lids = inventory.lids,
            coffee = inventory.coffee,
            milk = inventory.milk,
            chocolate = inventory.chocolate,
            topping = inventory.topping,
            sugar = inventory.sugar,
            stirrers = inventory.stirrers,
            garbageBags = inventory.garbageBags,
            updatedAt = inventory.updatedAt,
            isSynced = inventory.isSynced,
            syncError = inventory.syncError,
            syncErrorType = inventory.syncErrorType
        ),

        settings = StationSettings(
            cupsMax = settings.cupsMax,
            lidsMax = settings.lidsMax,
            coffeeMax = settings.coffeeMax,
            milkMax = settings.milkMax,
            chocolateMax = settings.chocolateMax,
            toppingMax = settings.toppingMax,
            sugarMax = settings.sugarMax,
            stirrersMax = settings.stirrersMax,
            garbageBagsMax = settings.garbageBagsMax,
            updatedAt = settings.updatedAt,
            isSynced = settings.isSynced,
            syncError = settings.syncError,
            syncErrorType = settings.syncErrorType
        )

    )

}
