package ru.coffeepoint

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.feature.analytics.model.*
import ru.coffeepoint.feature.analytics.ui.SalesAnalyticsContent
import ru.coffeepoint.feature.dashboard.ui.ManagerHomeScreen
import ru.coffeepoint.ui.theme.CoffeePointTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate

class SalesAnalyticsScreenTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val period = AnalyticsPeriod(LocalDate.parse("2026-11-01"))

    @Test fun reportShowsTotalStationsAndOpenEndOnNarrowScreen() {
        var refreshes = 0
        compose.setContent {
            CoffeePointTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.width(320.dp)) {
                        SalesAnalyticsContent(period, SalesAnalyticsReport(123,
                            listOf(StationSales(1, "Кофе-станция на Центральной", 123),
                                StationSales(2, "Вторая станция", 0), StationSales(3, "Третья станция", null)),
                            Instant.parse("2026-11-03T10:15:00Z")), false, null,
                            {}, { refreshes++ }, {})
                    }
                }
            }
        }
        compose.onNodeWithText("С 01.11.2026").assertIsDisplayed()
        compose.onNodeWithText("По Сейчас").assertIsDisplayed()
        compose.onNodeWithText("Всего продаж").assertIsDisplayed()
        compose.onNodeWithText("Обновлено: 03.11.2026 13:15").assertIsDisplayed()
        compose.onNodeWithText("ОБНОВИТЬ ПРОДАЖИ").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-tests").apply { mkdirs() }
        File(directory, "analytics.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Нет терминала").performScrollTo().assertIsDisplayed()
    }

    @Test fun loadingBlocksRequestAndPeriodChanges() {
        var requests = 0
        compose.setContent {
            CoffeePointTheme {
                SalesAnalyticsContent(period, null, true, null, {}, { requests++ }, {})
            }
        }
        compose.onNodeWithContentDescription("Загрузка данных").assertIsDisplayed()
        compose.onNodeWithText("ОБНОВИТЬ ПРОДАЖИ").assertIsNotEnabled()
        compose.onNodeWithText("По Сейчас").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun closedPeriodShowsEndDateWithoutResetButtonAndErrorDoesNotShowZeroTotal() {
        compose.setContent {
            CoffeePointTheme {
                SalesAnalyticsContent(period.copy(end = LocalDate.parse("2026-11-02")), null, false,
                    "Не удалось получить продажи", {}, {}, {})
            }
        }
        compose.onNodeWithText("Всего продаж").assertDoesNotExist()
        compose.onNodeWithText("Не удалось получить продажи").assertIsDisplayed()
        compose.onNodeWithText("По 02.11.2026").assertIsDisplayed()
        compose.onNodeWithText("По текущий момент").assertDoesNotExist()
    }

    @Test fun managerMenuHasAnalyticsEntry() {
        var opened = false
        compose.setContent {
            CoffeePointTheme { ManagerHomeScreen({}, {}, { opened = true }, {}, {}, {}) }
        }
        compose.onNodeWithText("Аналитика").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun managerMenuHasFourthCatalogEntry() {
        var opened = false
        compose.setContent {
            CoffeePointTheme { ManagerHomeScreen({}, {}, {}, { opened = true }, {}, {}) }
        }
        compose.onNodeWithText("Каталог закупок").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }
}
