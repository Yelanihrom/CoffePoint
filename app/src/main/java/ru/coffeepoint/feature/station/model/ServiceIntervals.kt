package ru.coffeepoint.feature.station.model

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class ServiceIntervals(
    val values: Map<TechServiceNode, Int> = TechServiceNode.entries.associateWith { it.defaultIntervalDays }
) {
    init {
        require(TechServiceNode.entries.all { (values[it] ?: 0) > 0 })
    }

    operator fun get(node: TechServiceNode): Int = values.getValue(node)
}

fun isServiceDue(date: String, intervalDays: Int, today: LocalDate = LocalDate.now(ZoneOffset.UTC)): Boolean {
    if (intervalDays <= 0) return false
    val lastService = runCatching {
        LocalDate.parse(date.take(10), DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT))
    }.getOrNull() ?: return false
    return !lastService.plusDays(intervalDays.toLong()).isAfter(today)
}
