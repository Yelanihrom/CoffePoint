package ru.coffeepoint

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.espresso.Espresso
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.preferences.ServiceIntervalPreferences
import ru.coffeepoint.feature.dashboard.component.StationCard
import ru.coffeepoint.feature.dashboard.model.*
import ru.coffeepoint.feature.settings.presentation.ServiceIntervalSettingsViewModel
import ru.coffeepoint.feature.settings.ui.ServiceIntervalSettingsContent
import ru.coffeepoint.feature.settings.ui.SettingsScreen
import ru.coffeepoint.feature.station.model.*
import ru.coffeepoint.shared.component.rememberServiceIntervals
import ru.coffeepoint.ui.theme.CoffeePointTheme
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ServiceIntervalSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun menuOpensServiceIntervals() {
        var opened = false
        compose.setContent { CoffeePointTheme { SettingsScreen({}, {}, { opened = true }) } }
        compose.onNodeWithText("Обслуживание узлов").performClick()
        compose.runOnIdle { assertTrue(opened) }
    }

    @Test fun numericFieldsFitNarrowScreenAndRejectNonDigits() {
        var saved = false
        compose.setContent {
            var values by remember { mutableStateOf(ServiceIntervals().values.mapValues { it.value.toString() }) }
            CoffeePointTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.width(320.dp)) {
                        ServiceIntervalSettingsContent(values, { node, value -> values = values + (node to value) },
                            { saved = true }, {}, values.values.all { (it.toIntOrNull() ?: 0) > 0 })
                    }
                }
            }
        }
        compose.onNodeWithTag("service_days_MIXERS").performScrollTo().performTextReplacement("12abc")
        compose.onNodeWithTag("service_days_MIXERS").assertTextContains("12")
        compose.onNodeWithTag("service_days_MIXERS").performTextReplacement("0")
        compose.onNodeWithText("СОХРАНИТЬ").assertIsNotEnabled()
        compose.onNodeWithTag("service_days_MIXERS").performTextReplacement("45")
        compose.onNodeWithText("СОХРАНИТЬ").performClick()
        compose.runOnIdle { assertTrue(saved) }
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("service_days_EXHAUST").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("service_days_LIQUID_WASTE_CONTAINER").performScrollTo().assertIsDisplayed()
        val file = File(context.getExternalFilesDir(null), "service-interval-settings.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun savedIntervalsPersistAndCardAndNotificationSelectionUseThem() {
        val prefs = context.getSharedPreferences("service_intervals", 0)
        val original = prefs.all
        val store = ServiceIntervalPreferences(prefs)
        try {
            store.save(ServiceIntervals())
            val detail = StationDetail(CoffeeStation(7, "Test station", lastServiceDate = "", salesAfterService = 0,
                mixersServiceDate = LocalDate.now(ZoneOffset.UTC).minusDays(20).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                StationInventory(0, 0, 0, 0, 0, 0, 0, 0),
                StationSettings(cupsMax = 0, lidsMax = 0, coffeeMax = 0, milkMax = 0, chocolateMax = 0, toppingMax = 0))
            compose.setContent {
                val intervals = rememberServiceIntervals()
                CoffeePointTheme { StationCard(detail, DashboardMode.SUPPLY, false, false, false,
                    showSupplyOperatorTechService = true, serviceIntervals = intervals) }
            }
            compose.onNodeWithText("Обслуживание не требуется").assertIsDisplayed()
            compose.runOnIdle { store.save(ServiceIntervals(ServiceIntervals().values + (TechServiceNode.MIXERS to 10))) }
            compose.waitUntil { compose.onAllNodesWithText("Миксеры").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Миксеры").assertIsDisplayed()
            val restored = ServiceIntervalPreferences(prefs).read()
            assertEquals(10, restored[TechServiceNode.MIXERS])
            assertEquals(listOf("Миксеры"), detail.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER, restored) { true }.map { it.title })
            assertTrue(detail.supplyAttentionTechServiceDashboardItems(UserRole.OPERATOR, restored) { true }.isEmpty())
            assertTrue(detail.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER, restored) { false }.isEmpty())
        } finally {
            prefs.edit().clear().apply { original.forEach { (key, value) -> if (value is Int) putInt(key, value) } }.commit()
        }
    }

    @Test fun viewModelValidatesAndOnlyPersistsOnSave() = runBlocking {
        val prefs = context.getSharedPreferences("service_intervals", 0)
        val original = prefs.all
        val store = ServiceIntervalPreferences(prefs)
        try {
            store.save(ServiceIntervals())
            val model = ServiceIntervalSettingsViewModel(context.applicationContext as Application)
            model.update(TechServiceNode.MIXERS, "0")
            assertFalse(model.valid)
            model.update(TechServiceNode.MIXERS, "")
            assertFalse(model.valid)
            model.update(TechServiceNode.MIXERS, "9999999999")
            assertFalse(model.valid)
            model.update(TechServiceNode.MIXERS, "45")
            assertTrue(model.valid && model.changed)
            assertEquals(30, store.read()[TechServiceNode.MIXERS])
            var saved = false
            compose.runOnIdle { model.save { saved = true } }
            withTimeout(10000) { while (!saved) delay(20) }
            assertEquals(45, store.read()[TechServiceNode.MIXERS])
            val reopened = ServiceIntervalSettingsViewModel(context.applicationContext as Application)
            assertEquals("45", reopened.values[TechServiceNode.MIXERS])
            assertFalse(reopened.changed)
        } finally {
            prefs.edit().clear().apply { original.forEach { (key, value) -> if (value is Int) putInt(key, value) } }.commit()
        }
    }
}
