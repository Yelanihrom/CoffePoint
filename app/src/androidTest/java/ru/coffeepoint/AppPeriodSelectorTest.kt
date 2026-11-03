package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.shared.component.AppPeriodSelector
import ru.coffeepoint.ui.theme.CoffeePointTheme

class AppPeriodSelectorTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun dateLabelsStayInlineAndCallbacksAreIndependent() {
        var startClicks = 0
        var endClicks = 0
        compose.setContent {
            CoffeePointTheme {
                Box(Modifier.width(360.dp)) {
                    AppPeriodSelector("01.11.2026", "03.11.2026", { startClicks++ }, { endClicks++ })
                }
            }
        }
        val start = compose.onNodeWithText("С 01.11.2026").assertIsDisplayed()
        val end = compose.onNodeWithText("По 03.11.2026").assertIsDisplayed()
        val startBounds = start.fetchSemanticsNode().boundsInRoot
        val endBounds = end.fetchSemanticsNode().boundsInRoot
        assertEquals(startBounds.top, endBounds.top, 1f)
        assertTrue(startBounds.right <= endBounds.left)
        start.performClick()
        compose.runOnIdle { assertEquals(1, startClicks); assertEquals(0, endClicks) }
        end.performClick()
        compose.runOnIdle { assertEquals(1, startClicks); assertEquals(1, endClicks) }
    }

    @Test fun disabledPeriodBlocksBothDateActions() {
        var clicks = 0
        compose.setContent {
            CoffeePointTheme {
                AppPeriodSelector("01.11.2026", "Сейчас", { clicks++ }, { clicks++ }, enabled = false)
            }
        }
        compose.onNodeWithText("С 01.11.2026").assertIsNotEnabled().performClick()
        compose.onNodeWithText("По Сейчас").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test fun narrowLayoutWithLargeFontStacksFieldsWithoutSplittingLabels() {
        compose.setContent {
            CoffeePointTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.5f)) {
                    Box(Modifier.width(260.dp).testTag("period-container")) {
                        AppPeriodSelector("01.11.2026", "03.11.2026", {}, {})
                    }
                }
            }
        }
        val start = compose.onNodeWithText("С 01.11.2026").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val end = compose.onNodeWithText("По 03.11.2026").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue(start.bottom <= end.top)
        assertEquals(start.width, end.width, 1f)
        val container = compose.onNodeWithTag("period-container").fetchSemanticsNode().boundsInRoot
        assertTrue(start.left >= container.left && start.right <= container.right)
    }
}
