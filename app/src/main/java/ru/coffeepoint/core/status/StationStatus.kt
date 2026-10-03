package ru.coffeepoint.core.status

import ru.coffeepoint.feature.station.model.StationDetail
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

data class StationAttentionIssue(
    val description: String,
    val severity: StationStatusSeverity,
    val affectsStationStatus: Boolean = true
)

enum class StationStatusSeverity {
    OK,
    WARNING,
    CRITICAL
}

fun StationDetail.statusSeverity(): StationStatusSeverity {

    return attentionIssues()
        .filter { issue -> issue.affectsStationStatus }
        .maxByOrNull { issue -> issue.severity.ordinal }
        ?.severity
        ?: StationStatusSeverity.OK

}

fun StationDetail.attentionIssues(): List<StationAttentionIssue> {

    val issues = mutableListOf<StationAttentionIssue>()

    when {
        station.salesAfterService >= 120 -> {
            issues += StationAttentionIssue(
                description = "продажи ${station.salesAfterService}",
                severity = StationStatusSeverity.CRITICAL
            )
        }

        station.salesAfterService >= 100 -> {
            issues += StationAttentionIssue(
                description = "продажи ${station.salesAfterService}",
                severity = StationStatusSeverity.WARNING
            )
        }
    }

    val daysAfterService = daysAfterService()

    when {
        daysAfterService >= 14 -> {
            issues += StationAttentionIssue(
                description = "обслуживание $daysAfterService дн.",
                severity = StationStatusSeverity.CRITICAL
            )
        }

        daysAfterService >= 10 -> {
            issues += StationAttentionIssue(
                description = "обслуживание $daysAfterService дн.",
                severity = StationStatusSeverity.WARNING
            )
        }
    }

    if (isRentOverdue()) {
        issues += StationAttentionIssue(
            description = "аренда просрочена",
            severity = StationStatusSeverity.WARNING,
            affectsStationStatus = false
        )
    }

    return issues

}

fun StationDetail.statusText(): String {

    return when (statusSeverity()) {
        StationStatusSeverity.CRITICAL -> "Требует обслуживания"
        StationStatusSeverity.WARNING -> "Требует внимания"
        StationStatusSeverity.OK -> "Все в порядке"
    }

}

private fun StationDetail.daysAfterService(): Long {

    if (station.lastServiceDate.isBlank()) return 0

    val formatter = SimpleDateFormat(
        "dd.MM.yyyy HH:mm",
        Locale.getDefault()
    ).apply {
        isLenient = false
    }

    val serviceDate = formatter.parse(station.lastServiceDate)
        ?: return 0

    val serviceStart = Calendar.getInstance().apply {
        time = serviceDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return TimeUnit.MILLISECONDS.toDays(
        todayStartMillis() - serviceStart
    ).coerceAtLeast(0)

}

private fun StationDetail.isRentOverdue(): Boolean {

    if (station.rentPaidUntil.isBlank()) return false

    val formatter = SimpleDateFormat(
        "dd.MM.yyyy",
        Locale.getDefault()
    ).apply {
        isLenient = false
    }

    val rentDate = formatter.parse(station.rentPaidUntil)
        ?: return false

    val rentStart = Calendar.getInstance().apply {
        time = rentDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return rentStart < todayStartMillis()

}

private fun todayStartMillis(): Long {

    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

}
