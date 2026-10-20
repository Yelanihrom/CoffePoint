package ru.coffeepoint.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.notification.StationAttentionNotification
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult

class StationAttentionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            runImportAndNotifications()
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Log.e(TAG, "Station data import or notification scan failed", error)
            Result.retry()
        }
    }

    private suspend fun runImportAndNotifications(): Result {
        val stationRepository = StationRepository(applicationContext)
        val warehouseStockRepository = WarehouseStockRepository(applicationContext)

        when (val importResult = importGoogleSheets(stationRepository)) {
            GoogleSheetsImportResult.Success -> Unit
            GoogleSheetsImportResult.SkippedPendingChanges -> {
                Log.w(TAG, "Station import deferred: local changes are still pending")
                return Result.retry()
            }
            GoogleSheetsImportResult.Empty -> {
                Log.w(TAG, "Station import returned no station rows")
                return Result.retry()
            }
            is GoogleSheetsImportResult.Error -> {
                Log.e(TAG, "Station import failed", importResult.cause)
                return Result.retry()
            }
        }

        val stations = stationRepository.getStations()
        val notification = StationAttentionNotification(applicationContext)

        notification.showSupplyAttentionIfNeeded(stations)
        notification.showWarehouseStockAttentionIfNeeded(warehouseStockRepository.getItems())
        notification.showRefillAttentionIfNeeded(stations)

        return Result.success()
    }

    private suspend fun importGoogleSheets(
        stationRepository: StationRepository
    ): GoogleSheetsImportResult {
        val firstResult = stationRepository.importFromGoogleSheetsResult()
        if (firstResult != GoogleSheetsImportResult.SkippedPendingChanges) return firstResult

        // Only clear conflicts already reported by Google Sheets; pending writes belong to GoogleSheetsSyncWorker.
        stationRepository.resolveConflictBlocksForImport()
        return stationRepository.importFromGoogleSheetsResult()
    }

    private companion object {
        const val TAG = "StationAttentionWorker"
    }
}
