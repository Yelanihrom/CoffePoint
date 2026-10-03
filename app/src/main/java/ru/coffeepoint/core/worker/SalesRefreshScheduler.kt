package ru.coffeepoint.core.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SalesRefreshScheduler {

    fun schedule(
        context: Context
    ) {

        val request = PeriodicWorkRequestBuilder<SalesRefreshWorker>(
            1,
            TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )

    }

    private const val WORK_NAME = "sales_refresh_worker"

}
