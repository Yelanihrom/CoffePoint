package ru.coffeepoint

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.service.ui.GrindRangeScale
import ru.coffeepoint.ui.theme.CoffeePointTheme

class GrindRangeScaleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun excellentRangeStartsAtThreeAndEndsAtSix() {
        compose.setContent {
            CoffeePointTheme { Box(Modifier.width(320.dp)) { GrindRangeScale() } }
        }
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithText("6").assertIsDisplayed()
        compose.onNodeWithText("4").assertDoesNotExist()
        compose.onNodeWithText("Отлично").assertIsDisplayed()
        val pixels = compose.onNodeWithContentDescription("Шкала помола от 0 до 10, отлично от 3 до 6")
            .captureToImage().toPixelMap()
        val green = Color(0xFF43C477)
        fun colorAt(fraction: Float) = pixels[(pixels.width * fraction).toInt(), pixels.height / 2]
        assertEquals(green, colorAt(0.35f))
        assertEquals(green, colorAt(0.45f))
        assertEquals(green, colorAt(0.55f))
        assertNotEquals(green, colorAt(0.25f))
        assertNotEquals(green, colorAt(0.65f))
    }
}
