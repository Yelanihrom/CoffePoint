package ru.coffeepoint.core.notification

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NotificationIntervalTest {
    private val now = TimeUnit.DAYS.toMillis(100)
    private val sixHours = TimeUnit.HOURS.toMillis(6)

    @Test fun `first notification is allowed`() {
        assertTrue(NotificationInterval.isDue(0L, now))
    }

    @Test fun `hourly scans and retries do not repeat notification before six hours`() {
        for (hours in 0L..5L) {
            assertFalse(NotificationInterval.isDue(now, now + TimeUnit.HOURS.toMillis(hours)))
        }
        assertFalse(NotificationInterval.isDue(now, now + sixHours - 1))
    }

    @Test fun `notification is allowed at six hours and later`() {
        assertTrue(NotificationInterval.isDue(now, now + sixHours))
        assertTrue(NotificationInterval.isDue(now, now + sixHours + 1))
    }

    @Test fun `clock rollback does not block notifications indefinitely`() {
        assertTrue(NotificationInterval.isDue(now, now - 1))
    }
}
