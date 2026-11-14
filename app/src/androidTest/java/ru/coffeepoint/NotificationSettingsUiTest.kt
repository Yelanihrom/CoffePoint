package ru.coffeepoint

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.notification.NotificationOption
import ru.coffeepoint.core.notification.NotificationSettings
import ru.coffeepoint.feature.settings.ui.NotificationSettingsContent
import androidx.compose.runtime.*
import ru.coffeepoint.feature.settings.ui.SettingsScreen
import ru.coffeepoint.ui.theme.CoffeePointTheme

class NotificationSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsMenuOpensNotifications() {
        var opened = false
        var back = false
        compose.setContent {
            CoffeePointTheme {
                SettingsScreen(onBack = { back = true }, onOpenNotifications = { opened = true })
            }
        }
        compose.onNodeWithText("Уведомления").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun supplierTogglePersistsWithoutChangingManager() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("notification_settings", 0)
        val original = preferences.all
        try {
            preferences.edit().clear().commit()
            val settings = NotificationSettings(context)
            fun readValues() = UserRole.entries.associateWith { role ->
                NotificationOption.entries.associateWith { settings.isEnabled(role, it) }
            }
            compose.setContent {
                var values by remember { mutableStateOf(readValues()) }
                CoffeePointTheme {
                    NotificationSettingsContent({}, values, { role, option, value ->
                        settings.setEnabled(role, option, value)
                        values = readValues()
                    })
                }
            }
            compose.onNodeWithText("Настройка уведомлений").assertIsDisplayed()
            compose.onNodeWithText("Уведомления").assertDoesNotExist()
            compose.onNodeWithText("Снабженец").performClick()
            compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
                File(context.getExternalFilesDir(null), "notification-settings.png").outputStream().use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            compose.onNodeWithText("Нужно пополнить склад").performClick()
            compose.runOnIdle {
                val reloaded = NotificationSettings(context)
                assertTrue(reloaded.isEnabled(UserRole.SUPPLY_OPERATOR, NotificationOption.WAREHOUSE))
                assertTrue(reloaded.isEnabled(UserRole.MANAGER, NotificationOption.WAREHOUSE))
                assertFalse(reloaded.isEnabled(UserRole.SUPPLY_OPERATOR, NotificationOption.MIXERS))
            }
            compose.onNodeWithText("Управляющий").performClick()
            compose.onNodeWithText("Снабженец").performClick()
            compose.onNodeWithText("Нужно пополнить склад").performClick()
            compose.runOnIdle {
                assertFalse(NotificationSettings(context).isEnabled(UserRole.SUPPLY_OPERATOR, NotificationOption.WAREHOUSE))
            }
        } finally {
            preferences.edit().clear().apply {
                original.forEach { (key, value) -> when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                    is Long -> putLong(key, value)
                } }
            }.commit()
        }
    }
}
