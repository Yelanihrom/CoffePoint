package ru.coffeepoint.core.sync.google.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object GoogleSheetsSyncScheduler {

    fun schedule(
        context: Context
    ) {

        val request = PeriodicWorkRequestBuilder<GoogleSheetsSyncWorker>(
            2,
            TimeUnit.HOURS
        )
            .setConstraints(networkConstraints())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

    }

    fun enqueueSync(
        context: Context
    ) {

        val request = OneTimeWorkRequestBuilder<GoogleSheetsSyncWorker>()
            .setConstraints(networkConstraints())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        Log.d(
            TAG,
            "Google Sheets one-time sync enqueued"
        )

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )

    }

    private fun networkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }

    private const val PERIODIC_WORK_NAME = "google_sheets_sync_worker"
    private const val ONE_TIME_WORK_NAME = "google_sheets_sync_now"
    private const val TAG = "GoogleSheetsSync"

}
