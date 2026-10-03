package ru.coffeepoint.core.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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

        val repository = SalesRefreshRepository(
            applicationContext
        )

        val notification = SalesRefreshNotification(
            applicationContext
        )

        return when (repository.refreshSales()) {

            is SalesRefreshResult.Success -> {

                val stationRepository = StationRepository(
                    applicationContext
                )

                val stations = stationRepository.getStations()

                notification.showAttentionIfNeeded(stations)

                Result.success()

            }

            SalesRefreshResult.NoInternet,
            SalesRefreshResult.ConnectionError -> Result.success()

        }

    }

}
