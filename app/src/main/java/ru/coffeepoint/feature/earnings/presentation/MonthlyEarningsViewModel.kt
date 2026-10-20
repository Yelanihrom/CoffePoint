package ru.coffeepoint.feature.earnings.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.MonthlyEarningsOperation
import ru.coffeepoint.core.data.repository.MonthlyEarningsRepository

class MonthlyEarningsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MonthlyEarningsRepository(application)

    var state by mutableStateOf(repository.getState())
        private set

    var operations by mutableStateOf<List<MonthlyEarningsOperation>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var showConnectionError by mutableStateOf(false)
        private set

    fun refreshOperations(
        startDate: String,
        endDate: String
    ) {
        if (loading) return

        viewModelScope.launch {
            loading = true

            try {
                operations = repository.getOperationsForPeriod(
                    startDate = startDate,
                    endDate = endDate
                )

                val refreshed = repository.refreshEmployeeTransactions()
                if (!refreshed) {
                    showConnectionError = true
                    return@launch
                }
                state = repository.getState()
                operations = repository.getOperationsForPeriod(
                    startDate = startDate,
                    endDate = endDate
                )
            } finally {
                loading = false
            }
        }
    }

    fun loadOperationsForPeriod(
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            operations = repository.getOperationsForPeriod(
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun addPayment(
        amount: Int,
        startDate: String,
        endDate: String
    ) {
        if (loading) return

        viewModelScope.launch {
            loading = true

            try {
                repository.addPayment(
                    amount = amount
                )
                state = repository.getState()
                operations = repository.getOperationsForPeriod(
                    startDate = startDate,
                    endDate = endDate
                )
                viewModelScope.launch {
                    repository.syncPendingData()
                }
            } finally {
                loading = false
            }
        }
    }

    fun dismissConnectionError() {
        showConnectionError = false
    }

}
