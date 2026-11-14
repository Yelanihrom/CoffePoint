package ru.coffeepoint.core.notification

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.coffeepoint.feature.station.model.CoffeeStation

class StationNotificationLabelTest {
    private fun label(name: String, address: String) = CoffeeStation(
        id = 1, name = name, address = address, lastServiceDate = "", salesAfterService = 0
    ).notificationLabel()

    @Test fun `includes station name and address`() {
        assertEquals("Поликлиника 1 (ул. Ваныкина, 10)",
            label(" Поликлиника 1 ", " ул. Ваныкина, 10 "))
    }

    @Test fun `missing address leaves only name`() {
        assertEquals("Поликлиника 1", label("Поликлиника 1", "  "))
    }

    @Test fun `missing name or duplicated address does not add separators`() {
        assertEquals("ул. Ваныкина, 10", label("", "ул. Ваныкина, 10"))
        assertEquals("Поликлиника 1", label("Поликлиника 1", "Поликлиника 1"))
    }
}
