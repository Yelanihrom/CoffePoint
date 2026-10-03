package ru.coffeepoint.feature.station.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.SupplyCatalogRepository
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    private val supplyCatalogRepository = SupplyCatalogRepository(application)

    var station by mutableStateOf<StationDetail?>(null)
        private set

    var supplyCatalogItems by mutableStateOf<List<SupplyCatalogEntity>>(emptyList())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun loadStation(
        stationId: Int
    ) {

        viewModelScope.launch {

            station = repository.getStation(stationId)
            supplyCatalogItems = supplyCatalogRepository.getItems()

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

    fun updateRentPaidUntil(
        date: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {

        val currentStation = station ?: return

        if (!isTodayOrFuture(date)) {
            onError()
            return
        }

        viewModelScope.launch {

            isSaving = true
            showSyncError = false

            val synced = repository.updateRentPaidUntil(
                stationId = currentStation.station.id,
                rentPaidUntil = date
            )

            station = repository.getStation(
                currentStation.station.id
            )

            isSaving = false

            onSuccess()

            if (!synced) {
                syncErrorMessage = station?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }

        }

    }

    fun updateTechServiceDate(
        node: TechServiceNode,
        date: String,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {

        val currentStation = station ?: return

        if (!isTodayOrPast(date)) {
            onError()
            return
        }

        viewModelScope.launch {

            isSaving = true
            showSyncError = false

            val synced = repository.updateTechServiceDate(
                stationId = currentStation.station.id,
                node = node,
                serviceDate = date
            )

            station = repository.getStation(
                currentStation.station.id
            )

            isSaving = false

            onSuccess()

            if (!synced) {
                syncErrorMessage = station?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }

        }

    }

    fun retrySync() {

        if (isSaving) return

        val currentStation = station ?: return

        viewModelScope.launch {
            isSaving = true
            showSyncError = false

            val synced = repository.syncStationNow(
                currentStation.station.id
            )

            station = repository.getStation(
                currentStation.station.id
            )

            isSaving = false

            if (!synced) {
                syncErrorMessage = station?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }
        }

    }

    fun dismissSyncError() {
        showSyncError = false

    }

    private fun isTodayOrFuture(date: String): Boolean {

        val formatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        )

        val selected = formatter.parse(date) ?: return false

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        return !selected.before(today)

    }

    private fun isTodayOrPast(date: String): Boolean {

        val formatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        )

        val selected = formatter.parse(date) ?: return false

        val tomorrow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, 1)
        }.time

        return selected.before(tomorrow)

    }

}
