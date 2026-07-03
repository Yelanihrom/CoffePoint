package ru.coffeepoint.feature.station.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.station.model.StationDetail

class StationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    var station by mutableStateOf<StationDetail?>(null)
        private set

    fun loadStation(
        stationId: Int
    ) {

        viewModelScope.launch {

            station = repository.getStation(stationId)

        }

    }

    fun deleteStation(
        onSuccess: () -> Unit
    ) {

        val currentStation = station ?: return

        viewModelScope.launch {

            repository.deleteStation(
                currentStation.station.id
            )

            onSuccess()

        }

    }

}