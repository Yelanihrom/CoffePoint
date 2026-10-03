package ru.coffeepoint.feature.service.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.service.model.ServiceChecklistItem
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SupplyServiceStep2ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    var checklist by mutableStateOf<List<SupplyServiceChecklistItem>>(emptyList())
        private set

    var isLoaded by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun loadStation(stationId: Int) {

        if (isLoaded) return

        viewModelScope.launch {
            val detail = repository.getStation(stationId)

            checklist = buildList {
                if (
                    detail != null &&
                    isTechServiceOverdue(
                        date = detail.station.mixersServiceDate,
                        serviceIntervalDays = MIXERS_SERVICE_INTERVAL_DAYS
                    )
                ) {
                    add(
                        SupplyServiceChecklistItem(
                            node = TechServiceNode.MIXERS,
                            checklistItem = ServiceChecklistItem(
                                title = "Обслужить миксеры",
                                description = "1. Снять миксеры\n2. Промыть в горячей воде убрав наросты\n3. Остатки воды убрать тряпкой\n4. Установить в обратном порядке"
                            )
                        )
                    )
                }

                if (
                    detail != null &&
                    isTechServiceOverdue(
                        date = detail.station.liquidWasteContainerServiceDate,
                        serviceIntervalDays = LIQUID_WASTE_CONTAINER_SERVICE_INTERVAL_DAYS
                    )
                ) {
                    add(
                        SupplyServiceChecklistItem(
                            node = TechServiceNode.LIQUID_WASTE_CONTAINER,
                            checklistItem = ServiceChecklistItem(
                                title = "Обслужить контейнер для жидких отходов",
                                description = "1. Открутить силиконовый шланг\n2. Слив закрыть пробкой.\n3. Промыть горячей водой убрав шлак\n4. Установить в обратном порядке"
                            )
                        )
                    )
                }
            }

            isLoaded = true
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
        return checklist.isNotEmpty() &&
            checklist.all { item -> item.checklistItem.checked }
    }

    fun finishSupplyService(
        stationId: Int,
        onFinished: () -> Unit
    ) {

        if (isSaving) return

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

    private fun isTechServiceOverdue(
        date: String,
        serviceIntervalDays: Int
    ): Boolean {

        val serviceDate = parseDateToMillis(date) ?: return false
        val dueDate = serviceDate + TimeUnit.DAYS.toMillis(
            serviceIntervalDays.toLong()
        )

        return dueDate <= todayStartMillis()

    }

    private fun parseDateToMillis(
        date: String
    ): Long? {

        if (date.isBlank()) return null

        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }.parse(date)?.time

    }

    private fun todayStartMillis(): Long {

        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    }

    companion object {
        private const val MIXERS_SERVICE_INTERVAL_DAYS = 30
        private const val LIQUID_WASTE_CONTAINER_SERVICE_INTERVAL_DAYS = 60
    }

}

data class SupplyServiceChecklistItem(
    val node: TechServiceNode,
    val checklistItem: ServiceChecklistItem
)
