package ru.coffeepoint.feature.dashboard.model

import ru.coffeepoint.feature.station.model.StationDetail

enum class SupplyStockFilter {
    ALL,
    WARNING,
    CRITICAL;

    fun matchesStation(detail: StationDetail): Boolean {
        val items = detail.supplyItems()
        return when (this) {
            ALL -> true
            WARNING -> items.any { it.isWarning || it.isCritical }
            CRITICAL -> items.any { it.isCritical }
        }
    }
}
