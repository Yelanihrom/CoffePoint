package ru.coffeepoint.feature.earnings.presentation

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.coffeepoint.core.data.repository.MonthlyEarningsAccrualResult
import ru.coffeepoint.core.data.repository.MonthlyEarningsPayment
import ru.coffeepoint.core.data.repository.MonthlyEarningsRepository
import ru.coffeepoint.core.data.repository.MonthlyEarningsResult

class MonthlyEarningsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = MonthlyEarningsRepository(application)

    var state by mutableStateOf(repository.getState())
        private set

    var payments by mutableStateOf<List<MonthlyEarningsPayment>>(emptyList())
        private set

    var loading by mutableStateOf(false)
        private set

    var showConnectionError by mutableStateOf(false)
        private set

    var accrualMessage by mutableStateOf<String?>(null)
        private set

    fun refreshPayments(
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            repository.refreshPaymentsForPeriod(
                startDate = startDate,
                endDate = endDate
            )
            state = repository.getState()
            payments = repository.getPaymentsForPeriod(
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun refresh(
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            loading = true

            when (
                repository.refresh(
                    startDate = startDate,
                    endDate = endDate
                )
            ) {
                is MonthlyEarningsResult.Success -> {
                    state = repository.getState()
                    payments = repository.getPaymentsForPeriod(
                        startDate = startDate,
                        endDate = endDate
                    )
                }

                MonthlyEarningsResult.ConnectionError -> {
                    showConnectionError = true
                }
            }

            loading = false
        }
    }

    fun addPayment(
        amount: Int,
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            repository.addPayment(
                amount = amount,
                startDate = startDate,
                endDate = endDate
            )
            state = repository.getState()
            payments = repository.getPaymentsForPeriod(
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun accrueEarnings(
        startDate: String,
        endDate: String
    ) {
        viewModelScope.launch {
            loading = true

            accrualMessage = when (
                repository.accrueEarnings(
                    startDate = startDate,
                    endDate = endDate
                )
            ) {
                MonthlyEarningsAccrualResult.Success -> {
                    state = repository.getState()
                    "\u0417\u0430\u0440\u0430\u0431\u043e\u0442\u043e\u043a \u043d\u0430\u0447\u0438\u0441\u043b\u0435\u043d"
                }

                MonthlyEarningsAccrualResult.AlreadyExists -> {
                    "\u0417\u0430 \u044d\u0442\u043e\u0442 \u043f\u0435\u0440\u0438\u043e\u0434 \u0443\u0436\u0435 \u0435\u0441\u0442\u044c \u043d\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u0435"
                }

                MonthlyEarningsAccrualResult.NeedRefresh -> {
                    "\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u0435 \u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0443 \u0437\u0430 \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u044b\u0439 \u043f\u0435\u0440\u0438\u043e\u0434"
                }
            }

            loading = false
        }
    }

    fun dismissConnectionError() {
        showConnectionError = false
    }

    fun dismissAccrualMessage() {
        accrualMessage = null
    }

}
