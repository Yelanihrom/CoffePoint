package ru.coffeepoint.feature.dashboard.model

import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.notification.NotificationOption
import ru.coffeepoint.feature.station.model.ServiceIntervals
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.feature.station.model.isServiceDue

data class TechServiceDashboardItem(
    val title: String,
    val serviceIntervalDays: Int
)

fun StationDetail.techServiceDashboardItems(intervals: ServiceIntervals = ServiceIntervals()): List<TechServiceDashboardItem> {

    return allTechServiceDashboardItems(intervals)
        .filter { item ->
            isServiceDue(
                date = item.date,
                intervalDays = item.serviceIntervalDays,
                salesAfterService = station.maintenanceSales[item.node],
                salesLimit = intervals.salesLimit(item.node)
            )
        }
        .map { item ->
            TechServiceDashboardItem(
                title = item.title,
                serviceIntervalDays = item.serviceIntervalDays
            )
        }

}

fun StationDetail.supplyOperatorTechServiceDashboardItems(intervals: ServiceIntervals = ServiceIntervals()): List<TechServiceDashboardItem> {
    return techServiceDashboardItems(intervals)
        .filter { item ->
            item.title == "Миксеры"
        }
}

fun StationDetail.supplyAttentionTechServiceDashboardItems(
    role: UserRole?,
    intervals: ServiceIntervals = ServiceIntervals(),
    isEnabled: (NotificationOption) -> Boolean = { role != null && it.defaultEnabled(role) }
): List<TechServiceDashboardItem> {
    if (role == null || role == UserRole.OPERATOR) return emptyList()
    return techServiceDashboardItems(intervals).filter { item ->
        NotificationOption.entries.any { option ->
            option.nodeTitle == item.title && option.availableFor(role) && isEnabled(option)
        }
    }
}

private fun StationDetail.allTechServiceDashboardItems(intervals: ServiceIntervals): List<TechServiceDashboardSourceItem> {

    return listOf(
        TechServiceDashboardSourceItem(
            title = "Миксеры",
            node = TechServiceNode.MIXERS,
            date = station.mixersServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.MIXERS]
        ),
        TechServiceDashboardSourceItem(
            title = "Контейнер жидких отходов",
            node = TechServiceNode.LIQUID_WASTE_CONTAINER,
            date = station.liquidWasteContainerServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.LIQUID_WASTE_CONTAINER]
        ),
        TechServiceDashboardSourceItem(
            title = "Узел заваривания кофе",
            node = TechServiceNode.BREW_UNIT,
            date = station.brewUnitServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.BREW_UNIT]
        ),
        TechServiceDashboardSourceItem(
            title = "Вытяжка",
            node = TechServiceNode.EXHAUST,
            date = station.exhaustServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.EXHAUST]
        ),
        TechServiceDashboardSourceItem(
            title = "Расширительный бочок",
            node = TechServiceNode.EXPANSION_TANK,
            date = station.expansionTankServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.EXPANSION_TANK]
        ),
        TechServiceDashboardSourceItem(
            title = "Кофемолка",
            node = TechServiceNode.COFFEE_GRINDER,
            date = station.coffeeGrinderServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.COFFEE_GRINDER]
        )
    )

}

private data class TechServiceDashboardSourceItem(
    val title: String,
    val node: TechServiceNode,
    val date: String,
    val serviceIntervalDays: Int
)
