package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.mapper.*
import ru.coffeepoint.core.data.repository.LocalSupplierUnavailableException
import ru.coffeepoint.core.data.repository.SupplierServiceCompletionRepository
import ru.coffeepoint.feature.station.model.*
import java.time.Instant

class SupplierServiceCompletionTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val completedAt = Instant.parse("2026-11-10T21:30:00Z")
    private fun database() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    private suspend fun seed(db: AppDatabase, supplier: Boolean = true) {
        val station = StationDetail(
            CoffeeStation(id = 7, name = "Station", lastServiceDate = "09.11.2026 10:02",
                salesAfterService = 42, mixersServiceDate = "27.09.2026", brewUnitServiceDate = "01.11.2026"),
            StationInventory(4, 3, 2, 1, 2, 3, 2, 1),
            StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10)
        )
        db.coffeeStationDao().insert(station.toStationEntity())
        db.stationInventoryDao().insert(station.toInventoryEntity())
        db.stationSettingsDao().insert(station.toSettingsEntity())
        db.warehouseStockDao().insert(WarehouseStockEntity("cups", 20))
        db.employeeDao().insert(EmployeeEntity(1, "Максим", "OPERATOR", "2026-09-13", true, "", ""))
        if (supplier) db.employeeDao().insert(EmployeeEntity(2, "Денис", "SUPPLY_OPERATOR", "", true,
            "", "", isSynced = false, isLocalOnly = true))
    }

    @Test fun completionRecordsSupplierAndOnlyChangesMixerFields(): Unit = runBlocking {
        val db = database()
        try {
            seed(db)
            val before = db.coffeeStationDao().getById(7)!!
            val inventory = db.stationInventoryDao().getByStationId(7)
            val settings = db.stationSettingsDao().getByStationId(7)
            val warehouse = db.warehouseStockDao().getAll()
            val visit = SupplierServiceCompletionRepository(db, true)
                .completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt)!!
            assertEquals(2, visit.employeeId)
            assertEquals(completedAt.toString(), visit.completedAt)
            assertNull(visit.createdAt)
            assertFalse(visit.isSynced)
            assertFalse(visit.syncError)
            assertEquals(before.copy(mixersServiceDate = "11.11.2026", isSynced = false,
                pendingSupplyServiceOnly = true, pendingMixersServiceDate = true), db.coffeeStationDao().getById(7))
            assertEquals(inventory, db.stationInventoryDao().getByStationId(7))
            assertEquals(settings, db.stationSettingsDao().getByStationId(7))
            assertEquals(warehouse, db.warehouseStockDao().getAll())
            val earning = db.employeeTransactionDao().getAll().single()
            assertEquals(earning.id, java.util.UUID.fromString(earning.id).toString())
            assertEquals(2, earning.employeeId)
            assertEquals("EARNING", earning.type)
            assertEquals(2000, earning.amount)
            assertEquals("SUPPLY_SERVICE", earning.sourceType)
            assertEquals(visit.id, earning.sourceId)
            assertEquals(visit.completedAt, earning.operationDate)
            assertTrue(earning.isLocalOnly)
            assertFalse(earning.isSynced)
            assertTrue(db.employeeTransactionDao().getUnsynced().isEmpty())
        } finally { db.close() }
    }

    @Test fun concurrentRetryAndNewRepositoryReuseThePersistedVisit(): Unit = runBlocking {
        val db = database()
        try {
            seed(db)
            val first = async { SupplierServiceCompletionRepository(db, true)
                .completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt) }
            val second = async { SupplierServiceCompletionRepository(db, true)
                .completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt) }
            assertEquals(first.await(), second.await())
            val original = db.stationServiceVisitDao().getById("visit-1")!!
            val originalEarning = db.employeeTransactionDao().getAll().single()
            val station = db.coffeeStationDao().getById(7)!!.copy(mixersServiceDate = "12.11.2026")
            db.coffeeStationDao().insert(station)
            val repeated = SupplierServiceCompletionRepository(db, true).completeLocally(
                "visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt.plusSeconds(86400))
            assertEquals(original, repeated)
            assertEquals(station, db.coffeeStationDao().getById(7))
            assertEquals(1, db.stationServiceVisitDao().getByEmployee(2).size)
            assertEquals(listOf(originalEarning), db.employeeTransactionDao().getAll())
        } finally { db.close() }
    }

    @Test fun separateCompletionsOnSameDayCreateSeparateVisits(): Unit = runBlocking {
        val db = database()
        try {
            seed(db)
            val repository = SupplierServiceCompletionRepository(db, true)
            repository.completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt)
            repository.completeLocally("visit-2", 7, UserRole.SUPPLY_OPERATOR, completedAt.plusSeconds(60))
            assertEquals(2, db.stationServiceVisitDao().getByEmployee(2).size)
            assertEquals(4000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
        } finally { db.close() }
    }

    @Test fun missingOrAmbiguousSupplierDoesNotChangeStation(): Unit = runBlocking {
        val db = database()
        try {
            seed(db, supplier = false)
            val before = db.coffeeStationDao().getById(7)
            for (count in listOf(0, 2)) {
                if (count == 2) for (id in 2..3) db.employeeDao().insert(
                    EmployeeEntity(id, "Supplier $id", "SUPPLY_OPERATOR", "", true, "", "", isLocalOnly = true))
                var rejected = false
                try { SupplierServiceCompletionRepository(db, true)
                    .completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt) }
                catch (_: LocalSupplierUnavailableException) { rejected = true }
                assertTrue(rejected)
                assertEquals(before, db.coffeeStationDao().getById(7))
                assertTrue(db.stationServiceVisitDao().getUnsynced().isEmpty())
                assertTrue(db.employeeTransactionDao().getAll().isEmpty())
            }
        } finally { db.close() }
    }

    @Test fun managerAndExplicitlyDisabledRecordingDoNotCreateVisits(): Unit = runBlocking {
        for ((role, enabled) in listOf(UserRole.MANAGER to true, UserRole.SUPPLY_OPERATOR to false)) {
            val db = database()
            try {
                seed(db, supplier = false)
                assertNull(SupplierServiceCompletionRepository(db, enabled)
                    .completeLocally("visit-1", 7, role, completedAt))
                assertTrue(db.stationServiceVisitDao().getUnsynced().isEmpty())
                assertTrue(db.employeeTransactionDao().getAll().isEmpty())
                assertEquals("11.11.2026", db.coffeeStationDao().getById(7)!!.mixersServiceDate)
            } finally { db.close() }
        }
    }

    @Test fun productionCompletionRequiresRemoteSupplierAndRetryDoesNotDuplicateMoney(): Unit = runBlocking {
        val db = database()
        try {
            seed(db)
            val before = db.coffeeStationDao().getById(7)
            val repository = SupplierServiceCompletionRepository(db, allowLocalSupplier = false)
            try {
                repository.completeLocally("production-visit", 7, UserRole.SUPPLY_OPERATOR, completedAt)
                fail("Production must not use a local test profile")
            } catch (_: LocalSupplierUnavailableException) { }
            assertEquals(before, db.coffeeStationDao().getById(7))
            assertTrue(db.stationServiceVisitDao().getUnsynced().isEmpty())
            val remote = db.employeeDao().getById(2)!!.copy(isLocalOnly = false, isSynced = true)
            db.employeeDao().insert(remote)
            val first = repository.completeLocally("production-visit", 7, UserRole.SUPPLY_OPERATOR, completedAt)
            assertNotNull(first)
            assertEquals(first, repository.completeLocally("production-visit", 7, UserRole.SUPPLY_OPERATOR,
                completedAt.plusSeconds(60)))
            assertEquals(1, db.stationServiceVisitDao().getByEmployee(2).size)
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
        } finally { db.close() }
    }

    @Test fun invalidRoleStationAndReusedVisitForAnotherStationAreRejected(): Unit = runBlocking {
        val db = database()
        try {
            seed(db)
            val repository = SupplierServiceCompletionRepository(db, true)
            var rejected = false
            try { repository.completeLocally("visit-1", 7, UserRole.OPERATOR, completedAt) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            rejected = false
            try { repository.completeLocally("missing", 999, UserRole.SUPPLY_OPERATOR, completedAt) }
            catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertTrue(db.stationServiceVisitDao().getUnsynced().isEmpty())
            repository.completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt)
            rejected = false
            try { repository.completeLocally("visit-1", 8, UserRole.SUPPLY_OPERATOR, completedAt) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
            assertEquals(1, db.stationServiceVisitDao().getUnsynced().size)
        } finally { db.close() }
    }

    @Test fun earningInsertFailureRollsBackVisitAndMixerDate(): Unit = runBlocking {
        val db = database()
        try {
            seed(db)
            val before = db.coffeeStationDao().getById(7)
            val conflict = EmployeeTransactionEntity(java.util.UUID.randomUUID().toString(), 1, "PAYMENT",
                completedAt.toString(), null, null, null, 500, "", "", "", sourceType = "SUPPLY_SERVICE", sourceId = "visit-1")
            db.employeeTransactionDao().insert(conflict)
            var rejected = false
            try { SupplierServiceCompletionRepository(db, true)
                .completeLocally("visit-1", 7, UserRole.SUPPLY_OPERATOR, completedAt) }
            catch (_: android.database.sqlite.SQLiteConstraintException) { rejected = true }
            assertTrue(rejected)
            assertEquals(before, db.coffeeStationDao().getById(7))
            assertTrue(db.stationServiceVisitDao().getUnsynced().isEmpty())
            assertEquals(listOf(conflict), db.employeeTransactionDao().getAll())
        } finally { db.close() }
    }
}
