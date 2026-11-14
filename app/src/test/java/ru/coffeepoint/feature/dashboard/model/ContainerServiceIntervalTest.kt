package ru.coffeepoint.feature.dashboard.model

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.feature.station.model.*

class ContainerServiceIntervalTest {
    private fun station(daysSinceService: Long): StationDetail {
        val date = LocalDate.now(ZoneOffset.UTC).minusDays(daysSinceService)
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        return StationDetail(
            CoffeeStation(
                id = 1, name = "Station", lastServiceDate = "", salesAfterService = 0,
                liquidWasteContainerServiceDate = date, mixersServiceDate = date,
                brewUnitServiceDate = date, coffeeGrinderServiceDate = date,
                expansionTankServiceDate = date, exhaustServiceDate = date
            ),
            StationInventory(0, 0, 0, 0, 0, 0, 0, 0),
            StationSettings(cupsMax = 10, lidsMax = 10, coffeeMax = 10, milkMax = 10,
                chocolateMax = 10, toppingMax = 10)
        )
    }

    @Test fun `container is due from day 180 only`() {
        fun items(days: Long) = station(days).techServiceDashboardItems()
            .filter { it.title == "Контейнер жидких отходов" }
        assertTrue(items(60).isEmpty())
        assertTrue(items(179).isEmpty())
        assertEquals(180, items(180).single().serviceIntervalDays)
        assertEquals(1, items(181).size)
    }

    @Test fun `supplier receives no node alerts even when every node is overdue`() {
        val detail = station(721)
        assertEquals(6, detail.techServiceDashboardItems().size)
        assertTrue(detail.supplyAttentionTechServiceDashboardItems(UserRole.SUPPLY_OPERATOR).isEmpty())
        assertEquals(setOf("Миксеры", "Контейнер жидких отходов"),
            detail.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER).map { it.title }.toSet())
    }
}
