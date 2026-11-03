package ru.coffeepoint.feature.analytics.model

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

val ANALYTICS_TIME_ZONE: ZoneId = ZoneId.of("Europe/Moscow")

data class AnalyticsPeriod(val start: LocalDate, val end: LocalDate? = null) {
    fun isValid(today: LocalDate): Boolean =
        start <= today && (end == null || (end >= start && end <= today))

    companion object {
        fun currentMonth(clock: Clock = Clock.system(ANALYTICS_TIME_ZONE)): AnalyticsPeriod =
            AnalyticsPeriod(LocalDate.now(clock.withZone(ANALYTICS_TIME_ZONE)).withDayOfMonth(1))
    }
}

data class StationSales(val stationId: Int, val name: String, val salesCount: Long?)

data class SalesAnalyticsReport(
    val totalSales: Long,
    val stations: List<StationSales>,
    val generatedAt: java.time.Instant
)
