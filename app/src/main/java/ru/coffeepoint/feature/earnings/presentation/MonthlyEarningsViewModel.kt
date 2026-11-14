package ru.coffeepoint.feature.earnings.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.MonthlyEarningsOperation
import ru.coffeepoint.core.data.repository.MonthlyEarningsRepository
import ru.coffeepoint.core.data.repository.LocalSupplierUnavailableException
import ru.coffeepoint.core.auth.UserRole
import kotlin.coroutines.cancellation.CancellationException

class MonthlyEarningsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    val employeeRole = savedStateHandle.get<String>("employeeRole")?.let {
        runCatching { UserRole.valueOf(it) }.getOrNull()
    } ?: UserRole.OPERATOR
    private val repository = MonthlyEarningsRepository(application, employeeRole)

    var errorMessage by mutableStateOf("Проверьте подключение к интернету и отключите VPN")
        private set

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
                try {
                    operations = repository.getOperationsForPeriod(startDate, endDate)
                } catch (_: LocalSupplierUnavailableException) {
                    // A fresh production installation must import its supplier before reading the cache.
                    operations = emptyList()
                }

                val refreshed = repository.refreshEmployeeTransactions()
                state = repository.getState()
                if (!refreshed) {
                    errorMessage = "Проверьте подключение к интернету и отключите VPN"
                    showConnectionError = true
                    return@launch
                }
                operations = repository.getOperationsForPeriod(
                    startDate = startDate,
                    endDate = endDate
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                errorMessage = if (error is LocalSupplierUnavailableException)
                    "Не удалось определить снабженца. Проверьте активного сотрудника в таблице"
                    else "Не удалось загрузить данные сотрудника"
                showConnectionError = true
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
            try {
                operations = repository.getOperationsForPeriod(
                    startDate = startDate,
                    endDate = endDate
                )
            } catch (cancellation: CancellationException) { throw cancellation }
            catch (_: Exception) {
                errorMessage = "Не удалось загрузить данные сотрудника"
                showConnectionError = true
            }
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
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                errorMessage = "Не удалось сохранить выплату"
                showConnectionError = true
            } finally {
                loading = false
            }
        }
    }

    fun dismissConnectionError() {
        showConnectionError = false
    }

}
