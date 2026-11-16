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
                intervalDays = item.serviceIntervalDays
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
            date = station.mixersServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.MIXERS]
        ),
        TechServiceDashboardSourceItem(
            title = "Контейнер жидких отходов",
            date = station.liquidWasteContainerServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.LIQUID_WASTE_CONTAINER]
        ),
        TechServiceDashboardSourceItem(
            title = "Узел заваривания кофе",
            date = station.brewUnitServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.BREW_UNIT]
        ),
        TechServiceDashboardSourceItem(
            title = "Вытяжка",
            date = station.exhaustServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.EXHAUST]
        ),
        TechServiceDashboardSourceItem(
            title = "Расширительный бочок",
            date = station.expansionTankServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.EXPANSION_TANK]
        ),
        TechServiceDashboardSourceItem(
            title = "Кофемолка",
            date = station.coffeeGrinderServiceDate,
            serviceIntervalDays = intervals[TechServiceNode.COFFEE_GRINDER]
        )
    )

}

private data class TechServiceDashboardSourceItem(
    val title: String,
    val date: String,
    val serviceIntervalDays: Int
)
