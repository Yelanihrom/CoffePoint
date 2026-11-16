package ru.coffeepoint.feature.service.model

import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

fun StationInventory.quantity(type: WarehouseItemType): Int = when (type) {
    WarehouseItemType.Cups -> cups
    WarehouseItemType.Lids -> lids
    WarehouseItemType.Sugar -> sugar
    WarehouseItemType.Stirrers -> stirrers
    WarehouseItemType.GarbageBags -> garbageBags
    WarehouseItemType.Coffee -> coffee
    WarehouseItemType.Milk -> milk
    WarehouseItemType.Chocolate -> chocolate
    WarehouseItemType.Topping -> topping
}

fun StationSettings.maximum(type: WarehouseItemType): Int = when (type) {
    WarehouseItemType.Cups -> cupsMax
    WarehouseItemType.Lids -> lidsMax
    WarehouseItemType.Sugar -> sugarMax
    WarehouseItemType.Stirrers -> stirrersMax
    WarehouseItemType.GarbageBags -> garbageBagsMax
    WarehouseItemType.Coffee -> coffeeMax
    WarehouseItemType.Milk -> milkMax
    WarehouseItemType.Chocolate -> chocolateMax
    WarehouseItemType.Topping -> toppingMax
}

fun StationInventory.withQuantity(type: WarehouseItemType, value: Int): StationInventory = when (type) {
    WarehouseItemType.Cups -> copy(cups = value)
    WarehouseItemType.Lids -> copy(lids = value)
    WarehouseItemType.Sugar -> copy(sugar = value)
    WarehouseItemType.Stirrers -> copy(stirrers = value)
    WarehouseItemType.GarbageBags -> copy(garbageBags = value)
    WarehouseItemType.Coffee -> copy(coffee = value)
    WarehouseItemType.Milk -> copy(milk = value)
    WarehouseItemType.Chocolate -> copy(chocolate = value)
    WarehouseItemType.Topping -> copy(topping = value)
}
