package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager

class EmployeeSyncRepository(
    context: Context
) {

    private val db = DatabaseProvider.getDatabase(context)
    private val employeeDao = db.employeeDao()
    private val assignmentDao = db.employeeStationAssignmentDao()
    private val transactionDao = db.employeeTransactionDao()
    private val syncManager = GoogleSheetsSyncManager()

    suspend fun syncEmployeesAndAssignments(): GoogleSheetsEntitySyncResult {
        return syncManager.syncEmployeesAndAssignments(
            employees = employeeDao.getAll(),
            assignments = assignmentDao.getAll()
        )
    }

    suspend fun syncEmployeeTransactions(): GoogleSheetsEntitySyncResult {
        return syncManager.syncEmployeeTransactions(
            transactions = transactionDao.getAll()
        )
    }

    suspend fun syncEmployeeData(): GoogleSheetsEntitySyncResult {
        val employeesResult = syncEmployeesAndAssignments()
        val transactionsResult = syncEmployeeTransactions()

        return when {
            !employeesResult.synced -> employeesResult
            !transactionsResult.synced -> transactionsResult
            else -> GoogleSheetsEntitySyncResult.success()
        }
    }

}
