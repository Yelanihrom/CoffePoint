package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeStationAssignmentEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.core.sync.google.EmployeeDataImporter
import ru.coffeepoint.core.sync.google.SupplierVisitSyncService
import ru.coffeepoint.core.data.local.AppDatabase

class EmployeeSyncRepository(
    context: Context,
    private val db: AppDatabase = DatabaseProvider.getDatabase(context),
    private val syncManager: GoogleSheetsSyncManager = GoogleSheetsSyncManager(),
    private val enqueueSync: () -> Unit = { syncManager.enqueueBackgroundSync(context.applicationContext) }
) {

    private val syncStatePreferences = context.applicationContext.getSharedPreferences(
        "google_sheets_sync_state",
        Context.MODE_PRIVATE
    )
    private val employeeDao = db.employeeDao()
    private val assignmentDao = db.employeeStationAssignmentDao()
    private val transactionDao = db.employeeTransactionDao()
    suspend fun syncSupplierVisits(): GoogleSheetsEntitySyncResult =
        SupplierVisitSyncService(db, syncManager).syncPending()

    suspend fun syncEmployeesAndAssignments(): GoogleSheetsEntitySyncResult {
        val employees = employeeDao.getUnsynced()
        val assignments = assignmentDao.getUnsynced()

        if (employees.isEmpty() && assignments.isEmpty()) {
            return GoogleSheetsEntitySyncResult.success()
        }

        val result = syncManager.syncEmployeesAndAssignments(
            employees = employees,
            assignments = assignments
        )

        updateEmployeesSyncStatus(
            employees = employees,
            result = result
        )
        updateAssignmentsSyncStatus(
            assignments = assignments,
            result = result
        )

        return result
    }

    suspend fun syncEmployeeTransactions(): GoogleSheetsEntitySyncResult {
        val transactions = transactionDao.getUnsynced()

        if (transactions.isEmpty()) {
            return GoogleSheetsEntitySyncResult.success()
        }

        val result = syncManager.syncEmployeeTransactions(
            transactions = transactions
        )

        updateTransactionsSyncStatus(
            transactions = transactions,
            result = result
        )

        return result
    }

    suspend fun syncEmployeeData(): GoogleSheetsEntitySyncResult {
        val visitsResult = syncSupplierVisits()
        val employeesResult = syncEmployeesAndAssignments()
        val transactionsResult = syncEmployeeTransactions()

        val failedResults = listOf(visitsResult, employeesResult, transactionsResult)
            .filterNot { result -> result.synced }

        if (failedResults.isEmpty()) return GoogleSheetsEntitySyncResult.success()

        val errorType = when {
            failedResults.any { result ->
                result.errorType == GoogleSheetsSyncErrorType.CONNECTION
            } -> GoogleSheetsSyncErrorType.CONNECTION
            failedResults.any { result ->
                result.errorType == GoogleSheetsSyncErrorType.CONFLICT
            } -> GoogleSheetsSyncErrorType.CONFLICT
            else -> failedResults.first().errorType
        }

        return GoogleSheetsEntitySyncResult.error(errorType)
    }

    suspend fun refreshEmployeeDataFromGoogleSheets(): Boolean {
        val shouldReconcileTransactions = !syncStatePreferences.getBoolean(
            TRANSACTIONS_RECONCILED_KEY,
            false
        )
        val remoteData = try {
            syncManager.loadFullSyncBundle(
                includeStations = false,
                includeSettings = false,
                includeSupplyCatalog = false,
                includeWarehouseStock = false,
                includeEmployeeData = true
            )
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) { throw cancellation }
        catch (_: Exception) {
            return false
        }

        val reconciled = try {
            EmployeeDataImporter(db).import(remoteData, shouldReconcileTransactions)
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) { throw cancellation }
        catch (_: Exception) {
            return false
        }

        if (reconciled) {
            syncStatePreferences.edit()
                .putBoolean(TRANSACTIONS_RECONCILED_KEY, true)
                .apply()
        }

        if (db.stationServiceVisitDao().getUnsynced().isNotEmpty() || transactionDao.getUnsynced().isNotEmpty()) {
            enqueueSync()
        }

        return true
    }

    private suspend fun updateEmployeesSyncStatus(
        employees: List<EmployeeEntity>,
        result: GoogleSheetsEntitySyncResult
    ) {
        employees.forEach { employee ->
            employeeDao.updateSyncStatus(
                id = employee.id,
                isSynced = result.synced,
                syncError = !result.synced,
                syncErrorType = if (result.synced) {
                    GoogleSheetsSyncErrorType.NONE.storageValue
                } else {
                    result.errorType.storageValue
                }
            )
        }
    }

    private suspend fun updateAssignmentsSyncStatus(
        assignments: List<EmployeeStationAssignmentEntity>,
        result: GoogleSheetsEntitySyncResult
    ) {
        assignments.forEach { assignment ->
            assignmentDao.updateSyncStatus(
                id = assignment.id,
                isSynced = result.synced,
                syncError = !result.synced,
                syncErrorType = if (result.synced) {
                    GoogleSheetsSyncErrorType.NONE.storageValue
                } else {
                    result.errorType.storageValue
                }
            )
        }
    }

    private suspend fun updateTransactionsSyncStatus(
        transactions: List<EmployeeTransactionEntity>,
        result: GoogleSheetsEntitySyncResult
    ) {
        transactions.forEach { transaction ->
            transactionDao.updateSyncStatus(
                id = transaction.id,
                isSynced = result.synced,
                syncError = !result.synced,
                syncErrorType = if (result.synced) {
                    GoogleSheetsSyncErrorType.NONE.storageValue
                } else {
                    result.errorType.storageValue
                }
            )
        }
    }

    private companion object {
        const val TRANSACTIONS_RECONCILED_KEY = "employees_transactions_reconciled_v1"
    }

}
