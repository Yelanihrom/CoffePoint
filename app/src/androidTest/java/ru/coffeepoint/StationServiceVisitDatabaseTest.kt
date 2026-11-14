package ru.coffeepoint

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.local.entity.StationServiceVisitEntity
import java.util.UUID

class StationServiceVisitDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun visit(id: String = "visit-1") = StationServiceVisitEntity(
        id = id, employeeId = 2, stationId = 7,
        completedAt = "2026-11-10T10:02:00.000Z", updatedAt = "2026-11-10T10:02:00.000Z"
    )

    @Test fun duplicateVisitDoesNotReplaceOriginalAndQueriesAreScoped(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.stationServiceVisitDao()
            val original = visit()
            assertTrue(dao.insertIfAbsent(original) != -1L)
            assertEquals(-1L, dao.insertIfAbsent(original.copy(employeeId = 99)))
            assertEquals(original, dao.getById(original.id))
            dao.insertIfAbsent(visit("visit-2").copy(employeeId = 3, stationId = 8, isSynced = true))
            assertEquals(listOf(original), dao.getByEmployee(2))
            assertEquals(listOf(original), dao.getByStation(7))
            assertEquals(listOf(original), dao.getUnsynced())
            assertNull(original.createdAt)
        } finally { db.close() }
    }

    @Test fun migrationAddsEmptyVisitTableAndPreservesExistingData(): Unit = runBlocking {
        val name = "visit-migration-${UUID.randomUUID()}.db"
        val employee = EmployeeEntity(2, "Supplier", "SUPPLY_OPERATOR", "2026-11-01", true,
            "2026-11-01T00:00:00Z", "2026-11-01T00:00:00Z")
        val oldPayment = EmployeeTransactionEntity("old-payment", 2, "PAYMENT", "2026-11-01", null, null,
            null, 1000, "", "2026-11-01", "2026-11-01")
        try {
            val original = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                original.employeeDao().insert(employee)
                original.employeeTransactionDao().insert(oldPayment)
            } finally { original.close() }
            // Recreate the pre-visit schema, before the local-only employee column.
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null,
                SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE station_service_visits")
                it.execSQL("ALTER TABLE employees DROP COLUMN isLocalOnly")
                it.execSQL("DROP INDEX index_employees_transactions_sourceType_sourceId")
                it.execSQL("ALTER TABLE employees_transactions DROP COLUMN sourceType")
                it.execSQL("ALTER TABLE employees_transactions DROP COLUMN sourceId")
                it.execSQL("ALTER TABLE employees_transactions DROP COLUMN isLocalOnly")
                it.version = 35
            }
            val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(DatabaseProvider.MIGRATION_35_36, DatabaseProvider.MIGRATION_36_37,
                    DatabaseProvider.MIGRATION_37_38).build()
            try {
                assertEquals(employee, migrated.employeeDao().getById(2))
                assertEquals(listOf(oldPayment), migrated.employeeTransactionDao().getAll())
                assertTrue(migrated.stationServiceVisitDao().getUnsynced().isEmpty())
                migrated.stationServiceVisitDao().insertIfAbsent(visit())
                assertEquals(visit(), migrated.stationServiceVisitDao().getById("visit-1"))
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
