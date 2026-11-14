package ru.coffeepoint.feature.earnings.ui

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class EarningsDefaultPeriodTest {
    @Test fun `default seven days include today after Moscow midnight before UTC midnight`() {
        val now = Instant.parse("2026-11-10T21:01:00Z")
        assertEquals("11.11.2026", currentDisplayDate(now))
        assertEquals("05.11.2026", lastSevenDaysStartDisplayDate(now))
    }

    @Test fun `default period switches at Moscow midnight`() {
        val now = Instant.parse("2026-11-10T20:59:00Z")
        assertEquals("10.11.2026", currentDisplayDate(now))
        assertEquals("04.11.2026", lastSevenDaysStartDisplayDate(now))
    }
}
