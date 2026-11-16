package ru.coffeepoint

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.mapper.*
import ru.coffeepoint.core.data.repository.SupplierServiceCompletionRepository
import ru.coffeepoint.feature.station.model.*
import java.time.Instant
import java.util.UUID

internal fun SQLiteDatabase.restoreVersion38MaintenanceColumns() {
    execSQL("DROP TABLE station_maintenance")
    listOf("mixersServiceDate", "liquidWasteContainerServiceDate", "brewUnitServiceDate",
        "coffeeGrinderServiceDate", "expansionTankServiceDate", "exhaustServiceDate").forEach {
        execSQL("ALTER TABLE coffee_station ADD COLUMN $it TEXT NOT NULL DEFAULT ''")
    }
}

class StationMaintenanceDatabaseTest {
    @Test fun migration39RetainsPendingResetDecisionWithoutActivatingOtherCounters(): Unit = runBlocking {
        val name = "maintenance39-${UUID.randomUUID()}.db"
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().let { db ->
                db.coffeeStationDao().insert(detail().toStationEntity())
                db.stationMaintenanceDao().upsertAll(listOf(
                    ru.coffeepoint.core.data.local.entity.StationMaintenanceEntity(7,"MIXERS","16.11.2026",0L,
                        "2026-11-16T10:00:00Z","2026-11-16T10:00:00Z",isSynced=false),
                    ru.coffeepoint.core.data.local.entity.StationMaintenanceEntity(7,"BREW_UNIT","15.11.2026",0L,
                        "2026-11-15T10:00:00Z","2026-11-15T10:00:00Z")))
                db.close()
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path,null,SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("ALTER TABLE station_maintenance DROP COLUMN pendingResetSales")
                it.version=39
            }
            val migrated=Room.databaseBuilder(context,AppDatabase::class.java,name)
                .addMigrations(DatabaseProvider.MIGRATION_39_40).build()
            try {
                assertTrue(migrated.stationMaintenanceDao().get(7,"MIXERS")!!.pendingResetSales)
                assertEquals("2026-11-16T10:00:00Z",migrated.stationMaintenanceDao().get(7,"MIXERS")!!.lastServiceDate)
                assertFalse(migrated.stationMaintenanceDao().get(7,"BREW_UNIT")!!.pendingResetSales)
                assertSchema(migrated)
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun database() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private fun detail(id: Int = 7) = StationDetail(
        CoffeeStation(id, "Station", lastServiceDate = "01.11.2026 10:00", salesAfterService = 42,
            mixersServiceDate = "01.10.2026", brewUnitServiceDate = "02.10.2026"),
        StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
        StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10))

    @Test fun migrationMovesSixDatesPreservesPendingAndMoneyAndDoesNotInventCounts(): Unit = runBlocking {
        val name = "maintenance-migration-${UUID.randomUUID()}.db"
        val payment = EmployeeTransactionEntity("old-payment", 1, "PAYMENT", "2026-11-01", null, null,
            null, 1000, "", "2026-11-01", "2026-11-01", isSynced = false)
        try {
            val original = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                val base = detail().toStationEntity()
                original.coffeeStationDao().insertAll(listOf(
                    base,
                    base.copy(id = 8, isSynced = false, pendingSupplyServiceOnly = true,
                        pendingMixersServiceDate = true, syncError = true, syncErrorType = "CONNECTION"),
                    base.copy(id = 9, isSynced = false, pendingFullStationSync = true),
                    base.copy(id = 10, isSynced = false, pendingServiceOnly = true),
                    base.copy(id = 11, isSynced = false),
                    base.copy(id = 12, isSynced = false, pendingSupplyServiceOnly = true,
                        pendingLiquidWasteContainerServiceDate = true)
                ))
                original.stationInventoryDao().insert(detail().toInventoryEntity())
                original.employeeTransactionDao().insert(payment)
            } finally { original.close() }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.restoreVersion38MaintenanceColumns()
                it.execSQL("""UPDATE coffee_station SET mixersServiceDate = '01.10.2026',
                    liquidWasteContainerServiceDate = '02.10.2026', brewUnitServiceDate = '03.10.2026',
                    coffeeGrinderServiceDate = '04.10.2026', expansionTankServiceDate = '05.10.2026',
                    exhaustServiceDate = '06.10.2026'""")
                it.version = 38
            }
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(DatabaseProvider.MIGRATION_38_39, DatabaseProvider.MIGRATION_39_40).build()
            try {
                val dao = migrated.stationMaintenanceDao()
                assertEquals(6, dao.getByStationId(7).size)
                val expected = mapOf("MIXERS" to "01.10.2026", "LIQUID_WASTE_CONTAINER" to "02.10.2026",
                    "BREW_UNIT" to "03.10.2026", "COFFEE_GRINDER" to "04.10.2026",
                    "EXPANSION_TANK" to "05.10.2026", "EXHAUST" to "06.10.2026")
                dao.getByStationId(7).forEach { node ->
                    assertEquals(expected[node.nodeType], node.lastServiceDate)
                    assertNull(node.salesAfterService)
                    assertEquals("", node.salesUpdatedAt)
                    assertTrue(node.isSynced)
                }
                assertEquals(listOf("MIXERS"), dao.getByStationId(8).filter { !it.isSynced }.map { it.nodeType })
                assertEquals("CONNECTION", dao.get(8, "MIXERS")!!.syncErrorType)
                assertEquals(6, dao.getByStationId(9).count { !it.isSynced && it.pendingFullStationSync })
                assertTrue(dao.getByStationId(10).all { it.isSynced })
                assertEquals(6, dao.getByStationId(11).count { !it.isSynced && it.pendingFullStationSync })
                assertEquals(listOf("LIQUID_WASTE_CONTAINER"), dao.getByStationId(12).filter { !it.isSynced }.map { it.nodeType })
                assertEquals(42, migrated.coffeeStationDao().getById(7)!!.salesAfterService)
                assertEquals(1, migrated.stationInventoryDao().getByStationId(7)!!.cups)
                assertEquals(listOf(payment), migrated.employeeTransactionDao().getAll())
                migrated.openHelper.writableDatabase.query("PRAGMA table_info(coffee_station)").use { cursor ->
                    val oldColumns = setOf("mixersServiceDate", "liquidWasteContainerServiceDate", "brewUnitServiceDate",
                        "coffeeGrinderServiceDate", "expansionTankServiceDate", "exhaustServiceDate")
                    while (cursor.moveToNext()) assertFalse(cursor.getString(cursor.getColumnIndexOrThrow("name")) in oldColumns)
                }
                assertSchema(migrated)
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun upsertKeepsCountersDeletionCascadesAndOrphanIsRejected(): Unit = runBlocking {
        val db = database()
        try {
            val detail = detail()
            db.coffeeStationDao().insert(detail.toStationEntity())
            db.stationMaintenanceDao().upsertAll(detail.station.toMaintenanceEntities().map { it.copy(salesAfterService = 123L) })
            db.coffeeStationDao().insert(detail.toStationEntity().copy(name = "Renamed"))
            assertEquals(6, db.stationMaintenanceDao().getByStationId(7).size)
            assertTrue(db.stationMaintenanceDao().getByStationId(7).all { it.salesAfterService == 123L })
            db.coffeeStationDao().deleteById(7)
            assertTrue(db.stationMaintenanceDao().getByStationId(7).isEmpty())
            try {
                db.stationMaintenanceDao().upsert(detail.station.toMaintenanceEntities().first())
                fail("Orphan maintenance row accepted")
            } catch (_: android.database.sqlite.SQLiteConstraintException) { }
            assertSchema(db)
        } finally { db.close() }
    }

    @Test fun importPreservesCountersAndPendingAndInvalidatesCountsOnlyWhenDateChanges(): Unit = runBlocking {
        val db = database()
        try {
            val station = detail().station
            db.coffeeStationDao().insert(detail().toStationEntity())
            val dao = db.stationMaintenanceDao()
            dao.upsertAll(station.toMaintenanceEntities().map { it.copy(salesAfterService = 500L, salesUpdatedAt = "counted") })
            db.withTransaction { dao.importMaintenance(7, dao.getByStationId(7).map { it.copy(updatedAt="2026-11-01T10:00:00Z") }) }
            assertTrue(dao.getByStationId(7).all { it.salesAfterService == 500L })
            val pending = dao.get(7, "MIXERS")!!.copy(lastServiceDate = "10.11.2026", salesAfterService = 0L,
                isSynced = false, updatedAt = "pending")
            dao.upsert(pending)
            db.withTransaction { dao.importMaintenance(7, dao.getByStationId(7).map { row ->
                when(row.nodeType) {
                    "MIXERS" -> row.copy(lastServiceDate="09.11.2026",updatedAt="2026-11-16T10:00:00Z",isSynced=true)
                    "BREW_UNIT" -> row.copy(lastServiceDate="11.11.2026",salesAfterService=null,salesUpdatedAt="",updatedAt="2026-11-16T10:00:00Z")
                    else -> row
                }
            }) }
            assertEquals(pending, dao.get(7, "MIXERS"))
            assertNull(dao.get(7, "BREW_UNIT")!!.salesAfterService)
            assertEquals(500L, dao.get(7, "EXHAUST")!!.salesAfterService)
            dao.acknowledge(7, "MIXERS", "older", pending.lastServiceDate)
            assertFalse(dao.get(7, "MIXERS")!!.isSynced)
            dao.acknowledge(7, "MIXERS", pending.updatedAt, pending.lastServiceDate)
            assertTrue(dao.get(7, "MIXERS")!!.isSynced)
        } finally { db.close() }
    }

    @Test fun completionResetsOnlyMixersAndKeepsOperatorSales(): Unit = runBlocking {
        val db = database()
        try {
            db.coffeeStationDao().insert(detail().toStationEntity())
            val dao = db.stationMaintenanceDao()
            dao.upsertAll(detail().station.toMaintenanceEntities().map { it.copy(salesAfterService = 777L) })
            val repository = SupplierServiceCompletionRepository(db)
            repository.completeLocally("manager", 7, UserRole.MANAGER, Instant.parse("2026-11-16T10:00:00Z"))
            assertEquals(0L, dao.get(7, "MIXERS")!!.salesAfterService)
            assertTrue(dao.getByStationId(7).filter { it.nodeType != "MIXERS" }.all { it.salesAfterService == 777L })
            assertEquals(42, db.coffeeStationDao().getById(7)!!.salesAfterService)
        } finally { db.close() }
    }

    private fun assertSchema(db: AppDatabase) {
        val contract = JSONObject(instrumentation.context.assets.open("contracts/station-maintenance-schema.json")
            .bufferedReader().use { it.readText() })
        val expected = contract.getJSONArray("columns")
        db.openHelper.writableDatabase.query("PRAGMA table_info(station_maintenance)").use { cursor ->
            assertEquals(expected.length(), cursor.count)
            while (cursor.moveToNext()) {
                val column = expected.getJSONObject(cursor.position)
                assertEquals(column.getString("name"), cursor.getString(cursor.getColumnIndexOrThrow("name")))
                assertEquals(column.getString("type"), cursor.getString(cursor.getColumnIndexOrThrow("type")))
                assertEquals(column.getBoolean("notNull"), cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1)
                assertEquals(column.getInt("primaryKey"), cursor.getInt(cursor.getColumnIndexOrThrow("pk")))
            }
        }
        val fk = contract.getJSONObject("foreignKey")
        db.openHelper.writableDatabase.query("PRAGMA foreign_key_list(station_maintenance)").use { cursor ->
            assertTrue(cursor.moveToFirst())
            for (key in listOf("table", "from", "to")) assertEquals(fk.getString(key), cursor.getString(cursor.getColumnIndexOrThrow(key)))
            assertEquals(fk.getString("onDelete"), cursor.getString(cursor.getColumnIndexOrThrow("on_delete")))
            assertFalse(cursor.moveToNext())
        }
    }
}
