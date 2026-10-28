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

    @Test fun `supplier sees mixers but receives no maintenance notifications`() {
        val station = detail()
        assertEquals(listOf("Миксеры"), station.supplyOperatorTechServiceDashboardItems().map { it.title })
        assertTrue(station.supplyAttentionTechServiceDashboardItems(UserRole.SUPPLY_OPERATOR).isEmpty())
    }

    @Test fun `operator and missing role receive no maintenance notifications`() {
        val station = detail()
        assertTrue(station.supplyAttentionTechServiceDashboardItems(UserRole.OPERATOR).isEmpty())
        assertTrue(station.supplyAttentionTechServiceDashboardItems(null).isEmpty())
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

    @Test fun `manager receives no maintenance notifications for nodes not yet due`() {
        val station = detail(mixers = "01.01.2999").let {
            it.copy(station = it.station.copy(liquidWasteContainerServiceDate = "01.01.2999"))
        }
        assertTrue(station.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER).isEmpty())
    }
}
