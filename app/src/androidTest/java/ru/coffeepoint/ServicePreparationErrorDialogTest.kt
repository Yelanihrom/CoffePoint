package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.station.component.ServicePreparationErrorDialog
import ru.coffeepoint.ui.theme.CoffeePointTheme

class ServicePreparationErrorDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun actionsAreVisibleOrderedAndInvokeTheirCallbacks() {
        var retries = 0
        var localContinues = 0
        var cancels = 0
        compose.setContent {
            CoffeePointTheme {
                ServicePreparationErrorDialog(
                    lastUpdated = "24.10.2026 22:31",
                    onRetry = { retries++ },
                    onContinueLocally = { localContinues++ },
                    onCancel = { cancels++ }
                )
            }
        }
        val retry = compose.onNodeWithText("ПОВТОРИТЬ").assertIsDisplayed()
        val locally = compose.onNodeWithText("ПРОДОЛЖИТЬ").assertIsDisplayed()
        compose.onNodeWithText("ПРОДОЛЖИТЬ ЛОКАЛЬНО").assertDoesNotExist()
        compose.onNodeWithText("Не удалось получить актуальные данные").assertIsDisplayed()
        compose.onNodeWithText("Данные от: 24.10.2026 22:31").assertIsDisplayed()
        compose.onNodeWithText("Повторите запрос или продолжите работу с текущими данными").assertIsDisplayed()
        compose.onNodeWithContentDescription("Закрыть").assertDoesNotExist()
        val cancel = compose.onNodeWithText("ОТМЕНИТЬ").assertIsDisplayed()
        val retryBounds = retry.fetchSemanticsNode().boundsInRoot
        val localBounds = locally.fetchSemanticsNode().boundsInRoot
        val cancelBounds = cancel.fetchSemanticsNode().boundsInRoot
        assertTrue(retryBounds.right < localBounds.left)
        assertTrue(retryBounds.bottom < cancelBounds.top)
        assertTrue(localBounds.bottom < cancelBounds.top)
        assertEquals(retryBounds.top, localBounds.top, 1f)
        assertEquals(retryBounds.width, localBounds.width, 1f)
        assertEquals(retryBounds.height, localBounds.height, 1f)
        retry.performClick()
        locally.performClick()
        cancel.performClick()
        compose.runOnIdle {
            assertEquals(1, retries)
            assertEquals(1, localContinues)
            assertEquals(1, cancels)
        }
    }
}
