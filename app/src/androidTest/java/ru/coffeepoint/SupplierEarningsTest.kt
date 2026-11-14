package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.preferences.MonthlyEarningsPreferences
import ru.coffeepoint.core.data.repository.MonthlyEarningsRepository
import java.util.UUID

class SupplierEarningsTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun earning(id: String, employeeId: Int, local: Boolean) = EmployeeTransactionEntity(
        id, employeeId, "EARNING", "2026-11-10T21:30:00Z", null, null, null,
        2000, "", "", "", isSynced = false, isLocalOnly = local,
        sourceType = if (local) "SUPPLY_SERVICE" else null,
        sourceId = if (local) "visit" else null
    )

    @Test fun supplierBalancePaymentsAndMoscowPeriodAreSeparateFromOperator(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val scope = "test-${UUID.randomUUID()}"
        try {
            db.employeeDao().insert(EmployeeEntity(1, "Максим", "OPERATOR", "2026-09-13", true, "", ""))
            db.employeeDao().insert(EmployeeEntity(2, "Денис", "SUPPLY_OPERATOR", "", true, "", "", isLocalOnly = true))
            db.employeeTransactionDao().insert(earning("operator", 1, false))
            db.employeeTransactionDao().insert(earning("supplier", 2, true))
            val repository = MonthlyEarningsRepository(context, UserRole.SUPPLY_OPERATOR, db,
                MonthlyEarningsPreferences(context, scope), enqueueSync = {})
            repository.refreshLocalState()
            assertEquals("Денис", repository.getState().employeeName)
            assertEquals(2000, repository.getState().settlementAmount)
            assertEquals("—", repository.getState().workStartDateText)
            assertTrue(repository.getOperationsForPeriod("10.11.2026", "10.11.2026").isEmpty())
            assertEquals(listOf("supplier"), repository.getOperationsForPeriod("11.11.2026", "11.11.2026").map { it.id })
            repository.addPayment(500)
            assertEquals(1500, repository.getState().settlementAmount)
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(1, "EARNING"))
            assertTrue(db.employeeTransactionDao().getByEmployee(2).all { it.isLocalOnly })
            assertEquals(listOf("operator"), db.employeeTransactionDao().getUnsynced().map { it.id })
        } finally {
            db.close()
            context.getSharedPreferences("monthly_earnings_$scope", 0).edit().clear().commit()
        }
    }

    @Test fun remoteReplacementPreservesLocalLedgerAndRejectsConflicts(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            db.employeeDao().insert(EmployeeEntity(2, "Денис", "SUPPLY_OPERATOR", "", true, "", "", isLocalOnly = true))
            val local = earning("local", 2, true)
            db.employeeTransactionDao().insert(local)
            db.employeeDao().insert(EmployeeEntity(1, "Максим", "OPERATOR", "2026-09-13", true, "", ""))
            db.employeeTransactionDao().insert(earning("remote", 1, false).copy(isSynced = true))
            db.employeeTransactionDao().deleteRemoteRecords()
            assertEquals(listOf(local), db.employeeTransactionDao().getAll())
            for (conflict in listOf(local.copy(isLocalOnly = false), earning("other", 2, false))) {
                var rejected = false
                try { db.employeeTransactionDao().insertRemoteAll(listOf(conflict)) }
                catch (_: IllegalStateException) { rejected = true }
                assertTrue(rejected)
                assertEquals(listOf(local), db.employeeTransactionDao().getAll())
            }
            var duplicateRejected = false
            try { db.employeeTransactionDao().insertLocalEarning(local.copy(id = "duplicate")) }
            catch (_: android.database.sqlite.SQLiteConstraintException) { duplicateRejected = true }
            assertTrue(duplicateRejected)
        } finally { db.close() }
    }
}
