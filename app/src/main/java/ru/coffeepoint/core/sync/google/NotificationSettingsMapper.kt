package ru.coffeepoint.core.sync.google

import java.time.Instant
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.notification.NotificationOption
import ru.coffeepoint.core.notification.NotificationSettingsRow

internal fun parseNotificationSettings(rows: List<Map<String, Any?>>?): List<NotificationSettingsRow> {
    requireNotNull(rows) { "Missing notification settings" }
    val result = rows.map { row ->
        val role = UserRole.valueOf(row["role"] as? String ?: error("Missing notification role"))
        val values = NotificationOption.entries.associateWith { option ->
            val value = row[option.sheetField] as? Boolean ?: error("Invalid notification flag")
            require(!value || option.availableFor(role)) { "Notification unavailable for role" }
            value
        }
        val timestamp = row["updatedAt"] as? String ?: error("Missing notification timestamp")
        NotificationSettingsRow(role, values, Instant.parse(timestamp).toString())
    }
    require(result.size == UserRole.entries.size && result.map { it.role }.toSet() == UserRole.entries.toSet()) {
        "Incomplete or duplicate notification roles"
    }
    return result
}
