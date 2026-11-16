package ru.coffeepoint.feature.station.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ServiceIntervalsTest {
    @Test fun defaultsMatchExistingIntervals() {
        val values = ServiceIntervals()
        assertEquals(30, values[TechServiceNode.MIXERS])
        assertEquals(180, values[TechServiceNode.BREW_UNIT])
        assertEquals(180, values[TechServiceNode.LIQUID_WASTE_CONTAINER])
        assertEquals(720, values[TechServiceNode.EXHAUST])
        assertEquals(360, values[TechServiceNode.EXPANSION_TANK])
        assertEquals(720, values[TechServiceNode.COFFEE_GRINDER])
    }

    @Test fun everyNodeIsDueOnTheConfiguredDayNotTheDayBefore() {
        val last = LocalDate.of(2026, 1, 1)
        TechServiceNode.entries.forEach { node ->
            val intervals = ServiceIntervals(ServiceIntervals().values + (node to 45))
            assertFalse(isServiceDue("01.01.2026", intervals[node], last.plusDays(44)))
            assertTrue(isServiceDue("01.01.2026", intervals[node], last.plusDays(45)))
            assertTrue(isServiceDue("01.01.2026", intervals[node], last.plusDays(46)))
        }
    }

    @Test fun invalidDatesAndDisabledValuesDoNotProduceFalseAlerts() {
        val today = LocalDate.of(2026, 11, 16)
        listOf("", "bad", "31.02.2026", "29.02.2025").forEach {
            assertFalse(isServiceDue(it, 30, today))
        }
        assertFalse(isServiceDue("16.11.2026", 30, today))
        assertFalse(isServiceDue("01.01.2026", 0, today))
    }

    @Test fun zeroNegativeAndIncompleteSettingsAreRejected() {
        listOf(0, -1).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) {
                ServiceIntervals(ServiceIntervals().values + (TechServiceNode.MIXERS to invalid))
            }
        }
        assertThrows(IllegalArgumentException::class.java) { ServiceIntervals(emptyMap()) }
        assertThrows(IllegalArgumentException::class.java) {
            ServiceIntervals(salesLimits = ServiceIntervals().salesLimits + (TechServiceNode.MIXERS to -1L))
        }
        assertThrows(IllegalArgumentException::class.java) { ServiceIntervals(salesLimits = emptyMap()) }
    }

    @Test fun salesThresholdIncludesBoundaryAndIgnoresUnknownOrDisabledCounters() {
        val today = LocalDate.of(2026, 11, 17)
        for (count in listOf(null, -1L, 0L, 499L)) {
            assertFalse(isServiceDue("17.11.2026", 30, today, count, 500))
        }
        for (count in listOf(500L, 501L, Long.MAX_VALUE)) {
            assertTrue(isServiceDue("17.11.2026", 30, today, count, 500))
            assertFalse(isServiceDue("17.11.2026", 30, today, count, 0))
        }
        assertTrue(isServiceDue("01.01.2026", 30, today, null, 500))
        assertTrue(isServiceDue("", 30, today, 500, 500))
    }
}
