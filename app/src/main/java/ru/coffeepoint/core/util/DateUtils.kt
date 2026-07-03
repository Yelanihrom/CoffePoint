package ru.coffeepoint.core.util

import java.text.SimpleDateFormat
import java.util.Locale

fun toApiDateFormat(localDate: String): String {

    val inputFormat = SimpleDateFormat(
        "dd.MM.yyyy HH:mm",
        Locale.getDefault()
    )

    val outputFormat = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault()
    )

    val parsedDate = inputFormat.parse(localDate)
        ?: return localDate

    return outputFormat.format(parsedDate)

}