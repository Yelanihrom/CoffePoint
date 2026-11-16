package ru.coffeepoint.feature.station.model

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class ServiceIntervals(
    val values: Map<TechServiceNode, Int> = TechServiceNode.entries.associateWith { it.defaultIntervalDays },
    val salesLimits: Map<TechServiceNode, Long> = TechServiceNode.entries.associateWith { 0L }
) {
    init {
        require(TechServiceNode.entries.all { (values[it] ?: 0) > 0 })
        require(TechServiceNode.entries.all { (salesLimits[it] ?: -1L) >= 0 })
    }

    operator fun get(node: TechServiceNode): Int = values.getValue(node)

    fun salesLimit(node: TechServiceNode): Long = salesLimits.getValue(node)
}

fun isServiceDue(date: String, intervalDays: Int, today: LocalDate = LocalDate.now(ZoneOffset.UTC),
    salesAfterService: Long? = null, salesLimit: Long = 0): Boolean {
    if (salesLimit > 0 && salesAfterService != null && salesAfterService >= salesLimit) return true
    if (intervalDays <= 0) return false
    val lastService = runCatching {
        LocalDate.parse(date.take(10), DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT))
    }.getOrNull() ?: return false
    return !lastService.plusDays(intervalDays.toLong()).isAfter(today)
}
