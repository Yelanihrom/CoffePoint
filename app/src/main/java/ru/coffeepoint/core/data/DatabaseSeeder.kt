package ru.coffeepoint.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.InitialStations

object DatabaseSeeder {

    suspend fun seed(context: Context) = withContext(Dispatchers.IO) {

        val repository = StationRepository(context)

        val station = repository.getStation(1)

        if (station != null) return@withContext

        InitialStations.forEach {

            repository.saveStation(it)

        }

    }

}