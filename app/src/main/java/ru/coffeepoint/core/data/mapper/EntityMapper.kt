package ru.coffeepoint.core.data.mapper

import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity
import ru.coffeepoint.core.data.local.entity.StationInventoryEntity
import ru.coffeepoint.core.data.local.entity.StationSettingsEntity
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings

fun StationDetail.toStationEntity() = CoffeeStationEntity(
    id = station.id,
    name = station.name,
    terminalId = station.terminalId,
    address = station.address,
    lastServiceDate = station.lastServiceDate,
    salesAfterService = station.salesAfterService,
    salesLimit = station.salesLimit,
    machinePhotoPath = station.machinePhotoPath,
    overviewPhotoPath = station.overviewPhotoPath
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
    stirrers = inventory.stirrers
)

fun StationDetail.toSettingsEntity() = StationSettingsEntity(
    stationId = station.id,
    cupsMax = settings.cupsMax,
    lidsMax = settings.lidsMax,
    coffeeMax = settings.coffeeMax,
    milkMax = settings.milkMax,
    chocolateMax = settings.chocolateMax,
    toppingMax = settings.toppingMax
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
            lastServiceDate = station.lastServiceDate,
            salesAfterService = station.salesAfterService,
            salesLimit = station.salesLimit,
            machinePhotoPath = station.machinePhotoPath,
            overviewPhotoPath = station.overviewPhotoPath
        ),

        inventory = StationInventory(
            cups = inventory.cups,
            lids = inventory.lids,
            coffee = inventory.coffee,
            milk = inventory.milk,
            chocolate = inventory.chocolate,
            topping = inventory.topping,
            sugar = inventory.sugar,
            stirrers = inventory.stirrers
        ),

        settings = StationSettings(
            cupsMax = settings.cupsMax,
            lidsMax = settings.lidsMax,
            coffeeMax = settings.coffeeMax,
            milkMax = settings.milkMax,
            chocolateMax = settings.chocolateMax,
            toppingMax = settings.toppingMax
        )

    )

}