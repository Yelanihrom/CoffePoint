package ru.coffeepoint.feature.analytics.model

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.coffeepoint.core.data.local.entity.ServiceVisitHistoryRow
import java.time.LocalDate

class ServiceVisitPeriodTest {
    private val today = LocalDate.of(2026, 11, 14)
    private fun visit(id: String, date: String) = ServiceVisitHistoryRow(
        id, 1, null, null, 2, null, date, false, false
    )

    @Test fun inclusiveDaysUseMoscowMidnightAndKeepPendingOrder() {
        val rows = listOf(
            visit("after", "2026-11-11T21:00:00Z"),
            visit("last", "2026-11-11T20:59:59.999Z"),
            visit("first", "2026-11-10T21:00:00Z"),
            visit("before", "2026-11-10T20:59:59.999Z")
        )
        assertEquals(listOf("last", "first"), filterServiceVisits(rows,
            AnalyticsPeriod(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 11)), today).map { it.id })
    }

    @Test fun openEndIncludesTodayAndOffsetDatesButNotTomorrowOrMalformedValues() {
        val rows = listOf(
            visit("today", "2026-11-14T23:59:59+03:00"),
            visit("tomorrow", "2026-11-14T21:00:00Z"),
            visit("invalid", "bad-date")
        )
        assertEquals(listOf("today"), filterServiceVisits(rows,
            AnalyticsPeriod(today), today).map { it.id })
    }

    @Test fun reversedAndFuturePeriodsAreEmpty() {
        val rows = listOf(visit("today", "2026-11-14T10:00:00Z"))
        assertEquals(emptyList<ServiceVisitHistoryRow>(), filterServiceVisits(rows,
            AnalyticsPeriod(today, today.minusDays(1)), today))
        assertEquals(emptyList<ServiceVisitHistoryRow>(), filterServiceVisits(rows,
            AnalyticsPeriod(today.plusDays(1)), today))
    }
}
