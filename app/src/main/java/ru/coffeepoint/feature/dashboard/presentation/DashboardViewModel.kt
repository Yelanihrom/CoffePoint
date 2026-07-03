package ru.coffeepoint.feature.dashboard.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.core.data.remote.repository.SalesRepository
import java.text.SimpleDateFormat
import java.util.Locale
import ru.coffeepoint.core.util.toApiDateFormat

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    private val salesRepository = SalesRepository()

    var stations by mutableStateOf<List<StationDetail>>(emptyList())
        private set

    fun loadStations() {


        viewModelScope.launch {

            stations = repository.getStations()

        }

    }

    fun refreshSales() {

        viewModelScope.launch {

            val currentStations = repository.getStations()

            currentStations.forEach { detail ->

                if (detail.station.lastServiceDate.isBlank()) return@forEach

                val terminalId = detail.station.terminalId
                    ?: return@forEach

                val apiDate = toApiDateFormat(detail.station.lastServiceDate)

                val salesByTerminal = salesRepository.loadSalesByTerminal(
                    dateFrom = apiDate
                )

                val salesCount = salesByTerminal[terminalId] ?: 0

                repository.updateSalesAfterService(
                    stationId = detail.station.id,
                    salesAfterService = salesCount
                )

            }

            stations = repository.getStations()

        }

    }

}