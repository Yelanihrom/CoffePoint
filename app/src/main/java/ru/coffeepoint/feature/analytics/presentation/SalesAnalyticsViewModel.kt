package ru.coffeepoint.feature.analytics.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.SalesAnalyticsRepository
import ru.coffeepoint.core.sync.google.salesAnalyticsErrorMessage
import ru.coffeepoint.feature.analytics.model.ANALYTICS_TIME_ZONE
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import ru.coffeepoint.feature.analytics.model.SalesAnalyticsReport
import java.time.LocalDate

class SalesAnalyticsViewModel : ViewModel() {
    private val repository = SalesAnalyticsRepository()
    var period by mutableStateOf(AnalyticsPeriod.currentMonth())
        private set
    var report by mutableStateOf<SalesAnalyticsReport?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    init { refresh() }

    fun selectPeriod(value: AnalyticsPeriod) {
        if (loading || value == period) return
        period = value
        report = null
        error = null
    }

    fun refresh() {
        if (loading) return
        if (!period.isValid(LocalDate.now(ANALYTICS_TIME_ZONE))) {
            error = "Проверьте период: начало не должно быть позже конца или текущей даты"
            return
        }
        loading = true
        report = null
        error = null
        val requestedPeriod = period
        viewModelScope.launch {
            try {
                report = repository.load(requestedPeriod)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = salesAnalyticsErrorMessage(failure)
            } finally {
                loading = false
            }
        }
    }
}
