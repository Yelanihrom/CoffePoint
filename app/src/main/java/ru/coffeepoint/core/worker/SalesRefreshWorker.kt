package ru.coffeepoint.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.coffeepoint.core.data.repository.WarehouseStockRepository
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.SalesRefreshRepository
import ru.coffeepoint.core.data.repository.SalesRefreshResult
import ru.coffeepoint.core.notification.SalesRefreshNotification

class SalesRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(
    context,
    params
) {

    override suspend fun doWork(): Result {

        val stationRepository = StationRepository(
            applicationContext
        )

        val googleSheetsImportResult = importGoogleSheets(
            stationRepository = stationRepository
        )

        if (googleSheetsImportResult != GoogleSheetsImportResult.Success) {
            return Result.success()
        }

        val repository = SalesRefreshRepository(
            applicationContext
        )

        val notification = SalesRefreshNotification(
            applicationContext
        )

        return when (repository.refreshSales()) {

            is SalesRefreshResult.Success -> {

                val stations = stationRepository.getStations()

                notification.showAttentionIfNeeded(stations)

                Result.success()

            }

            SalesRefreshResult.NoInternet,
            SalesRefreshResult.ConnectionError -> Result.success()

        }

    }

    private suspend fun importGoogleSheets(
        stationRepository: StationRepository
    ): GoogleSheetsImportResult {
        val firstResult = stationRepository.importFromGoogleSheetsResult()

        if (firstResult != GoogleSheetsImportResult.SkippedPendingChanges) {
            return firstResult
        }

        stationRepository.syncPendingGoogleSheets()
        WarehouseStockRepository(applicationContext).syncPendingGoogleSheets()
        stationRepository.resolveConflictBlocksForImport()

        return stationRepository.importFromGoogleSheetsResult()
    }

}
