package ru.coffeepoint

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.core.data.local.entity.ServiceVisitHistoryRow
import ru.coffeepoint.feature.analytics.ui.AnalyticsMenuScreen
import ru.coffeepoint.feature.analytics.ui.ServiceVisitHistoryContent
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import ru.coffeepoint.ui.theme.CoffeePointTheme
import java.io.File
import java.time.LocalDate

class AnalyticsSectionsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun menuOpensBothSectionsAndHistoryReturnsBack() {
        var sales = 0
        compose.setContent {
            var history by remember { mutableStateOf(false) }
            CoffeePointTheme {
                if (history) ServiceVisitHistoryContent(emptyList(), false, null, {}, { history = false })
                else AnalyticsMenuScreen({}, { sales++ }, { history = true })
            }
        }
        compose.onNodeWithText("Продажи").performClick()
        compose.runOnIdle { assertEquals(1, sales) }
        compose.onNodeWithText("История обслуживаний").performClick()
        compose.onNodeWithText("За этот период обслуживаний нет").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Продажи").assertIsDisplayed()
        screenshot("analytics-menu.png")
    }

    @Test fun historyShowsMoscowTimeSupplierAddressAndOfflineState() {
        var refreshes = 0
        compose.setContent {
            CoffeePointTheme {
                ServiceVisitHistoryContent(listOf(ServiceVisitHistoryRow("visit", 7,
                    "Поликлиника 1", "ул. Ваныкина, 10", 2, "Денис",
                    "2026-11-10T21:30:00Z", false, false)), false,
                    "Не удалось обновить историю. Показаны данные устройства", { refreshes++ }, {})
            }
        }
        compose.onNodeWithText("Поликлиника 1").assertIsDisplayed()
        compose.onNodeWithText("ул. Ваныкина, 10").assertIsDisplayed()
        compose.onNodeWithText("Денис").assertIsDisplayed()
        compose.onNodeWithText("11.11.2026 00:30").assertIsDisplayed()
        compose.onNodeWithText("Ожидает отправки").assertIsDisplayed()
        compose.onNodeWithContentDescription("Обновить историю").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
        screenshot("service-history.png")
    }

    @Test fun loadingBlocksRefreshAndMissingReferencesStayVisible() {
        compose.setContent {
            CoffeePointTheme {
                ServiceVisitHistoryContent(listOf(ServiceVisitHistoryRow("visit", 17,
                    null, null, 12, null, "2026-11-10T10:00:00Z", false, true)), true, null, {}, {})
            }
        }
        compose.onNodeWithContentDescription("Обновить историю").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Загрузка данных").assertIsDisplayed()
        compose.onNodeWithText("Сейчас", substring = true).assertIsNotEnabled()
        compose.onNodeWithText("Станция №17").assertIsDisplayed()
        compose.onNodeWithText("Сотрудник №12").assertIsDisplayed()
        compose.onNodeWithText("Не отправлено").assertIsDisplayed()
    }

    @Test fun historyPeriodCanBeChangedWithoutRefreshing() {
        var refreshes = 0
        var changes = 0
        compose.setContent {
            var period by remember {
                mutableStateOf(AnalyticsPeriod(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10)))
            }
            CoffeePointTheme {
                ServiceVisitHistoryContent(emptyList(), false, null, { refreshes++ }, {}, period, {
                    changes++
                    period = it
                })
            }
        }
        compose.onNodeWithText("По 10.11.2026").performClick()
        compose.onNodeWithText("Выбрать", ignoreCase = true).performClick()
        compose.runOnIdle {
            assertEquals(1, changes)
            assertEquals(0, refreshes)
        }
        compose.onNodeWithText("По 10.11.2026").assertIsDisplayed()
    }

    private fun screenshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "ui-tests").apply { mkdirs() }
        File(directory, name).outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
