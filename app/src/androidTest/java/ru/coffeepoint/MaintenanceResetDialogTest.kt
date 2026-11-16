package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.station.component.MaintenanceResetDialog
import ru.coffeepoint.ui.theme.CoffeePointTheme

class MaintenanceResetDialogTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun choiceIsExplicitAndButtonsFit() {
        var choice: Boolean? = null
        var cancelled = false
        compose.setContent { CoffeePointTheme {
            MaintenanceResetDialog("16.11.2026", { choice = it }, { cancelled = true })
        } }
        compose.onNodeWithText("Обнулить счётчик продаж?").assertIsDisplayed()
        val no = compose.onNodeWithText("НЕ ОБНУЛЯТЬ").assertIsDisplayed()
        val yes = compose.onNodeWithText("ОБНУЛИТЬ").assertIsDisplayed()
        assertTrue(no.fetchSemanticsNode().boundsInRoot.right < yes.fetchSemanticsNode().boundsInRoot.left)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        java.io.File(instrumentation.targetContext.getExternalFilesDir(null), "maintenance-reset-dialog.png")
            .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        no.performClick()
        assertEquals(false, choice)
        yes.performClick()
        assertEquals(true, choice)
        compose.onNodeWithText("ОТМЕНИТЬ").assertIsDisplayed().performClick()
        assertTrue(cancelled)
    }
}
