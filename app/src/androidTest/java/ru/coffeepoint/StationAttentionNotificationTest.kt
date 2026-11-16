package ru.coffeepoint

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.room.withTransaction
import kotlinx.coroutines.runBlocking
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.coffeepoint.core.auth.RolePreferences
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.notification.StationAttentionNotification
import ru.coffeepoint.feature.station.model.*

class StationAttentionNotificationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val notifications = StationAttentionNotification(context)
    private val preferenceNames = listOf("notification_settings", "notification_intervals", "role_preferences", "service_intervals")
    private lateinit var original: Map<String, Map<String, *>>

    @Before fun prepare() {
        if (Build.VERSION.SDK_INT >= 33) {
            instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        original = preferenceNames.associateWith { context.getSharedPreferences(it, 0).all }
        preferenceNames.forEach { context.getSharedPreferences(it, 0).edit().clear().commit() }
        clearNotifications()
        RolePreferences(context).saveSelectedRole(UserRole.MANAGER)
    }

    @After fun restore() {
        clearNotifications()
        original.forEach { (name, values) ->
            context.getSharedPreferences(name, 0).edit().clear().apply {
                values.forEach { (key, value) ->
                    when (value) {
                        is String -> putString(key, value)
                        is Boolean -> putBoolean(key, value)
                        is Long -> putLong(key, value)
                        is Int -> putInt(key, value)
                    }
                }
            }.commit()
        }
    }

    @Test fun stockAndNodesHaveSeparateCardsAndTimers() {
        val stations = listOf(station())
        notifications.showCriticalStockAttentionIfNeeded(stations)
        val stockTimestamp = intervals().getLong("last_shown_1002", 0)
        assertTrue(stockTimestamp > 0)
        runBlocking { notifications.showServiceAttentionIfNeeded(stations) }
        assertTrue(intervals().getLong("last_shown_1004", 0) > 0)
        val stock = posted(1002)
        val service = posted(1004)
        assertEquals("Нужно пополнить станции", stock.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Требуется обслуживание узлов", service.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(stock.extras.getString(Notification.EXTRA_BIG_TEXT)!!.contains("Кофе"))
        assertFalse(stock.extras.getString(Notification.EXTRA_BIG_TEXT)!!.contains("Миксеры"))
        assertTrue(service.extras.getString(Notification.EXTRA_BIG_TEXT)!!.contains("Миксеры"))
        assertFalse(service.extras.getString(Notification.EXTRA_BIG_TEXT)!!.contains("Кофе"))
        assertTrue(service.extras.getString(Notification.EXTRA_BIG_TEXT)!!.contains("Тест (Адрес)"))
        clearNotifications()
        scan(stations)
        assertTrue(manager.activeNotifications.none { it.id == 1002 || it.id == 1004 })
        assertEquals(stockTimestamp, intervals().getLong("last_shown_1002", 0))
    }

    @Test fun existingSupplyTimerDoesNotBlockFirstServiceNotification() {
        intervals().edit().putLong("last_shown_1002", System.currentTimeMillis()).commit()
        scan(listOf(station()))
        assertFalse(manager.activeNotifications.any { it.id == 1002 })
        posted(1004)
    }

    @Test fun roleAndIndependentFlagsAreRespected() {
        RolePreferences(context).saveSelectedRole(UserRole.OPERATOR)
        scan(listOf(station()))
        assertTrue(manager.activeNotifications.none { it.id == 1002 || it.id == 1004 })
        RolePreferences(context).saveSelectedRole(UserRole.SUPPLY_OPERATOR)
        scan(listOf(station()))
        posted(1002)
        assertFalse(manager.activeNotifications.any { it.id == 1004 })
        clearNotifications()
        context.getSharedPreferences("notification_settings", 0).edit()
            .putBoolean("SUPPLY_OPERATOR.CRITICAL_STOCK", false)
            .putBoolean("SUPPLY_OPERATOR.MIXERS", true).commit()
        intervals().edit().clear().commit()
        scan(listOf(station()))
        assertFalse(manager.activeNotifications.any { it.id == 1002 })
        posted(1004)
    }

    @Test fun noReasonsAndUnconfiguredMinimumDoNotConsumeTimers() {
        scan(emptyList())
        val detail = station()
        scan(listOf(detail.copy(station = detail.station.copy(mixersServiceDate = ""),
            settings = detail.settings.copy(coffeeMin = 0))))
        assertTrue(intervals().all.isEmpty())
        assertTrue(manager.activeNotifications.none { it.id == 1002 || it.id == 1004 })
    }

    @Test fun salesLimitUsesRoomSettingsAndUnknownCountDoesNotNotify(): Unit = runBlocking {
        val repository = ru.coffeepoint.core.data.repository.ServiceIntervalRepository(context)
        val db = ru.coffeepoint.core.data.local.DatabaseProvider.getDatabase(context)
        val originalRows = db.maintenanceSettingsDao().getAll()
        try {
            repository.save(ServiceIntervals(salesLimits = ServiceIntervals().salesLimits + (TechServiceNode.MIXERS to 500L)))
            val today = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            val detail = station().let { it.copy(station = it.station.copy(mixersServiceDate = today,
                maintenanceSales = mapOf(TechServiceNode.MIXERS to 499L))) }
            notifications.showServiceAttentionIfNeeded(listOf(detail))
            assertFalse(manager.activeNotifications.any { it.id == 1004 })
            notifications.showServiceAttentionIfNeeded(listOf(detail.copy(station = detail.station.copy(maintenanceSales = emptyMap()))))
            assertFalse(manager.activeNotifications.any { it.id == 1004 })
            notifications.showServiceAttentionIfNeeded(listOf(detail.copy(station = detail.station.copy(
                maintenanceSales = mapOf(TechServiceNode.MIXERS to 500L)))))
            posted(1004)
        } finally {
            db.withTransaction {
                db.openHelper.writableDatabase.execSQL("DELETE FROM maintenance_settings")
                db.maintenanceSettingsDao().upsertAll(originalRows)
            }
        }
    }

    private fun scan(stations: List<StationDetail>) {
        notifications.showCriticalStockAttentionIfNeeded(stations)
        runBlocking { notifications.showServiceAttentionIfNeeded(stations) }
    }

    private fun posted(id: Int): Notification {
        // NotificationManager publishes through a binder; wait for the system to expose the card.
        repeat(50) {
            manager.activeNotifications.firstOrNull { it.id == id }?.let { return it.notification }
            Thread.sleep(20)
        }
        error("Notification $id was not posted")
    }

    private fun intervals() = context.getSharedPreferences("notification_intervals", 0)

    private fun clearNotifications() {
        manager.cancel(1002)
        manager.cancel(1004)
        repeat(50) {
            if (manager.activeNotifications.none { it.id == 1002 || it.id == 1004 }) return
            Thread.sleep(20)
        }
        error("Notifications were not cancelled")
    }

    private fun station() = StationDetail(
        CoffeeStation(7, "Тест", address = "Адрес", lastServiceDate = "", salesAfterService = 0,
            mixersServiceDate = "01.01.2020"),
        StationInventory(0, 0, 0, 0, 0, 0, 0, 0),
        StationSettings(cupsMax = 0, lidsMax = 0, coffeeMax = 10, milkMax = 0,
            chocolateMax = 0, toppingMax = 0, sugarMax = 0, stirrersMax = 0, garbageBagsMax = 0)
    )
}
