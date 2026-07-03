package ru.coffeepoint.feature.service.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.coffeepoint.feature.camera.model.PhotoType
import ru.coffeepoint.feature.camera.model.ServicePhoto
import ru.coffeepoint.feature.service.model.ServiceChecklistItem
import android.net.Uri
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.StationRepository
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
                title = "Протереть аппарат внутри",
                description = "Протереть внутренние поверхности влажной салфеткой и затем сухой."
            ),
            ServiceChecklistItem(
                title = "Протереть аппарат снаружи",
                description = "Удалить пыль с крышки и дверцы аппарата."
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
                description = "Поменять канистры с водой."
            )
        )
    )
        private set

    var machinePhoto by mutableStateOf<ServicePhoto?>(null)
        private set

    var standPhoto by mutableStateOf<ServicePhoto?>(null)
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

        return isChecklistCompleted() &&
                isPhotosCompleted()

    }

    fun finishService(
        stationId: Int,
        onFinished: () -> Unit
    ) {

        viewModelScope.launch {

            val formatter = SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.getDefault()
            )

            val now = Date()

            val formattedDate = formatter.format(now)

            repository.finishService(
                stationId = stationId,
                machinePhotoPath = machinePhoto?.uri?.toString(),
                overviewPhotoPath = standPhoto?.uri?.toString(),
                lastServiceDate = formattedDate
            )

            onFinished()

        }

    }

}