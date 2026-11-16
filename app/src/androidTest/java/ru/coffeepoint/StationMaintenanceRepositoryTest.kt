package ru.coffeepoint

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.mapper.*
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.station.model.*
import kotlin.random.Random

class StationMaintenanceRepositoryTest {
    @Test fun stationReadsNodesManualDatePreservesOrResetsCounterAndOperatorRefillKeepsAllNodes(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT && BuildConfig.TEST_SHEETS_WEB_APP_URL.isBlank())
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = DatabaseProvider.getDatabase(context)
        val id = Random.nextInt(1_000_000, Int.MAX_VALUE)
        assertNull(db.coffeeStationDao().getById(id))
        val detail = StationDetail(CoffeeStation(id, "Maintenance test", lastServiceDate = "01.11.2026 10:00",
            salesAfterService = 42, mixersServiceDate = "01.10.2026", brewUnitServiceDate = "02.10.2026"),
            StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
            StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10))
        try {
            db.coffeeStationDao().insert(detail.toStationEntity())
            db.stationInventoryDao().insert(detail.toInventoryEntity())
            db.stationSettingsDao().insert(detail.toSettingsEntity())
            val nodes = db.stationMaintenanceDao()
            nodes.upsertAll(detail.station.toMaintenanceEntities().map { it.copy(salesAfterService = 321L) })
            val repository = StationRepository(context)
            assertEquals("01.10.2026", repository.getStation(id)!!.station.mixersServiceDate)
            assertEquals("02.10.2026", repository.getStations().single { it.station.id == id }.station.brewUnitServiceDate)
            assertFalse(repository.updateTechServiceDate(id, TechServiceNode.BREW_UNIT, "15.11.2026"))
            assertEquals(321L, nodes.get(id, "BREW_UNIT")!!.salesAfterService)
            assertFalse(nodes.get(id, "BREW_UNIT")!!.pendingResetSales)
            assertEquals("15.11.2026", repository.getStation(id)!!.station.brewUnitServiceDate)
            assertTrue(nodes.getByStationId(id).filter { it.nodeType != "BREW_UNIT" }.all { it.salesAfterService == 321L })
            assertFalse(repository.updateTechServiceDate(id, TechServiceNode.BREW_UNIT, "15.11.2026", resetSales=true))
            assertEquals(0L, nodes.get(id, "BREW_UNIT")!!.salesAfterService)
            assertTrue(nodes.get(id, "BREW_UNIT")!!.pendingResetSales)
            val before = nodes.getByStationId(id)
            assertFalse(repository.finishService(id, null, null, "16.11.2026 15:00"))
            assertEquals(0, repository.getStation(id)!!.station.salesAfterService)
            assertEquals(before, nodes.getByStationId(id))
        } finally {
            WorkManager.getInstance(context).cancelUniqueWork("google_sheets_sync_now").result.get()
            db.stationInventoryDao().deleteByStationId(id)
            db.stationSettingsDao().deleteByStationId(id)
            db.coffeeStationDao().deleteById(id)
        }
    }
}
