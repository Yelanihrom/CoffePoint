package ru.coffeepoint.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object StationAttentionScheduler {

    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val request = PeriodicWorkRequestBuilder<StationAttentionWorker>(
            1,
            TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

        // Remove the previous periodic schedule after registering the replacement.
        workManager.cancelUniqueWork(LEGACY_WORK_NAME)
    }

    private const val WORK_NAME = "station_attention_worker"
    private const val LEGACY_WORK_NAME = "sales_refresh_worker"
}
