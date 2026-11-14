package ru.coffeepoint.feature.analytics.presentation

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.ServiceVisitHistoryRow
import ru.coffeepoint.core.data.repository.EmployeeSyncRepository
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import ru.coffeepoint.feature.analytics.model.filterServiceVisits

class ServiceVisitHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = DatabaseProvider.getDatabase(application)
    private val sync = EmployeeSyncRepository(application, db)
    private var allVisits by mutableStateOf<List<ServiceVisitHistoryRow>>(emptyList())
    var period by mutableStateOf(AnalyticsPeriod.currentMonth())
        private set
    val visits: List<ServiceVisitHistoryRow> get() = filterServiceVisits(allVisits, period)
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { db.stationServiceVisitDao().observeHistory().collect { allVisits = it } }
        refresh()
    }

    fun selectPeriod(value: AnalyticsPeriod) {
        period = value
    }

    fun refresh() {
        if (loading) return
        loading = true
        error = null
        viewModelScope.launch {
            try {
                if (!sync.refreshEmployeeDataFromGoogleSheets()) {
                    error = "Не удалось обновить историю. Показаны данные устройства"
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error = "Не удалось обновить историю. Показаны данные устройства" }
            finally { loading = false }
        }
    }
}
