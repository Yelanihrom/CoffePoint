package ru.coffeepoint.feature.service.model

import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

enum class RefillSource(val title: String) {
    Warehouse("Склад"), ExternalPurchase("Сторонняя закупка")
}

data class SupplierRefillLine(
    val type: WarehouseItemType,
    val original: Int,
    val maximum: Int,
    val added: Int,
    val actual: Int,
    val actualEdited: Boolean = false
) {
    val expected: Int get() = (original.toLong() + added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    fun withAdded(value: Int): SupplierRefillLine {
        require(value >= 0)
        val changed = copy(added = value)
        return if (actualEdited) changed else changed.copy(actual = changed.expected)
    }
    fun withActual(value: Int): SupplierRefillLine {
        require(value >= 0)
        return copy(actual = value, actualEdited = true)
    }
}

fun createSupplierRefillLines(inventory: StationInventory, settings: StationSettings): List<SupplierRefillLine> =
    WarehouseItemType.entries.map { type ->
        val original = inventory.quantity(type)
        val maximum = settings.maximum(type)
        val added = (maximum.toLong() - original).coerceAtLeast(0).toInt()
        SupplierRefillLine(type, original, maximum, added, (original.toLong() + added).toInt())
    }

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

fun List<SupplierRefillLine>.actualInventory(original: StationInventory): StationInventory =
    fold(original) { result, line -> result.withQuantity(line.type, line.actual) }

fun List<SupplierRefillLine>.warehouseWriteOff(source: RefillSource): Map<String, Int> =
    if (source == RefillSource.Warehouse) filter { it.added > 0 }.associate { it.type.code to it.added }
    else emptyMap()

fun warehouseShortages(writeOff: Map<String, Int>, available: Map<String, Int>): Set<String> =
    writeOff.filter { (type, amount) -> amount > (available[type] ?: 0) }.keys
