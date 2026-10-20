package ru.coffeepoint.core.data.preferences

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoogleSheetsImportPreferences(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        "google_sheets_import",
        Context.MODE_PRIVATE
    )

    fun saveLastImportTime(timeMillis: Long) {
        preferences.edit()
            .putLong(KEY_LAST_IMPORT_TIME, timeMillis)
            .apply()
    }

    fun getLastImportText(): String? {
        val timeMillis = preferences.getLong(KEY_LAST_IMPORT_TIME, 0L)
        if (timeMillis == 0L) return null

        return SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        ).format(Date(timeMillis))
    }

    private companion object {
        const val KEY_LAST_IMPORT_TIME = "last_import_time"
    }
}
