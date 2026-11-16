package ru.coffeepoint.core.sync.google

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.station.model.TechServiceNode

class MaintenanceSettingsMapperTest {
    private fun rows(): List<Map<String, Any?>> = TechServiceNode.entries.map {
        mapOf("nodeType" to it.name, "intervalDays" to it.defaultIntervalDays,
            "salesLimit" to 0, "updatedAt" to "2026-11-17T00:00:00Z")
    }

    @Test fun `valid settings accept zero and safe maximum including gson numbers`() {
        val list = rows().map { it + ("salesLimit" to MAX_MAINTENANCE_SALES_LIMIT) }
        val jsonRows: List<Map<String, Any?>> = Gson().fromJson(Gson().toJson(list),
            object : TypeToken<List<Map<String, Any?>>>() {}.type)
        assertTrue(parseMaintenanceSettings(jsonRows).all { it.salesLimit == MAX_MAINTENANCE_SALES_LIMIT && it.isSynced })
        assertTrue(parseMaintenanceSettings(rows()).all { it.salesLimit == 0L })
    }

    @Test fun `invalid missing duplicate and incomplete settings fail closed`() {
        val invalid = listOf(null, emptyList(), rows().drop(1), rows() + rows().first()) +
            listOf(mapOf("nodeType" to "UNKNOWN"), mapOf("intervalDays" to 0),
                mapOf("intervalDays" to 1.5), mapOf("intervalDays" to "30"),
                mapOf("intervalDays" to 2147483648L), mapOf("salesLimit" to -1),
                mapOf("salesLimit" to 0.5), mapOf("salesLimit" to "500"),
                mapOf("salesLimit" to 9007199254740992L), mapOf("updatedAt" to "bad"))
                .map { bad -> rows().toMutableList().apply { set(0, first() + bad) } }
        for (candidate in invalid) {
            try { parseMaintenanceSettings(candidate); fail("Invalid settings accepted: $candidate") }
            catch (_: IllegalArgumentException) { }
            catch (_: IllegalStateException) { }
            catch (_: ArithmeticException) { }
            catch (_: java.time.format.DateTimeParseException) { }
        }
    }
}
