package ru.coffeepoint.feature.analytics.model

import ru.coffeepoint.core.data.local.entity.ServiceVisitHistoryRow
import java.time.Instant
import java.time.LocalDate

fun filterServiceVisits(
    visits: List<ServiceVisitHistoryRow>, period: AnalyticsPeriod,
    today: LocalDate = LocalDate.now(ANALYTICS_TIME_ZONE)
): List<ServiceVisitHistoryRow> {
    if (!period.isValid(today)) return emptyList()
    val start = period.start.atStartOfDay(ANALYTICS_TIME_ZONE).toInstant()
    val endExclusive = (period.end ?: today).plusDays(1).atStartOfDay(ANALYTICS_TIME_ZONE).toInstant()
    return visits.filter { visit ->
        runCatching { Instant.parse(visit.completedAt) }.getOrNull()?.let {
            it >= start && it < endExclusive
        } ?: false
    }
}
