package ru.coffeepoint.feature.dashboard.model

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.coffeepoint.feature.station.model.*

class BrewUnitServiceIntervalTest {
    private fun brewUnitItems(daysSinceService: Long) = StationDetail(
        CoffeeStation(
            id = 1, name = "Station", lastServiceDate = "", salesAfterService = 0,
            brewUnitServiceDate = LocalDate.now(ZoneOffset.UTC).minusDays(daysSinceService)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        ),
        StationInventory(0, 0, 0, 0, 0, 0, 0, 0),
        StationSettings(cupsMax = 10, lidsMax = 10, coffeeMax = 10, milkMax = 10,
            chocolateMax = 10, toppingMax = 10)
    ).techServiceDashboardItems().filter { it.title == "Узел заваривания кофе" }

    @Test fun `brew unit is not due before 180 days`() {
        assertEquals(0, brewUnitItems(179).size)
    }

    @Test fun `brew unit is due at 180 days and later`() {
        assertEquals(180, brewUnitItems(180).single().serviceIntervalDays)
        assertEquals(1, brewUnitItems(181).size)
    }
}
