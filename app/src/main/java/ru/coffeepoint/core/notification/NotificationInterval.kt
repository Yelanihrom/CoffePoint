package ru.coffeepoint.core.notification

import java.util.concurrent.TimeUnit

internal object NotificationInterval {
    private val intervalMillis = TimeUnit.HOURS.toMillis(6)

    fun isDue(lastShownAt: Long, now: Long): Boolean =
        lastShownAt <= 0 || now < lastShownAt || now - lastShownAt >= intervalMillis
}
