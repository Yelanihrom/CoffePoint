package ru.coffeepoint.feature.settings.presentation

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeepoint.core.data.preferences.ServiceIntervalPreferences
import ru.coffeepoint.feature.station.model.ServiceIntervals
import ru.coffeepoint.feature.station.model.TechServiceNode

class ServiceIntervalSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ServiceIntervalPreferences(application)
    private var saved = store.read()
    var values by mutableStateOf(saved.values.mapValues { it.value.toString() })
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val valid: Boolean get() = TechServiceNode.entries.all { (values[it]?.toIntOrNull() ?: 0) > 0 }
    val changed: Boolean get() = values != saved.values.mapValues { it.value.toString() }

    fun update(node: TechServiceNode, value: String) {
        if (!saving && value.length <= 10 && value.all(Char::isDigit)) {
            values = values + (node to value)
            error = null
        }
    }

    fun save(onSaved: () -> Unit) {
        if (saving || !valid || !changed) return
        val intervals = ServiceIntervals(values.mapValues { it.value.toInt() })
        saving = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(intervals) }
                saved = intervals
                onSaved()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Не удалось сохранить сроки обслуживания" }
            finally { saving = false }
        }
    }
}
