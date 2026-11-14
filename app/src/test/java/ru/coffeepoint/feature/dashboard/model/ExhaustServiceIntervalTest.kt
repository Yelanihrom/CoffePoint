package ru.coffeepoint.feature.dashboard.model

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.coffeepoint.feature.station.model.*

class ExhaustServiceIntervalTest {
    private fun exhaustItems(daysSinceService: Long) = StationDetail(
        CoffeeStation(
            id = 1, name = "Station", lastServiceDate = "", salesAfterService = 0,
            exhaustServiceDate = LocalDate.now(ZoneOffset.UTC).minusDays(daysSinceService)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        ),
        StationInventory(0, 0, 0, 0, 0, 0, 0, 0),
        StationSettings(cupsMax = 10, lidsMax = 10, coffeeMax = 10, milkMax = 10,
            chocolateMax = 10, toppingMax = 10)
    ).techServiceDashboardItems().filter { it.title == "Вытяжка" }

    @Test fun `exhaust is not due after 360 or 719 days`() {
        assertEquals(0, exhaustItems(360).size)
        assertEquals(0, exhaustItems(719).size)
    }

    @Test fun `exhaust is due at 720 days and later`() {
        assertEquals(720, exhaustItems(720).single().serviceIntervalDays)
        assertEquals(1, exhaustItems(721).size)
    }
}
