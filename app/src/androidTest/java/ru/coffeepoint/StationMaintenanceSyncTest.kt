package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.mapper.*
import ru.coffeepoint.core.sync.google.*
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi
import ru.coffeepoint.core.sync.google.dto.*
import ru.coffeepoint.feature.station.model.*
import java.net.UnknownHostException

class StationMaintenanceSyncTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun database() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private fun detail() = StationDetail(
        CoffeeStation(7, "Station", lastServiceDate = "01.11.2026 10:00", salesAfterService = 42,
            mixersServiceDate = "01.10.2026", brewUnitServiceDate = "02.10.2026", updatedAt = "2026-11-01T10:00:00Z"),
        StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
        StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10))
    private suspend fun seed(db: AppDatabase) {
        val detail = detail()
        db.coffeeStationDao().insert(detail.toStationEntity())
        db.stationInventoryDao().insert(detail.toInventoryEntity())
        db.stationSettingsDao().insert(detail.toSettingsEntity())
        db.stationMaintenanceDao().upsertAll(detail.station.toMaintenanceEntities().map { it.copy(salesAfterService = 123L) })
    }
    private class Server : GoogleSheetsSyncApi {
        var offline = false
        var gets = 0
        var onGet: (suspend () -> Unit)? = null
        var onPost: (suspend () -> Unit)? = null
        val posts = mutableListOf<GoogleSheetsSyncRequestDto>()
        var maintenance = TechServiceNode.entries.map { node -> mapOf<String,Any?>("stationId" to 7,"nodeType" to node.name,
            "lastServiceDate" to when(node) { TechServiceNode.MIXERS -> "01.10.2026"; TechServiceNode.BREW_UNIT -> "02.10.2026"; else -> "" },
            "salesAfterService" to 123,"salesUpdatedAt" to "2026-11-16T13:00:00Z","updatedAt" to "2026-11-01T10:00:00Z") }
        val station = mutableMapOf<String, Any?>("id" to 7, "name" to "Remote",
            "lastServiceDate" to "01.11.2026 10:00", "salesAfterService" to 42,
            "mixersServiceDate" to "01.10.2026", "brewUnitServiceDate" to "02.10.2026")
        override suspend fun salesAnalytics(url: String, action: String, token: String, startDate: String,
            endDate: String?): SalesAnalyticsResponseDto = error("Not used")
        override suspend fun fullSync(url: String, action: String, token: String, entities: String?, stationId: Int?): GoogleSheetsFullSyncResponseDto {
            gets++
            onGet?.invoke()
            return GoogleSheetsFullSyncResponseDto(true, GoogleSheetsFullSyncDataDto(
                stations = listOf(station.toMap()), inventories = listOf(mapOf<String, Any?>("stationId" to 7) +
                    listOf("cups", "lids", "coffee", "milk", "chocolate", "topping", "sugar", "stirrers", "garbageBags").associateWith { 1 }),
                settings = listOf(mapOf("stationId" to 7)), stationMaintenance = maintenance), null)
        }
        override suspend fun upsert(url: String, request: GoogleSheetsSyncRequestDto): GoogleSheetsSyncResponseDto {
            if (offline) throw UnknownHostException()
            posts += request
            if (request.entity == "station_maintenance") {
                @Suppress("UNCHECKED_CAST")
                val changes = request.data["maintenance"] as List<Map<String,Any?>>
                maintenance = maintenance.map { row -> changes.firstOrNull { it["nodeType"] == row["nodeType"] }?.let {
                    row + it + mapOf("salesAfterService" to if(it["resetSales"] == true) 0 else "",
                        "salesUpdatedAt" to if(it["resetSales"] == true) it["lastServiceDate"] else "")
                } ?: row }
            } else station.putAll(request.data)
            onPost?.invoke()
            return GoogleSheetsSyncResponseDto(true, null)
        }
        fun remote() = GoogleSheetsRemoteDataSource(this, "https://test.invalid/exec", "test-token", reportFailure = { _, _ -> })
        fun manager() = GoogleSheetsSyncManager(remote())
    }
    private fun sync(db: AppDatabase, server: Server) = GoogleSheetsStationSyncService(
        db.coffeeStationDao(), db.stationInventoryDao(), db.stationSettingsDao(), db.stationMaintenanceDao(), server.manager())

    @Test fun fullAndServiceStartImportsReadCountersWithoutChangingCanonicalDates(): Unit = runBlocking {
        val db = database()
        val server = Server()
        try {
            seed(db)
            val changes = listOf(
                mapOf("stationId" to 7, "nodeType" to "MIXERS", "lastServiceDate" to "01.10.2026",
                    "salesAfterService" to 45, "salesUpdatedAt" to "2026-11-16T13:00:00"),
                mapOf("stationId" to 7, "nodeType" to "BREW_UNIT", "lastServiceDate" to "01.10.2026",
                    "salesAfterService" to 99, "salesUpdatedAt" to "2026-11-16T13:00:00"))
            server.maintenance = server.maintenance.map { row -> row + (changes.firstOrNull { it["nodeType"] == row["nodeType"] } ?: emptyMap()) }
            assertEquals(GoogleSheetsImportResult.Success,
                GoogleSheetsImportService(db, server.manager()).importFullSync(false, false))
            assertEquals(45L, db.stationMaintenanceDao().get(7, "MIXERS")!!.salesAfterService)
            assertEquals(99L, db.stationMaintenanceDao().get(7, "BREW_UNIT")!!.salesAfterService)
            assertEquals("01.10.2026", db.stationMaintenanceDao().get(7, "BREW_UNIT")!!.lastServiceDate)
            server.maintenance = server.maintenance.map { if(it["nodeType"] == "MIXERS") it + ("salesAfterService" to 50) else it }
            assertEquals(GoogleSheetsImportResult.Success,
                GoogleSheetsServiceStartImporter(db, server.remote()).refresh(7, false))
            assertEquals(50L, db.stationMaintenanceDao().get(7, "MIXERS")!!.salesAfterService)
        } finally { db.close() }
    }

    @Test fun pendingNodeIsRetriedThroughExistingNarrowRequestAndImportKeepsCounters(): Unit = runBlocking {
        val db = database()
        val server = Server()
        try {
            seed(db)
            val dao = db.stationMaintenanceDao()
            dao.upsert(dao.get(7, "MIXERS")!!.copy(lastServiceDate = "2026-11-16T10:00:00Z", salesAfterService = 0L,
                salesUpdatedAt="2026-11-16T10:00:00Z", updatedAt = "2026-11-16T10:00:00Z", isSynced = false, pendingResetSales=true))
            server.offline = true
            assertFalse(sync(db, server).syncPendingStations().synced)
            assertTrue(dao.get(7, "MIXERS")!!.syncError)
            assertEquals("CONNECTION", dao.get(7, "MIXERS")!!.syncErrorType)
            server.offline = false
            assertTrue(sync(db, server).syncPendingStations().synced)
            val request = server.posts.single()
            assertEquals("complete_service", request.action)
            assertEquals(setOf("id", "maintenance"), request.data.keys)
            assertEquals("station_maintenance", request.entity)
            assertTrue(dao.get(7, "MIXERS")!!.isSynced)
            assertEquals(0L, dao.get(7, "MIXERS")!!.salesAfterService)
            assertTrue(sync(db, server).syncPendingStations().synced)
            assertEquals(1, server.posts.size)
            assertEquals(GoogleSheetsImportResult.Success,
                GoogleSheetsImportService(db, server.manager()).importFullSync(false, false))
            assertEquals(0L, dao.get(7, "MIXERS")!!.salesAfterService)
            assertEquals(123L, dao.get(7, "BREW_UNIT")!!.salesAfterService)
            assertEquals("16.11.2026", toStationDetail(db.coffeeStationDao().getById(7)!!,
                db.stationInventoryDao().getByStationId(7)!!, db.stationSettingsDao().getByStationId(7)!!,
                dao.getByStationId(7)).station.mixersServiceDate)
        } finally { db.close() }
    }

    @Test fun fullImportDoesNotReplaceNodeChangedDuringRequest(): Unit = runBlocking {
        val db = database()
        val server = Server()
        try {
            seed(db)
            val dao = db.stationMaintenanceDao()
            server.onGet = { dao.upsert(dao.get(7, "MIXERS")!!.copy(isSynced = false, lastServiceDate = "16.11.2026")) }
            val importer = GoogleSheetsImportService(db, server.manager())
            assertEquals(GoogleSheetsImportResult.SkippedPendingChanges, importer.importFullSync(false, false))
            assertEquals("16.11.2026", dao.get(7, "MIXERS")!!.lastServiceDate)
            assertEquals("Station", db.coffeeStationDao().getById(7)!!.name)
            assertEquals(GoogleSheetsImportResult.SkippedPendingChanges, importer.importFullSync(false, false))
            assertEquals(1, server.gets)
        } finally { db.close() }
    }

    @Test fun responseForOlderNodeEditDoesNotAcknowledgeNewerEdit(): Unit = runBlocking {
        val db = database()
        val server = Server()
        try {
            seed(db)
            val dao = db.stationMaintenanceDao()
            dao.upsert(dao.get(7, "MIXERS")!!.copy(lastServiceDate = "15.11.2026", salesAfterService=null, isSynced = false, updatedAt = "2026-11-15T10:00:00Z"))
            server.onPost = {
                dao.upsert(dao.get(7, "MIXERS")!!.copy(lastServiceDate = "16.11.2026", updatedAt = "2026-11-16T10:00:00Z"))
            }
            assertFalse(sync(db, server).syncPendingStations().synced)
            assertFalse(dao.get(7, "MIXERS")!!.isSynced)
            server.onPost = null
            assertTrue(sync(db, server).syncPendingStations().synced)
            assertEquals(2, server.posts.size)
            assertEquals("station_maintenance", server.posts.last().entity)
        } finally { db.close() }
    }

    @Test fun fullStationAdapterUsesNodeDatesWithoutSendingLocalCounters(): Unit = runBlocking {
        val db = database()
        val server = Server()
        try {
            seed(db)
            val dao = db.stationMaintenanceDao()
            dao.upsert(dao.get(7, "BREW_UNIT")!!.copy(lastServiceDate = "16.11.2026", salesAfterService = null,
                isSynced = false, updatedAt = "2026-11-16T10:00:00Z", pendingFullStationSync = true))
            assertTrue(sync(db, server).syncPendingStations().synced)
            val request = server.posts.single()
            assertEquals("complete_service", request.action)
            assertEquals("station_maintenance", request.entity)
            assertTrue(request.data.containsKey("maintenance"))
            assertFalse(request.data.containsKey("salesAfterService"))
            assertFalse(request.data.containsKey("station_maintenance"))
            assertTrue(dao.get(7, "BREW_UNIT")!!.isSynced)
            assertEquals(123L, dao.get(7, "MIXERS")!!.salesAfterService)
        } finally { db.close() }
    }

    @Test fun serviceStartRefreshKeepsNodeCountersAndDetectsConcurrentNodeChange(): Unit = runBlocking {
        val db = database()
        val server = Server()
        try {
            seed(db)
            val dao = db.stationMaintenanceDao()
            val importer = GoogleSheetsServiceStartImporter(db, server.remote())
            assertEquals(GoogleSheetsImportResult.Success, importer.refresh(7, false))
            assertEquals(123L, dao.get(7, "MIXERS")!!.salesAfterService)
            server.onGet = { dao.upsert(dao.get(7, "MIXERS")!!.copy(isSynced = false, updatedAt = "newer")) }
            assertEquals(GoogleSheetsImportResult.SkippedPendingChanges, importer.refresh(7, false))
            assertFalse(dao.get(7, "MIXERS")!!.isSynced)
        } finally { db.close() }
    }
}
