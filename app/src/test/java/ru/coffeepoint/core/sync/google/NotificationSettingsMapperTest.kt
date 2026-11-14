package ru.coffeepoint.core.sync.google

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.notification.NotificationOption

class NotificationSettingsMapperTest {
    private fun rows() = UserRole.entries.map { role -> buildMap<String, Any?> {
        put("role", role.name)
        put("updatedAt", "2026-11-14T08:15:30.399Z")
        NotificationOption.entries.forEach { put(it.sheetField, it.defaultEnabled(role)) }
    } }

    @Test fun `valid snapshot preserves explicit false and milliseconds`() {
        val actual = parseNotificationSettings(rows())
        assertEquals(3, actual.size)
        val supplier = actual.single { it.role == UserRole.SUPPLY_OPERATOR }
        assertEquals(false, supplier.values[NotificationOption.WAREHOUSE])
        assertEquals(true, supplier.values[NotificationOption.CRITICAL_STOCK])
        assertEquals("2026-11-14T08:15:30.399Z", supplier.updatedAt)
    }

    @Test fun `rejects missing duplicate invalid flags and unsupported roles atomically`() {
        val valid = rows()
        val patches = listOf(
            mapOf("role" to "UNKNOWN"), mapOf("updatedAt" to "bad"),
            mapOf("refillEnabled" to "false"), mapOf("warehouseEnabled" to 0),
            mapOf("warehouseEnabled" to null)
        )
        for (patch in patches) {
            assertThrows(Exception::class.java) { parseNotificationSettings(listOf(valid[0] + patch) + valid.drop(1)) }
        }
        assertThrows(Exception::class.java) { parseNotificationSettings(null) }
        assertThrows(Exception::class.java) { parseNotificationSettings(emptyList()) }
        assertThrows(Exception::class.java) { parseNotificationSettings(valid.dropLast(1)) }
        assertThrows(Exception::class.java) { parseNotificationSettings(listOf(valid[0], valid[0], valid[2])) }
        assertThrows(Exception::class.java) {
            parseNotificationSettings(valid.map { if (it["role"] == "OPERATOR") it + ("brewUnitEnabled" to true) else it })
        }
    }
}
