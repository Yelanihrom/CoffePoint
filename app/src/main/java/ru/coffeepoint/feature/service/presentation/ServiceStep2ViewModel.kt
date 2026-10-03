package ru.coffeepoint.feature.service.presentation

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
import ru.coffeepoint.feature.camera.model.PhotoType
import ru.coffeepoint.feature.camera.model.ServicePhoto
import ru.coffeepoint.feature.service.model.ServiceChecklistItem
import ru.coffeepoint.feature.station.model.syncErrorDialogMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ServiceStep2ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StationRepository(application)

    var checklist by mutableStateOf(
        listOf(
            ServiceChecklistItem(
                title = "Почистить аппарат щеткой",
                description = "Очистить остатки ингредиентов вокруг контейнеров, заварочный узел и контейнер для жмыха."
            ),
            ServiceChecklistItem(
                title = "Протереть аппарат внутри и снаружи",
                description = "Сухой тряпкой убрать пыль с контейнеров, корпуса внутри, крышки и дверцы снаружи"
            ),
            ServiceChecklistItem(
                title = "Протереть окно выдачи и столешницу",
                description = "Протереть сначала влажной салфеткой, после чего сухой"
            ),
            ServiceChecklistItem(
                title = "Почистить миксеры и промыть",
                description = "Маленькой щеткой сбить скопившиеся наросты, после чего кнопкой Двойное эспрессо промыть миксеры"
            ),
            ServiceChecklistItem(
                title = "Выбросить жмых",
                description = "Собрать жмых в пакет и выбросить."
            ),
            ServiceChecklistItem(
                title = "Вылить жидкие отходы",
                description = "Вылить канистру с отходами."
            ),
            ServiceChecklistItem(
                title = "Заменить воду",
                description = "Залить в канистру свежую воду"
            )
        )
    )
        private set

    var machinePhoto by mutableStateOf<ServicePhoto?>(null)
        private set

    var standPhoto by mutableStateOf<ServicePhoto?>(null)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var showSyncError by mutableStateOf(false)
        private set

    var syncErrorMessage by mutableStateOf("Проверьте подключение к интернету или VPN")
        private set

    fun toggleChecklist(index: Int, checked: Boolean) {

        checklist = checklist.toMutableList().also {
            it[index] = it[index].copy(
                checked = checked
            )
        }

    }

    fun setPhoto(
        stationId: Int,
        photoType: PhotoType,
        path: String
    ) {

        val photo = ServicePhoto(
            stationId = stationId,
            photoType = photoType,
            uri = Uri.parse(path)
        )

        when (photoType) {

            PhotoType.MACHINE -> machinePhoto = photo

            PhotoType.OVERVIEW -> standPhoto = photo

        }

    }

    fun isChecklistCompleted(): Boolean {

        return checklist.all {
            it.checked
        }

    }

    fun isPhotosCompleted(): Boolean {

        return machinePhoto != null &&
            standPhoto != null

    }

    fun canFinish(): Boolean {

        return isChecklistCompleted()

    }

    fun finishService(
        stationId: Int,
        onFinished: () -> Unit
    ) {

        if (isSaving) return

        viewModelScope.launch {

            isSaving = true
            showSyncError = false

            val formatter = SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            )

            val now = Date()

            val formattedDate = formatter.format(now)

            val synced = repository.finishService(
                stationId = stationId,
                machinePhotoPath = machinePhoto?.uri?.toString(),
                overviewPhotoPath = standPhoto?.uri?.toString(),
                lastServiceDate = formattedDate
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
