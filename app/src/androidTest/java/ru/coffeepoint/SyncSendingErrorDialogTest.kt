package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.savedLocallySyncMessage
import ru.coffeepoint.ui.theme.CoffeePointTheme

class SyncSendingErrorDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun savedDataButtonsFitOneLineWithoutClipping() {
        val message = savedLocallySyncMessage("Проверьте интернет или VPN.")
        assertFalse(message.contains("VPN."))
        assertFalse(message.endsWith('.'))
        compose.setContent {
            CoffeePointTheme {
                SyncSendingErrorDialog(
                    title = "Изменения сохранены на устройстве",
                    message = message,
                    dismissText = "Продолжить",
                    retryText = "Повторить",
                    onDismiss = {},
                    onRetry = {}
                )
            }
        }
        compose.onNodeWithText(message).assertIsDisplayed()
        for (label in listOf("ПРОДОЛЖИТЬ", "ПОВТОРИТЬ")) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(label).assertIsDisplayed().performSemanticsAction(
                SemanticsActions.GetTextLayoutResult
            ) { it(layouts) }
            assertEquals(1, layouts.single().lineCount)
            assertFalse("Button label overflows: $label", layouts.single().hasVisualOverflow)
        }
    }
}
