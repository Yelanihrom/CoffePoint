package ru.coffeepoint.core.sync.google

import java.math.BigDecimal
import java.time.Instant
import ru.coffeepoint.core.data.local.entity.MaintenanceSettingsEntity
import ru.coffeepoint.feature.station.model.TechServiceNode

const val MAX_MAINTENANCE_SALES_LIMIT = 9007199254740991L

internal fun parseMaintenanceSettings(rows: List<Map<String, Any?>>?): List<MaintenanceSettingsEntity> {
    requireNotNull(rows) { "Missing maintenance settings" }
    val result = rows.map { row ->
        val node = TechServiceNode.valueOf(row["nodeType"] as? String ?: error("Missing node type"))
        fun integer(field: String): Long {
            val raw = row[field]
            require(raw is Number) { "Invalid maintenance number" }
            return BigDecimal(raw.toString()).longValueExact()
        }
        val days = integer("intervalDays")
        val sales = integer("salesLimit")
        require(days in 1..Int.MAX_VALUE.toLong() && sales in 0..MAX_MAINTENANCE_SALES_LIMIT)
        MaintenanceSettingsEntity(node.name, days.toInt(), sales,
            Instant.parse(row["updatedAt"] as? String ?: error("Missing maintenance version")).toString())
    }
    require(result.size == TechServiceNode.entries.size &&
        result.map { it.nodeType }.toSet() == TechServiceNode.entries.map { it.name }.toSet()) {
        "Incomplete or duplicate maintenance settings"
    }
    return result
}
