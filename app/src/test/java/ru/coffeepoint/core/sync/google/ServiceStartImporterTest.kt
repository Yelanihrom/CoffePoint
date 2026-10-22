package ru.coffeepoint.core.sync.google

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.data.mapper.toInventoryEntity
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.feature.station.model.*
import java.io.IOException

class ServiceStartImporterTest {
    private val detail = StationDetail(
        CoffeeStation(id = 7, name = "Station", lastServiceDate = "", salesAfterService = 10),
        StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
        StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10)
    )
    private val snapshot = ServiceStartSnapshot(detail.station, detail.inventory.copy(cups = 8), emptyList())

    private inner class Store : ServiceStartStore {
        var state = ServiceStartLocalState(detail.toStationEntity(), detail.toInventoryEntity(), emptyList())
        var warehouseReads = 0
        var applied = false
        override suspend fun read(stationId: Int, includeWarehouse: Boolean): ServiceStartLocalState {
            if (includeWarehouse) warehouseReads++
            return state.copy(warehouse = if (includeWarehouse) state.warehouse else emptyList())
        }
        override suspend fun applyIfUnchanged(before: ServiceStartLocalState, fresh: ServiceStartSnapshot, includeWarehouse: Boolean): GoogleSheetsImportResult {
            if (read(before.station.id, includeWarehouse) != before) return GoogleSheetsImportResult.SkippedPendingChanges
            applied = true
            return GoogleSheetsImportResult.Success
        }
    }

    @Test fun `pending station or inventory prevents network and replacement`() = runBlocking {
        for (stationPending in listOf(true, false)) {
            val store = Store()
            store.state = if (stationPending) store.state.copy(station = store.state.station.copy(isSynced = false))
                else store.state.copy(inventory = store.state.inventory.copy(isSynced = false))
            val importer = GoogleSheetsServiceStartImporter(store) { _, _ -> error("Must not request") }
            assertEquals(GoogleSheetsImportResult.SkippedPendingChanges, importer.refresh(7, false))
            assertFalse(store.applied)
        }
    }

    @Test fun `operator ignores pending warehouse but supplier protects it`() = runBlocking {
        val store = Store()
        store.state = store.state.copy(warehouse = listOf(WarehouseStockEntity("milk", 2, isSynced = false)))
        var requests = 0
        val importer = GoogleSheetsServiceStartImporter(store) { _, warehouse ->
            assertFalse(warehouse); requests++; snapshot
        }
        assertEquals(GoogleSheetsImportResult.Success, importer.refresh(7, false))
        assertEquals(0, store.warehouseReads)
        assertEquals(GoogleSheetsImportResult.SkippedPendingChanges, importer.refresh(7, true))
        assertEquals(1, requests)
    }

    @Test fun `local change while request runs is not replaced`() = runBlocking {
        val store = Store()
        val importer = GoogleSheetsServiceStartImporter(store) { _, _ ->
            store.state = store.state.copy(inventory = store.state.inventory.copy(cups = 4, isSynced = false))
            snapshot
        }
        assertEquals(GoogleSheetsImportResult.SkippedPendingChanges, importer.refresh(7, false))
        assertFalse(store.applied)
        assertEquals(4, store.state.inventory.cups)
    }

    @Test fun `network error leaves local data unchanged and retry can succeed`() = runBlocking {
        val store = Store()
        val before = store.state
        var fail = true
        val importer = GoogleSheetsServiceStartImporter(store) { _, _ ->
            if (fail) throw IOException("Offline") else snapshot
        }
        assertTrue(importer.refresh(7, true) is GoogleSheetsImportResult.Error)
        assertEquals(before, store.state)
        assertFalse(store.applied)
        fail = false
        assertEquals(GoogleSheetsImportResult.Success, importer.refresh(7, true))
    }

    @Test fun `cancellation is not presented as offline error`() = runBlocking {
        val store = Store()
        val importer = GoogleSheetsServiceStartImporter(store) { _, _ -> throw CancellationException() }
        try { importer.refresh(7, false); fail("Cancellation swallowed") }
        catch (_: CancellationException) { }
        assertFalse(store.applied)
    }

    @Test fun `incomplete warehouse snapshot does not apply station or warehouse`() = runBlocking {
        val store = Store()
        store.state = store.state.copy(warehouse = listOf(WarehouseStockEntity("milk", 2)))
        val importer = GoogleSheetsServiceStartImporter(store) { _, _ -> snapshot }
        assertTrue(importer.refresh(7, true) is GoogleSheetsImportResult.Error)
        assertFalse(store.applied)
    }
}
