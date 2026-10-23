package ru.coffeepoint.feature.service.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.service.model.SupplyServiceChecklistItem
import ru.coffeepoint.feature.service.model.buildSupplyServiceChecklist
import ru.coffeepoint.feature.service.model.canFinishSupplyService
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SupplyServiceStep2ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    var checklist by mutableStateOf<List<SupplyServiceChecklistItem>>(emptyList())
        private set

    private var isLoaded = false
    private var isLoading = false

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun loadStation(stationId: Int) {

        if (isLoaded || isLoading) return
        isLoading = true

        viewModelScope.launch {
            try {
                val detail = repository.getStation(stationId) ?: return@launch
                checklist = buildSupplyServiceChecklist(detail.station)
                isLoaded = true
            } finally {
                isLoading = false
            }
        }

    }

    fun toggleChecklist(index: Int, checked: Boolean) {
        checklist = checklist.toMutableList().also { items ->
            val current = items[index]
            items[index] = current.copy(
                checklistItem = current.checklistItem.copy(
                    checked = checked
                )
            )
        }
    }

    fun canFinish(): Boolean {
        return checklist.canFinishSupplyService()
    }

    fun finishSupplyService(
        stationId: Int,
        onFinished: () -> Unit
    ) {

        if (isSaving || !canFinish()) return

        viewModelScope.launch {
            isSaving = true
            showSyncError = false

            val serviceDate = SimpleDateFormat(
                "dd.MM.yyyy",
                Locale.getDefault()
            ).format(Date())

            val synced = repository.updateSupplyOperatorServiceDates(
                stationId = stationId,
                serviceDate = serviceDate,
                updateMixers = checklist.any { item ->
                    item.node == TechServiceNode.MIXERS
                },
                updateLiquidWasteContainer = checklist.any { item ->
                    item.node == TechServiceNode.LIQUID_WASTE_CONTAINER
                }
            )

            isSaving = false

            if (synced) {
                onFinished()
            } else {
                syncErrorMessage = repository.getStation(stationId)
                    ?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }
        }

    }

    fun retrySync(
        stationId: Int,
        onFinished: () -> Unit
    ) {

        if (isSaving) return

        viewModelScope.launch {
            isSaving = true
            showSyncError = false

            val synced = repository.syncStationNow(stationId)

            isSaving = false

            if (synced) {
                onFinished()
            } else {
                syncErrorMessage = repository.getStation(stationId)
                    ?.syncErrorDialogMessage()
                    ?: "Проверьте подключение к интернету или VPN"
                showSyncError = true
            }
        }

    }

    fun dismissSyncError() {
        showSyncError = false
    }

}
