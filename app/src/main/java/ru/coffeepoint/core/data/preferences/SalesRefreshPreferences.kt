package ru.coffeepoint.core.data.preferences

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SalesRefreshPreferences(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        "sales_refresh",
        Context.MODE_PRIVATE
    )

    fun saveLastRefreshTime(
        timeMillis: Long
    ) {

        preferences.edit()
            .putLong(KEY_LAST_REFRESH_TIME, timeMillis)
            .apply()

    }

    fun getLastRefreshText(): String? {

        val timeMillis = preferences.getLong(
            KEY_LAST_REFRESH_TIME,
            0L
        )

        if (timeMillis == 0L) return null

        val formatter = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        )

        return formatter.format(Date(timeMillis))

    }

    private companion object {

        const val KEY_LAST_REFRESH_TIME = "last_refresh_time"

    }

}
