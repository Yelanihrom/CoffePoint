package ru.coffeepoint.feature.dashboard.model

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.station.model.*

class SupplyStockFilterTest {
    @Test fun `all stations includes full stations without overdue maintenance`() {
        val full = station(1, 10).copy(inventory = StationInventory(10, 10, 10, 10, 10, 10, 10, 10))
        assertTrue(full.supplyItems().isEmpty())
        assertTrue(full.supplyOperatorTechServiceDashboardItems().isEmpty())
        assertTrue(SupplyStockFilter.ALL.matchesStation(full))
        assertFalse(SupplyStockFilter.WARNING.matchesStation(full))
        assertFalse(SupplyStockFilter.CRITICAL.matchesStation(full))
        val excluded = full.copy(station = full.station.copy(excludeFromSupplyPlanning = true))
        assertTrue(SupplyStockFilter.ALL.matchesStation(excluded))
    }

    private fun station(id: Int, cups: Int, minimum: Int = 4) = StationDetail(
        CoffeeStation(id = id, name = "Station $id", lastServiceDate = "", salesAfterService = 0,
            mixersServiceDate = "01.01.2999"),
        StationInventory(cups, 9, 9, 9, 9, 9, 9, 9),
        StationSettings(cupsMax = 10, cupsMin = minimum, lidsMax = 10, lidsMin = 4,
            coffeeMax = 10, coffeeMin = 4, milkMax = 10, milkMin = 4,
            chocolateMax = 10, chocolateMin = 4, toppingMax = 10, toppingMin = 4)
    )

    @Test fun `warning includes critical and half stations but not normal stations`() {
        val stations = listOf(station(1, 3), station(2, 4), station(3, 7))
        assertEquals(listOf(1, 2, 3), stations.filter(SupplyStockFilter.ALL::matchesStation).map { it.station.id })
        assertEquals(listOf(1, 2), stations.filter(SupplyStockFilter.WARNING::matchesStation).map { it.station.id })
        assertEquals(listOf(1), stations.filter(SupplyStockFilter.CRITICAL::matchesStation).map { it.station.id })
    }

    @Test fun `selected stations retain normal supplies and individual refill quantities`() {
        val critical = station(1, 3)
        val before = critical.supplyItems()
        assertTrue(SupplyStockFilter.CRITICAL.matchesStation(critical))
        assertEquals(before, critical.supplyItems())
        assertTrue(before.any { it.itemType == "lids" && it.urgency == SupplyUrgency.NORMAL })
        assertEquals("7", before.single { it.itemType == "cups" }.value)
        assertEquals("1", before.single { it.itemType == "lids" }.value)
    }

    @Test fun `zero minimum and excluded stations do not match urgency filters`() {
        val unconfigured = station(1, 0, minimum = 0)
        assertTrue(SupplyStockFilter.ALL.matchesStation(unconfigured))
        assertFalse(SupplyStockFilter.WARNING.matchesStation(unconfigured))
        assertFalse(SupplyStockFilter.CRITICAL.matchesStation(unconfigured))
        val excluded = station(2, 0).let { it.copy(station = it.station.copy(excludeFromSupplyPlanning = true)) }
        assertFalse(SupplyStockFilter.WARNING.matchesStation(excluded))
        assertFalse(SupplyStockFilter.CRITICAL.matchesStation(excluded))
    }
}
