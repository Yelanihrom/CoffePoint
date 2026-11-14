package ru.coffeepoint

import android.view.MotionEvent
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.ui.theme.CoffeePointTheme

class BlockingSyncOverlayTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun overlayBlocksTouchAndBackThenReleasesScreen() {
        var busy by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            CoffeePointTheme {
                Button(onClick = { clicks++ }) { Text("Действие") }
                if (busy) SyncSendingOverlay()
            }
        }
        val bounds = compose.onNodeWithText("Действие").fetchSemanticsNode().boundsInWindow
        compose.runOnIdle { busy = true }
        compose.onNodeWithContentDescription("Загрузка данных").assertIsDisplayed()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val time = SystemClock.uptimeMillis()
        listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
            val event = MotionEvent.obtain(time, SystemClock.uptimeMillis(), action,
                bounds.center.x, bounds.center.y, 0)
            try { automation.injectInputEvent(event, true) } finally { event.recycle() }
        }
        Espresso.pressBack()
        compose.onNodeWithContentDescription("Загрузка данных").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, clicks)
            busy = false
        }
        compose.onNodeWithContentDescription("Загрузка данных").assertDoesNotExist()
        compose.onNodeWithText("Действие").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
