package ru.coffeepoint.feature.dashboard.model

import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.core.auth.UserRole
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val MIXERS_SERVICE_INTERVAL_DAYS = 30
private const val LIQUID_WASTE_CONTAINER_SERVICE_INTERVAL_DAYS = 60
private const val BREW_UNIT_SERVICE_INTERVAL_DAYS = 360
private const val EXHAUST_SERVICE_INTERVAL_DAYS = 360
private const val EXPANSION_TANK_SERVICE_INTERVAL_DAYS = 360
private const val COFFEE_GRINDER_SERVICE_INTERVAL_DAYS = 720

data class TechServiceDashboardItem(
    val title: String,
    val serviceIntervalDays: Int
)

fun StationDetail.techServiceDashboardItems(): List<TechServiceDashboardItem> {

    return allTechServiceDashboardItems()
        .filter { item ->
            isTechServiceOverdue(
                date = item.date,
                serviceIntervalDays = item.serviceIntervalDays
            )
        }
        .map { item ->
            TechServiceDashboardItem(
                title = item.title,
                serviceIntervalDays = item.serviceIntervalDays
            )
        }

}

fun StationDetail.supplyOperatorTechServiceDashboardItems(): List<TechServiceDashboardItem> {
    return techServiceDashboardItems()
        .filter { item ->
            item.title == "Миксеры"
        }
}

fun StationDetail.supplyAttentionTechServiceDashboardItems(role: UserRole?): List<TechServiceDashboardItem> {
    return if (role == UserRole.MANAGER) {
        techServiceDashboardItems().filter { it.title == "Миксеры" || it.title == "Контейнер жидких отходов" }
    } else {
        emptyList()
    }
}

private fun StationDetail.allTechServiceDashboardItems(): List<TechServiceDashboardSourceItem> {

    return listOf(
        TechServiceDashboardSourceItem(
            title = "Миксеры",
            date = station.mixersServiceDate,
            serviceIntervalDays = MIXERS_SERVICE_INTERVAL_DAYS
        ),
        TechServiceDashboardSourceItem(
            title = "Контейнер жидких отходов",
            date = station.liquidWasteContainerServiceDate,
            serviceIntervalDays = LIQUID_WASTE_CONTAINER_SERVICE_INTERVAL_DAYS
        ),
        TechServiceDashboardSourceItem(
            title = "Узел заваривания кофе",
            date = station.brewUnitServiceDate,
            serviceIntervalDays = BREW_UNIT_SERVICE_INTERVAL_DAYS
        ),
        TechServiceDashboardSourceItem(
            title = "Вытяжка",
            date = station.exhaustServiceDate,
            serviceIntervalDays = EXHAUST_SERVICE_INTERVAL_DAYS
        ),
        TechServiceDashboardSourceItem(
            title = "Расширительный бочок",
            date = station.expansionTankServiceDate,
            serviceIntervalDays = EXPANSION_TANK_SERVICE_INTERVAL_DAYS
        ),
        TechServiceDashboardSourceItem(
            title = "Кофемолка",
            date = station.coffeeGrinderServiceDate,
            serviceIntervalDays = COFFEE_GRINDER_SERVICE_INTERVAL_DAYS
        )
    )

}

private data class TechServiceDashboardSourceItem(
    val title: String,
    val date: String,
    val serviceIntervalDays: Int
)

private fun isTechServiceOverdue(
    date: String,
    serviceIntervalDays: Int
): Boolean {

    val serviceDate = parseDateToMillis(date) ?: return false
    val dueDate = serviceDate + TimeUnit.DAYS.toMillis(
        serviceIntervalDays.toLong()
    )

    return dueDate <= todayStartMillis()

}

private fun parseDateToMillis(
    date: String
): Long? {

    if (date.isBlank()) return null

    return dateFormatter().parse(date)?.time

}

private fun todayStartMillis(): Long {

    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

}

private fun dateFormatter(): SimpleDateFormat {

    return SimpleDateFormat(
        "dd.MM.yyyy",
        Locale.getDefault()
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

}

