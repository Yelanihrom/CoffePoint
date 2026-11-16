package ru.coffeepoint.feature.service.supply.model

import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType
import ru.coffeepoint.feature.service.model.maximum
import ru.coffeepoint.feature.service.model.quantity
import ru.coffeepoint.feature.service.model.withQuantity

enum class RefillSource(val title: String) {
    Warehouse("Склад"), ExternalPurchase("Сторонняя закупка")
}

data class SupplierRefillLine(
    val type: WarehouseItemType,
    val original: Int,
    val maximum: Int,
    val added: Int,
    val verified: Int = original,
    val addedEdited: Boolean = false
) {
    val actual: Int get() = (verified.toLong() + added).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    fun withAdded(value: Int): SupplierRefillLine {
        require(value >= 0)
        return copy(added = value, addedEdited = true)
    }
    fun withVerified(value: Int): SupplierRefillLine {
        require(value >= 0)
        return copy(verified = value, added = if (addedEdited) added else
            (maximum.toLong() - value).coerceIn(0, Int.MAX_VALUE.toLong()).toInt())
    }
}

fun createSupplierRefillLines(inventory: StationInventory, settings: StationSettings): List<SupplierRefillLine> =
    WarehouseItemType.entries.map { type ->
        val original = inventory.quantity(type)
        val maximum = settings.maximum(type)
        val added = (maximum.toLong() - original).coerceAtLeast(0).toInt()
        SupplierRefillLine(type, original, maximum, added)
    }

fun List<SupplierRefillLine>.actualInventory(original: StationInventory): StationInventory =
    fold(original) { result, line -> result.withQuantity(line.type, line.actual) }

fun List<SupplierRefillLine>.warehouseWriteOff(source: RefillSource): Map<String, Int> =
    if (source == RefillSource.Warehouse) filter { it.added > 0 }.associate { it.type.code to it.added }
    else emptyMap()

fun warehouseShortages(writeOff: Map<String, Int>, available: Map<String, Int>): Set<String> =
    writeOff.filter { (type, amount) -> amount > (available[type] ?: 0) }.keys
