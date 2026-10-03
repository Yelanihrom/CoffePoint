package ru.coffeepoint.feature.station.model

fun CoffeeStation.visitScheduleText(): String {
    val days = visitDays.toDayNumbers()
    val daysText = days.toDaysText()
    val timeText = if (visitTimeFrom.isNotBlank() && visitTimeTo.isNotBlank()) {
        "$visitTimeFrom-$visitTimeTo"
    } else {
        ""
    }

    return listOf(daysText, timeText)
        .filter { value -> value.isNotBlank() }
        .joinToString(" ")
}

private fun String.toDayNumbers(): List<Int> {
    return split(",")
        .mapNotNull { value -> value.trim().toIntOrNull() }
        .filter { day -> day in 1..7 }
        .distinct()
        .sorted()
}

private fun List<Int>.toDaysText(): String {
    if (isEmpty()) return ""

    val ranges = mutableListOf<String>()
    var rangeStart = first()
    var previous = first()

    drop(1).forEach { day ->
        if (day == previous + 1) {
            previous = day
        } else {
            ranges += formatDayRange(rangeStart, previous)
            rangeStart = day
            previous = day
        }
    }

    ranges += formatDayRange(rangeStart, previous)
    return ranges.joinToString(", ")
}

private fun formatDayRange(
    start: Int,
    end: Int
): String {
    return if (start == end) {
        dayTitle(start)
    } else {
        "${dayTitle(start)} - ${dayTitle(end)}"
    }
}

private fun dayTitle(day: Int): String {
    return when (day) {
        1 -> "Пн."
        2 -> "Вт."
        3 -> "Ср."
        4 -> "Чт."
        5 -> "Пт."
        6 -> "Сб."
        7 -> "Вс."
        else -> ""
    }
}
