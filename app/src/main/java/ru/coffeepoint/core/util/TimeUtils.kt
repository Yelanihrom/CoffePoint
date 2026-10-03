package ru.coffeepoint.core.util

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun normalizeTime(value: String): String {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return ""

    Regex("^(\\d{1,2}):(\\d{2})(?::\\d{2})?$")
        .matchEntire(trimmed)
        ?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return trimmed
            val minute = match.groupValues[2].toIntOrNull() ?: return trimmed
            return formatTime(hour, minute)
        }

    val inputFormats = listOf(
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.getDefault()
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        },
        SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            Locale.getDefault()
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
    )

    val date = inputFormats.firstNotNullOfOrNull { formatter ->
        runCatching { formatter.parse(trimmed) }.getOrNull()
    } ?: return trimmed

    return SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Europe/Moscow")
    }.format(date)
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format(
        Locale.getDefault(),
        "%02d:%02d",
        hour.coerceIn(0, 23),
        minute.coerceIn(0, 59)
    )
}
