package ru.coffeepoint.core.sync.google.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.WarehouseStockRepository

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

        StationRepository(applicationContext).syncPendingGoogleSheets()
        WarehouseStockRepository(applicationContext).syncPendingGoogleSheets()

        Log.d(
            TAG,
            "Google Sheets sync worker finished"
        )

        return Result.success()
    }

    private companion object {
        const val TAG = "GoogleSheetsSync"
    }

}
