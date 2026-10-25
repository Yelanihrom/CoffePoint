package ru.coffeepoint.feature.station.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.core.data.remote.repository.TerminalCommandRepository
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.core.data.repository.SupplyCatalogRepository
import ru.coffeepoint.core.data.preferences.GoogleSheetsImportPreferences
import ru.coffeepoint.core.sync.google.GoogleSheetsImportResult
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ServicePreparationError(
    val lastUpdated: String,
    val hasPendingChanges: Boolean
)

class StationViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)
    private val supplyCatalogRepository = SupplyCatalogRepository(application)
    private val commandRepository = TerminalCommandRepository()

    var station by mutableStateOf<StationDetail?>(null)
        private set

    var supplyCatalogItems by mutableStateOf<List<SupplyCatalogEntity>>(emptyList())
        private set

    var isSaving by mutableStateOf(false)
        private set

    var isPreparingService by mutableStateOf(false)
        private set
    var servicePreparationError by mutableStateOf<ServicePreparationError?>(null)
        private set

    fun prepareService(includeWarehouse: Boolean, onReady: () -> Unit) {
        val current = station ?: return
        if (isPreparingService || isSaving || isCommandSending) return
        viewModelScope.launch {
            isPreparingService = true
            servicePreparationError = null
            try {
                val result = repository.refreshBeforeService(current.station.id, includeWarehouse)
                if (result == GoogleSheetsImportResult.Success) {
                    station = repository.getStation(current.station.id)
                    onReady()
                } else {
                    val lastImport = GoogleSheetsImportPreferences(getApplication()).getLastImportText() ?: "неизвестно"
                    servicePreparationError = ServicePreparationError(
                        lastUpdated = lastImport,
                        hasPendingChanges = result == GoogleSheetsImportResult.SkippedPendingChanges
                    )
                }
            } finally {
                isPreparingService = false
            }
        }
    }

    fun dismissServicePreparation() { servicePreparationError = null }

    var isCommandSending by mutableStateOf(false)
        private set

    var commandMessage by mutableStateOf<String?>(null)
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

    fun clearCommandMessage() {
        commandMessage = null
    }

    fun rebootTerminal() {
        sendTerminalCommand(
            successMessage = "\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430"
        ) { terminalId ->
            commandRepository.rebootTerminal(terminalId)
        }
    }

    fun turnScreenOff() {
        sendTerminalCommand(
            successMessage = "\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f \u044d\u043a\u0440\u0430\u043d\u0430 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430"
        ) { terminalId ->
            commandRepository.turnScreenOff(terminalId)
        }
    }

    fun turnScreenOn() {
        sendTerminalCommand(
            successMessage = "\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f \u044d\u043a\u0440\u0430\u043d\u0430 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430"
        ) { terminalId ->
            commandRepository.turnScreenOn(terminalId)
        }
    }

    private fun sendTerminalCommand(
        successMessage: String,
        action: suspend (Long) -> Unit
    ) {
        val terminalId = station?.station?.terminalId

        if (terminalId == null) {
            commandMessage = "\u0423 \u0441\u0442\u0430\u043d\u0446\u0438\u0438 \u043d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d \u043d\u043e\u043c\u0435\u0440 \u0442\u0435\u0440\u043c\u0438\u043d\u0430\u043b\u0430"
            return
        }

        viewModelScope.launch {
            isCommandSending = true
            commandMessage = "\u041e\u0442\u043f\u0440\u0430\u0432\u043b\u044f\u0435\u043c \u043a\u043e\u043c\u0430\u043d\u0434\u0443..."

            try {
                action(terminalId)
                commandMessage = successMessage
            } catch (_: Exception) {
                commandMessage = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u043a\u043e\u043c\u0430\u043d\u0434\u0443"
            } finally {
                isCommandSending = false
            }
        }
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
