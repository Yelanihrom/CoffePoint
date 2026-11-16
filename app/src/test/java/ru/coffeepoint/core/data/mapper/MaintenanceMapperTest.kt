package ru.coffeepoint.core.data.mapper

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.TechServiceNode

class MaintenanceMapperTest {
    @Test fun `known node counters reach the dashboard without changing operator sales`() {
        val rows = station.toMaintenanceEntities().map {
            when (it.nodeType) {
                "MIXERS" -> it.copy(salesAfterService = 500)
                "COFFEE_GRINDER" -> it.copy(salesAfterService = 0)
                else -> it
            }
        }
        val projected = station.withMaintenance(rows)
        assertEquals(500L, projected.maintenanceSales[TechServiceNode.MIXERS])
        assertEquals(0L, projected.maintenanceSales[TechServiceNode.COFFEE_GRINDER])
        assertNull(projected.maintenanceSales[TechServiceNode.BREW_UNIT])
        assertEquals(42, projected.salesAfterService)
    }

    @Test fun `counter import preserves unknown pending mismatched and newer local states`() {
        val local = ru.coffeepoint.core.data.local.entity.StationMaintenanceEntity(
            7, "MIXERS", "16.11.2026", salesAfterService = 0L, salesUpdatedAt="2026-11-16T10:00:00Z", updatedAt = "2026-11-16T10:00:00Z")
        val remote = local.copy(salesAfterService = 12L, salesUpdatedAt = "2026-11-16T13:15:00")
        assertEquals(12L, local.withRemoteMaintenance(remote).salesAfterService)
        assertEquals(local.updatedAt, local.withRemoteMaintenance(remote).updatedAt)
        listOf(remote.copy(salesAfterService = null), remote.copy(lastServiceDate = "15.11.2026"),
            remote.copy(salesUpdatedAt = "2026-11-16T12:59:00"), remote.copy(salesUpdatedAt = ""),
            remote.copy(stationId = 8), remote.copy(nodeType = "BREW_UNIT")).forEach {
            assertEquals(local, local.withRemoteMaintenance(it))
        }
        val pending = local.copy(isSynced = false)
        assertEquals(pending, pending.withRemoteMaintenance(remote))
        val newer = local.copy(salesUpdatedAt = "2026-11-16T14:00:00", salesAfterService = 30L)
        assertEquals(newer, newer.withRemoteMaintenance(remote))
    }

    private val station = CoffeeStation(7, "Station", lastServiceDate = "01.11.2026 10:00", salesAfterService = 42,
        mixersServiceDate = "01.10.2026", liquidWasteContainerServiceDate = "02.10.2026",
        brewUnitServiceDate = "03.10.2026", coffeeGrinderServiceDate = "04.10.2026",
        expansionTankServiceDate = "05.10.2026", exhaustServiceDate = "06.10.2026")

    @Test fun `six wire dates round trip through independent node rows`() {
        val rows = station.toMaintenanceEntities()
        assertEquals(6, rows.size)
        assertEquals(TechServiceNode.entries.map { it.name }.toSet(), rows.map { it.nodeType }.toSet())
        rows.forEach { assertNull(it.salesAfterService) }
        assertEquals(station, station.copy(mixersServiceDate = "", brewUnitServiceDate = "").withMaintenance(rows))
        assertFalse(CoffeeStationEntity::class.java.declaredFields.any { it.name == "mixersServiceDate" })
    }

    @Test fun `pending node projects status and service flags without changing operator sales`() {
        val rows = station.toMaintenanceEntities().map {
            if (it.nodeType == "MIXERS") it.copy(isSynced = false, syncError = true, syncErrorType = "CONNECTION") else it
        }
        val projected = station.withMaintenance(rows)
        assertFalse(projected.isSynced)
        assertTrue(projected.syncError)
        assertEquals("CONNECTION", projected.syncErrorType)
        assertTrue(projected.pendingSupplyServiceOnly)
        assertTrue(projected.pendingMixersServiceDate)
        assertFalse(projected.pendingLiquidWasteContainerServiceDate)
        assertFalse(projected.pendingServiceOnly)
        assertEquals(42, projected.salesAfterService)
        assertEquals(station.lastServiceDate, projected.lastServiceDate)
    }

    @Test fun `full node update uses existing full station path not supplier service flags`() {
        val rows = station.toMaintenanceEntities().map {
            if (it.nodeType == "BREW_UNIT") it.copy(isSynced = false, pendingFullStationSync = true) else it
        }
        val projected = station.withMaintenance(rows)
        assertFalse(projected.isSynced)
        assertTrue(projected.pendingFullStationSync)
        assertFalse(projected.pendingSupplyServiceOnly)
    }
}
