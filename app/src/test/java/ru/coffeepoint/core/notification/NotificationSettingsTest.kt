package ru.coffeepoint.core.notification

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole

class NotificationSettingsTest {
    @Test fun `defaults preserve manager and operator alerts but disable supplier warehouse`() {
        assertEquals(setOf(NotificationOption.REFILL), enabled(UserRole.OPERATOR))
        assertEquals(setOf(NotificationOption.CRITICAL_STOCK), enabled(UserRole.SUPPLY_OPERATOR))
        assertEquals(setOf(NotificationOption.REFILL, NotificationOption.CRITICAL_STOCK,
            NotificationOption.WAREHOUSE, NotificationOption.MIXERS, NotificationOption.CONTAINER),
            enabled(UserRole.MANAGER))
    }

    @Test fun `supplier can enable warehouse and every node but cannot enable refill`() {
        assertFalse(NotificationOption.REFILL.availableFor(UserRole.SUPPLY_OPERATOR))
        assertTrue(NotificationOption.WAREHOUSE.availableFor(UserRole.SUPPLY_OPERATOR))
        assertEquals(6, NotificationOption.entries.count {
            it.nodeTitle != null && it.availableFor(UserRole.SUPPLY_OPERATOR)
        })
        assertEquals(6, NotificationOption.entries.mapNotNull { it.nodeTitle }.toSet().size)
    }

    private fun enabled(role: UserRole) = NotificationOption.entries.filter { it.defaultEnabled(role) }.toSet()
}
