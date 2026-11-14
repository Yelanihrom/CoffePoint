package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.Assume.assumeTrue
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.LocalTestEmployees
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeEntity

class LocalTestEmployeesDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun testApplicationDatabaseContainsRequestedLocalSupplier(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        val suppliers = DatabaseProvider.getDatabase(context).employeeDao().getActiveByRole("SUPPLY_OPERATOR")
        val supplier = suppliers.single { it.name == "Денис" }
        // An existing TEST install may already have adopted the server employee.
        assertEquals("SUPPLY_OPERATOR", supplier.role)
        assertTrue(supplier.id >= 2)
    }

    @Test fun supplierIsLocalOnlyAndRepeatedSeedingPreservesBothEmployees(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val operator = EmployeeEntity(1, "Максим", "OPERATOR", "2026-09-13", true,
                "2026-09-13T00:00:00Z", "2026-09-13T00:00:00Z", isSynced = false)
            db.employeeDao().insert(operator)
            LocalTestEmployees.ensureSupplier(db.openHelper.writableDatabase)
            val supplier = db.employeeDao().getActiveByRole("SUPPLY_OPERATOR").single()
            assertEquals("Денис", supplier.name)
            assertEquals(2, supplier.id)
            assertTrue(supplier.isLocalOnly)
            assertFalse(supplier.isSynced)
            assertEquals("", supplier.workStartDate)
            assertEquals(listOf(operator), db.employeeDao().getUnsynced())
            LocalTestEmployees.ensureSupplier(db.openHelper.writableDatabase)
            assertEquals(listOf(supplier), db.employeeDao().getActiveByRole("SUPPLY_OPERATOR"))
            assertEquals(operator, db.employeeDao().getById(1))
        } finally { db.close() }
    }

    @Test fun remoteImportCannotReplaceLocalSupplierButOtherEmployeesStillImport(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            LocalTestEmployees.ensureSupplier(db.openHelper.writableDatabase)
            val supplier = db.employeeDao().getLocalOnly().single()
            var rejected = false
            try {
                db.employeeDao().insertRemoteAll(listOf(supplier.copy(name = "Remote", isLocalOnly = false)))
            } catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertEquals(supplier, db.employeeDao().getById(supplier.id))
            val operator = EmployeeEntity(1, "Максим", "OPERATOR", "2026-09-13", true, "", "")
            db.employeeDao().insertRemoteAll(listOf(operator))
            assertEquals(operator, db.employeeDao().getById(1))
        } finally { db.close() }
    }
}
