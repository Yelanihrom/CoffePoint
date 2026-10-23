package ru.coffeepoint.feature.dashboard.model

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.feature.station.model.*

class SupplyMaintenanceResponsibilitiesTest {
    private fun detail(mixers: String = "01.01.2000") = StationDetail(
        CoffeeStation(id = 7, name = "Station", lastServiceDate = "", salesAfterService = 0,
            mixersServiceDate = mixers, liquidWasteContainerServiceDate = "01.01.2000"),
        StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
        StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10)
    )

    @Test fun `supplier sees and receives only mixers even when container overdue`() {
        val station = detail()
        assertEquals(listOf("Миксеры"), station.supplyOperatorTechServiceDashboardItems().map { it.title })
        assertEquals(listOf("Миксеры"), station.supplyAttentionTechServiceDashboardItems(UserRole.SUPPLY_OPERATOR).map { it.title })
    }

    @Test fun `container stays in manager notifications and maintenance data`() {
        val station = detail()
        assertEquals(setOf("Миксеры", "Контейнер жидких отходов"),
            station.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER).map { it.title }.toSet())
        assertTrue(station.techServiceDashboardItems().any { it.title == "Контейнер жидких отходов" })
    }

    @Test fun `overdue container alone does not put station in supplier maintenance`() {
        val station = detail(mixers = "01.01.2999")
        assertTrue(station.supplyOperatorTechServiceDashboardItems().isEmpty())
        assertTrue(station.supplyAttentionTechServiceDashboardItems(UserRole.SUPPLY_OPERATOR).isEmpty())
        assertEquals(listOf("Контейнер жидких отходов"),
            station.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER).map { it.title })
    }
}
