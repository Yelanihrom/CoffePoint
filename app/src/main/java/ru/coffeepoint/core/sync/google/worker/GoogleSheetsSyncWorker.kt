package ru.coffeepoint.core.sync.google.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.coffeepoint.core.data.repository.EmployeeSyncRepository
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType

class GoogleSheetsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(
    context,
    params
) {

    override suspend fun doWork(): Result {
        Log.d(
            TAG,
            "Google Sheets sync worker started"
        )

        return try {
            val results = listOf(
                "stations" to StationRepository(applicationContext)
                    .syncPendingGoogleSheets(),
                "warehouse" to WarehouseStockRepository(applicationContext)
                    .syncPendingGoogleSheets(),
                "employees" to EmployeeSyncRepository(applicationContext)
                    .syncEmployeeData()
            )

            results.forEach { (entity, result) ->
                logResult(entity, result)
            }

            if (results.any { (_, result) -> result.isRetryableConnectionFailure() }) {
                Log.w(TAG, "Google Sheets sync will retry after a connection failure")
                return Result.retry()
            }

            Log.d(TAG, "Google Sheets sync worker finished")
            Result.success()
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "Google Sheets sync worker failed unexpectedly", error)
            Result.retry()
        }
    }

    private fun logResult(
        entity: String,
        result: GoogleSheetsEntitySyncResult
    ) {
        if (result.synced) return

        Log.w(
            TAG,
            "Google Sheets $entity sync remains pending (${result.errorType})"
        )
    }

    private fun GoogleSheetsEntitySyncResult.isRetryableConnectionFailure(): Boolean {
        return !synced && errorType == GoogleSheetsSyncErrorType.CONNECTION
    }

    private companion object {
        const val TAG = "GoogleSheetsSync"
    }

}
