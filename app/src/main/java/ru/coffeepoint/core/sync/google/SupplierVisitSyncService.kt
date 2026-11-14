package ru.coffeepoint.core.sync.google

import kotlinx.coroutines.CancellationException
import ru.coffeepoint.core.data.local.AppDatabase

class SupplierVisitSyncService(
    private val db: AppDatabase,
    private val syncManager: GoogleSheetsSyncManager = GoogleSheetsSyncManager(),
    private val enabled: Boolean = true
) {
    suspend fun syncPending(): GoogleSheetsEntitySyncResult {
        if (!enabled) return GoogleSheetsEntitySyncResult.success()
        try {
            if (db.employeeDao().getLocalOnly().any { it.role == "SUPPLY_OPERATOR" }) {
                val snapshot = syncManager.loadFullSyncBundle(includeStations = false, includeSettings = false,
                    includeSupplyCatalog = false, includeWarehouseStock = false, includeEmployeeData = true)
                EmployeeDataImporter(db).import(snapshot)
            }
            var failure: GoogleSheetsEntitySyncResult? = null
            db.stationServiceVisitDao().getUnsynced().forEach { visit ->
                if (db.employeeDao().getById(visit.employeeId)?.isLocalOnly != false) {
                    db.stationServiceVisitDao().markError(visit.id, GoogleSheetsSyncErrorType.CONFLICT.storageValue)
                    failure = GoogleSheetsEntitySyncResult.error(GoogleSheetsSyncErrorType.CONFLICT)
                    return@forEach
                }
                val localEarning = db.employeeTransactionDao().getBySource("SUPPLY_SERVICE", visit.id)
                val earningId = localEarning?.id?.takeIf { id ->
                    runCatching { java.util.UUID.fromString(id).toString().equals(id, ignoreCase = true) }.getOrDefault(false)
                }
                val outcome = syncManager.completeSupplyVisit(visit, earningId)
                if (outcome.result.synced) {
                    val receipt = checkNotNull(outcome.receipt)
                    EmployeeDataImporter(db).import(GoogleSheetsFullSyncBundle(
                        stationServiceVisits = listOf(receipt.visit), employeeTransactions = listOf(receipt.earning)))
                } else {
                    db.stationServiceVisitDao().markError(visit.id, outcome.result.errorType.storageValue)
                    db.employeeTransactionDao().markVisitEarningError(visit.id,
                        outcome.result.errorType.storageValue)
                    if (failure == null || outcome.result.errorType == GoogleSheetsSyncErrorType.CONNECTION) failure = outcome.result
                }
            }
            return failure ?: GoogleSheetsEntitySyncResult.success()
        } catch (cancellation: CancellationException) { throw cancellation }
        catch (error: Exception) {
            val type = if (error is java.io.IOException) GoogleSheetsSyncErrorType.CONNECTION
                else GoogleSheetsSyncErrorType.UNKNOWN
            db.stationServiceVisitDao().getUnsynced().forEach {
                db.stationServiceVisitDao().markError(it.id, type.storageValue)
            }
            return GoogleSheetsEntitySyncResult.error(type)
        }
    }
}
