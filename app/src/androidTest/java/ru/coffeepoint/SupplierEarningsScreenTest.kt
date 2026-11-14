package ru.coffeepoint

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.earnings.presentation.MonthlyEarningsViewModel
import ru.coffeepoint.feature.earnings.ui.MonthlyEarningsScreen
import ru.coffeepoint.ui.theme.CoffeePointTheme

class SupplierEarningsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var earningsViewModel: MonthlyEarningsViewModel

    private fun content(manager: Boolean) {
        compose.setContent {
            val viewModel = remember {
                MonthlyEarningsViewModel(compose.activity.application as Application,
                    SavedStateHandle(mapOf("employeeRole" to "SUPPLY_OPERATOR")))
            }
            earningsViewModel = viewModel
            CoffeePointTheme {
                MonthlyEarningsScreen(canCreatePayment = manager, onBack = {},
                    viewModel = viewModel)
            }
        }
        compose.waitUntil(10000) { !earningsViewModel.loading }
        compose.runOnIdle { earningsViewModel.dismissConnectionError() }
    }

    @Test fun managerCanEnterPaymentWithoutWorkStartDate() {
        content(true)
        compose.onNodeWithText("Заработок снабженца").assertIsDisplayed()
        compose.onNodeWithText("Дата начала работы").assertDoesNotExist()
        compose.onNodeWithText("К выплате снабженцу").assertIsDisplayed()
        compose.onNodeWithText("ВЫПЛАТИТЬ").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("500")
        compose.onNodeWithText("ВЫПЛАТИТЬ").assertIsEnabled()
    }

    @Test fun supplierCannotCreateOwnPayments() {
        content(false)
        compose.onNodeWithText("Дата начала работы").assertDoesNotExist()
        compose.onNodeWithText("ВЫПЛАТИТЬ").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
    }
}
