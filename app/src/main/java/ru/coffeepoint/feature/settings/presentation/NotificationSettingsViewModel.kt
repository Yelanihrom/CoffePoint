package ru.coffeepoint.feature.settings.presentation

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.repository.NotificationSettingsRepository
import ru.coffeepoint.core.notification.NotificationOption
import ru.coffeepoint.core.notification.NotificationSettings
import ru.coffeepoint.core.sync.google.worker.GoogleSheetsSyncScheduler

class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settings = NotificationSettings(application)
    private val repository = NotificationSettingsRepository(application)
    var values by mutableStateOf(readValues())
        private set
    var isBusy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var hasPending by mutableStateOf(settings.pending().isNotEmpty())
        private set

    init {
        viewModelScope.launch {
            settings.changes().collect {
                values = readValues()
                hasPending = settings.pending().isNotEmpty()
            }
        }
        refresh()
    }

    fun refresh() = runOperation {
        repository.refresh()
    }

    fun setEnabled(role: UserRole, option: NotificationOption, value: Boolean) = runOperation {
        withContext(Dispatchers.IO) { settings.setEnabled(role, option, value) }
        GoogleSheetsSyncScheduler.enqueueSync(getApplication())
        if (!repository.syncPending().synced) {
            message = "Сохранено на устройстве. Ожидает отправки"
        }
    }

    private fun runOperation(block: suspend () -> Unit) {
        if (isBusy) return
        isBusy = true
        message = null
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { message = "Не удалось синхронизировать настройки. Показаны данные устройства" }
            finally {
                values = readValues()
                hasPending = settings.pending().isNotEmpty()
                isBusy = false
            }
        }
    }

    private fun readValues() = UserRole.entries.associateWith { role ->
        NotificationOption.entries.associateWith { option -> settings.isEnabled(role, option) }
    }
}
