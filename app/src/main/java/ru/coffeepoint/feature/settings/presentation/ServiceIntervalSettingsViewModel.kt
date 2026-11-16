package ru.coffeepoint.feature.settings.presentation

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeepoint.core.data.repository.ServiceIntervalRepository
import ru.coffeepoint.core.sync.google.MAX_MAINTENANCE_SALES_LIMIT
import ru.coffeepoint.feature.station.model.ServiceIntervals
import ru.coffeepoint.feature.station.model.TechServiceNode

class ServiceIntervalSettingsViewModel(
    application: Application, private val store: ServiceIntervalRepository
) : AndroidViewModel(application) {
    constructor(application: Application) : this(application, ServiceIntervalRepository(application))
    private var saved: ServiceIntervals? = null
    var values by mutableStateOf(ServiceIntervals().values.mapValues { it.value.toString() })
        private set
    var salesValues by mutableStateOf(ServiceIntervals().salesLimits.mapValues { it.value.toString() })
        private set
    var loading by mutableStateOf(true)
        private set
    var saving by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    val valid: Boolean get() = !loading && saved != null && TechServiceNode.entries.all {
        (values[it]?.toIntOrNull() ?: 0) > 0 &&
            (salesValues[it]?.toLongOrNull() ?: -1L) in 0..MAX_MAINTENANCE_SALES_LIMIT
    }
    val changed: Boolean get() = saved?.let {
        values != it.values.mapValues { entry -> entry.value.toString() } ||
            salesValues != it.salesLimits.mapValues { entry -> entry.value.toString() }
    } ?: false

    init {
        viewModelScope.launch {
            try {
                try { store.refresh() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = "Не удалось обновить настройки. Показаны данные устройства" }
                val initial = store.read()
                saved = initial
                values = initial.values.mapValues { it.value.toString() }
                salesValues = initial.salesLimits.mapValues { it.value.toString() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Не удалось загрузить настройки обслуживания" }
            finally { loading = false }
        }
    }

    fun update(node: TechServiceNode, value: String) {
        if (!saving && !loading && value.length <= 10 && value.all(Char::isDigit)) {
            values = values + (node to value)
            error = null
        }
    }

    fun updateSales(node: TechServiceNode, value: String) {
        if (!saving && !loading && value.length <= 19 && value.all(Char::isDigit)) {
            salesValues = salesValues + (node to value)
            error = null
        }
    }

    fun save(onSaved: () -> Unit) {
        if (saving || !valid || !changed) return
        val intervals = ServiceIntervals(values.mapValues { it.value.toInt() }, salesValues.mapValues { it.value.toLong() })
        saving = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { store.save(intervals) }
                saved = intervals
                val result = store.syncPending()
                if (result.synced) onSaved()
                else error = if (result.errorType == ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType.CONFLICT)
                    "Сохранено на устройстве. На сервере более свежие настройки"
                else "Сохранено на устройстве. Изменения будут отправлены при появлении интернета"
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Не удалось сохранить сроки обслуживания" }
            finally { saving = false }
        }
    }
}
