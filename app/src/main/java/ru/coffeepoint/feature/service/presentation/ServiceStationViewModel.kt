package ru.coffeepoint.feature.service.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.station.model.StationDetail

class ServiceStationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StationRepository(application)
    private var loading = false
    var station by mutableStateOf<StationDetail?>(null)
        private set

    fun loadStation(stationId: Int) {
        if (station != null || loading) return
        loading = true
        viewModelScope.launch {
            try { station = repository.getStation(stationId) }
            finally { loading = false }
        }
    }
}
