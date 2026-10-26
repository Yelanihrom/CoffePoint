package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.dashboard.component.AppVersionFooter
import ru.coffeepoint.ui.theme.CoffeePointTheme

class AppVersionFooterTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun versionAndLinkFitNarrowScreenAndLinkRunsCheck() {
        var checks = 0
        compose.setContent {
            CoffeePointTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.width(300.dp)) {
                        AppVersionFooter("1.0.19-ci", false, true, null, false) { checks++ }
                    }
                }
            }
        }
        val version = compose.onNodeWithText("Версия 1.0.19-ci").assertIsDisplayed()
        val link = compose.onNodeWithText("Проверить обновление").assertIsDisplayed()
        val first = version.fetchSemanticsNode().boundsInRoot
        val second = link.fetchSemanticsNode().boundsInRoot
        assertTrue(first.right <= second.left || first.bottom <= second.top)
        link.performClick()
        compose.runOnIdle { assertEquals(1, checks) }
    }

    @Test fun checkingBlocksRepeatedClicksAndShowsProgress() {
        var checks = 0
        compose.setContent {
            CoffeePointTheme { AppVersionFooter("1.0.19", true, true, null, false) { checks++ } }
        }
        compose.onNodeWithText("Проверяем обновление…").assertIsDisplayed().assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, checks) }
    }

    @Test fun feedbackAndDisabledDownloadStateAreVisible() {
        compose.setContent {
            CoffeePointTheme {
                AppVersionFooter("1.0.19", false, false, "Не удалось проверить обновление. Повторите позже", true) {}
            }
        }
        compose.onNodeWithText("Проверить обновление").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Не удалось проверить обновление. Повторите позже").assertIsDisplayed()
    }
}
