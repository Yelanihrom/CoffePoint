package ru.coffeepoint.feature.analytics

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsPeriodTest {
    @Test fun `default month follows Moscow even when UTC month has not changed`() {
        val clock = Clock.fixed(Instant.parse("2026-10-31T21:01:00Z"), ZoneOffset.UTC)
        val period = AnalyticsPeriod.currentMonth(clock)
        assertEquals(LocalDate.parse("2026-11-01"), period.start)
        assertNull(period.end)
    }

    @Test fun `period rejects future and reversed dates but accepts same day and open end`() {
        val today = LocalDate.parse("2026-11-03")
        assertTrue(AnalyticsPeriod(today).isValid(today))
        assertTrue(AnalyticsPeriod(today, today).isValid(today))
        assertFalse(AnalyticsPeriod(today.plusDays(1)).isValid(today))
        assertFalse(AnalyticsPeriod(today, today.minusDays(1)).isValid(today))
        assertFalse(AnalyticsPeriod(today, today.plusDays(1)).isValid(today))
    }
}
