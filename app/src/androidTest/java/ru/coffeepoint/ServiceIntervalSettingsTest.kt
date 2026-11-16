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
import androidx.room.Room
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.repository.ServiceIntervalRepository
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
            var sales by remember { mutableStateOf(ServiceIntervals().salesLimits.mapValues { it.value.toString() }) }
            CoffeePointTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.3f)) {
                    Box(Modifier.width(320.dp)) {
                        ServiceIntervalSettingsContent(values, { node, value -> values = values + (node to value) },
                            { saved = true }, {}, values.values.all { (it.toIntOrNull() ?: 0) > 0 } &&
                                sales.values.all { (it.toLongOrNull() ?: -1L) >= 0 },
                            salesValues = sales, onSalesChange = { node, value -> sales = sales + (node to value) })
                    }
                }
            }
        }
        compose.onNodeWithTag("service_days_MIXERS").performScrollTo().performTextReplacement("12abc")
        compose.onNodeWithTag("service_days_MIXERS").assertTextContains("12")
        compose.onNodeWithTag("service_days_MIXERS").performTextReplacement("0")
        compose.onNodeWithText("СОХРАНИТЬ").assertIsNotEnabled()
        compose.onNodeWithTag("service_days_MIXERS").performTextReplacement("45")
        compose.onNodeWithTag("service_sales_MIXERS").performTextReplacement("9999999999999999999")
        compose.onNodeWithText("СОХРАНИТЬ").assertIsNotEnabled()
        compose.onNodeWithTag("service_sales_MIXERS").performTextReplacement("500")
        compose.onNodeWithTag("service_sales_MIXERS").assertTextContains("500")
        compose.onNodeWithText("СОХРАНИТЬ").performClick()
        compose.runOnIdle { assertTrue(saved) }
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("service_days_EXHAUST").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("service_days_LIQUID_WASTE_CONTAINER").performScrollTo().assertIsDisplayed()
        val file = File(context.getExternalFilesDir(null), "service-interval-settings.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun savedIntervalsPersistAndCardAndNotificationSelectionUseThem() {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ServiceIntervalRepository(db)
        try {
            runBlocking { store.save(ServiceIntervals()) }
            val detail = StationDetail(CoffeeStation(7, "Test station", lastServiceDate = "", salesAfterService = 0,
                mixersServiceDate = LocalDate.now(ZoneOffset.UTC).minusDays(20).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                StationInventory(0, 0, 0, 0, 0, 0, 0, 0),
                StationSettings(cupsMax = 0, lidsMax = 0, coffeeMax = 0, milkMax = 0, chocolateMax = 0, toppingMax = 0))
            compose.setContent {
                val intervals = rememberServiceIntervals(store)
                CoffeePointTheme { StationCard(detail, DashboardMode.SUPPLY, false, false, false,
                    showSupplyOperatorTechService = true, serviceIntervals = intervals) }
            }
            compose.onNodeWithText("Обслуживание не требуется").assertIsDisplayed()
            runBlocking { store.save(ServiceIntervals(ServiceIntervals().values + (TechServiceNode.MIXERS to 10))) }
            compose.waitUntil { compose.onAllNodesWithText("Миксеры").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Миксеры").assertIsDisplayed()
            val restored = runBlocking { store.read() }
            assertEquals(10, restored[TechServiceNode.MIXERS])
            assertEquals(listOf("Миксеры"), detail.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER, restored) { true }.map { it.title })
            assertTrue(detail.supplyAttentionTechServiceDashboardItems(UserRole.OPERATOR, restored) { true }.isEmpty())
            assertTrue(detail.supplyAttentionTechServiceDashboardItems(UserRole.MANAGER, restored) { false }.isEmpty())
        } finally {
            db.close()
        }
    }

    @Test fun viewModelValidatesAndOnlyPersistsOnSave() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val store = ServiceIntervalRepository(db, send = { ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult.success() })
        try {
            store.save(ServiceIntervals())
            val model = ServiceIntervalSettingsViewModel(context.applicationContext as Application, store)
            withTimeout(10000) { while (model.loading) delay(20) }
            model.update(TechServiceNode.MIXERS, "0")
            assertFalse(model.valid)
            model.update(TechServiceNode.MIXERS, "")
            assertFalse(model.valid)
            model.update(TechServiceNode.MIXERS, "9999999999")
            assertFalse(model.valid)
            model.update(TechServiceNode.MIXERS, "45")
            model.updateSales(TechServiceNode.MIXERS, "9999999999999999999")
            assertFalse(model.valid)
            model.updateSales(TechServiceNode.MIXERS, "9007199254740992")
            assertFalse(model.valid)
            model.updateSales(TechServiceNode.MIXERS, "500")
            assertTrue(model.valid && model.changed)
            assertEquals(30, store.read()[TechServiceNode.MIXERS])
            var saved = false
            compose.runOnIdle { model.save { saved = true } }
            withTimeout(10000) { while (!saved) delay(20) }
            assertEquals(45, store.read()[TechServiceNode.MIXERS])
            assertEquals(500L, store.read().salesLimit(TechServiceNode.MIXERS))
            val reopened = ServiceIntervalSettingsViewModel(context.applicationContext as Application, store)
            withTimeout(10000) { while (reopened.loading) delay(20) }
            assertEquals("45", reopened.values[TechServiceNode.MIXERS])
            assertFalse(reopened.changed)
        } finally {
            db.close()
        }
    }
}
