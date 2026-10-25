package ru.coffeepoint

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.core.data.local.entity.SupplyCatalogEntity
import ru.coffeepoint.feature.supplycatalog.component.SupplyCatalogQuickInfoDialog
import ru.coffeepoint.shared.component.*
import ru.coffeepoint.ui.theme.CoffeePointTheme

class AppDialogsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun longContentScrollsWithoutHidingActions() {
        compose.setContent {
            CoffeePointTheme {
                AppDialog(
                    modifier = Modifier.width(300.dp),
                    onDismissRequest = {},
                    title = { Text("Проверка длинного содержимого") },
                    text = { Column { repeat(40) { Text("Строка $it: данные станции") } } },
                    confirmButton = { AppDialogButton("Закрыть", onClick = {}) }
                )
            }
        }
        compose.onNodeWithText("ЗАКРЫТЬ").assertIsDisplayed()
        compose.onNodeWithText("Строка 39: данные станции").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ЗАКРЫТЬ").assertIsDisplayed()
        screenshot("dialog-long-content.png")
    }

    @Test fun destructiveActionAndCancelAreIndependentAtLargerFont() {
        var deleted = 0
        var cancelled = 0
        compose.setContent {
            CoffeePointTheme {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                    AppDialog(
                        modifier = Modifier.width(300.dp), onDismissRequest = { cancelled++ },
                        title = { Text("Удалить станцию?") }, text = { Text("Станция будет удалена без возможности восстановления") },
                        confirmButton = { AppDialogButton("Удалить", onClick = { deleted++ }, style = DialogButtonStyle.Destructive) },
                        dismissButton = { AppDialogButton("Отменить", onClick = { cancelled++ }, style = DialogButtonStyle.Secondary) }
                    )
                }
            }
        }
        val cancel = compose.onNodeWithText("ОТМЕНИТЬ").assertIsDisplayed()
        val delete = compose.onNodeWithText("УДАЛИТЬ").assertIsDisplayed()
        val cancelBounds = cancel.fetchSemanticsNode().boundsInRoot
        val deleteBounds = delete.fetchSemanticsNode().boundsInRoot
        assertTrue(cancelBounds.right < deleteBounds.left)
        assertEquals(cancelBounds.top, deleteBounds.top, 1f)
        cancel.performClick()
        compose.runOnIdle { assertEquals(0, deleted); assertEquals(1, cancelled) }
        delete.performClick()
        compose.runOnIdle { assertEquals(1, deleted) }
        screenshot("dialog-delete.png")
    }

    @Test fun disabledPrimaryActionDoesNotExecute() {
        var saves = 0
        compose.setContent {
            CoffeePointTheme {
                AppDialog(
                    onDismissRequest = {}, title = { Text("Сохранение") }, text = { Text("Дождитесь завершения операции") },
                    confirmButton = { AppDialogButton("Сохранить", onClick = { saves++ }, enabled = false) }
                )
            }
        }
        compose.onNodeWithText("СОХРАНИТЬ").assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, saves) }
    }

    @Test fun catalogueWithManyOptionsKeepsCloseVisible() {
        val items = (1..20).map { id -> SupplyCatalogEntity(
            id = id, itemType = "cups", title = "Вариант $id", supplier = "", article = "$id", url = "", maxPrice = 100
        ) }
        compose.setContent {
            CoffeePointTheme { SupplyCatalogQuickInfoDialog("cups", "8", items, onDismiss = {}) }
        }
        compose.onNodeWithText("ЗАКРЫТЬ").assertIsDisplayed()
        compose.onNodeWithText("Вариант 20").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ЗАКРЫТЬ").assertIsDisplayed()
        screenshot("dialog-catalogue.png")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test fun calendarUsesCommonActionsAndKeepsDisabledSelection() {
        var cancelled = false
        compose.setContent {
            CoffeePointTheme {
                val state = rememberDatePickerState()
                AppDatePickerDialog(
                    onDismissRequest = { cancelled = true },
                    confirmButton = { AppDialogButton("Выбрать", onClick = {}, enabled = state.selectedDateMillis != null) },
                    dismissButton = { AppDialogButton("Отменить", onClick = { cancelled = true }, style = DialogButtonStyle.Secondary) }
                ) { DatePicker(state = state) }
            }
        }
        compose.onNodeWithText("ВЫБРАТЬ").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("ОТМЕНИТЬ").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(cancelled) }
        screenshot("dialog-calendar.png")
    }

    @Test fun timeSelectionPreservesValueAndDoesNotSaveOnCancel() {
        var saved = ""
        var cancelled = false
        compose.setContent {
            CoffeePointTheme {
                AppTimePickerDialog("09:30", onDismiss = { cancelled = true }, onSave = { saved = it })
            }
        }
        compose.onNodeWithText("ОТМЕНИТЬ").performClick()
        compose.runOnIdle { assertTrue(cancelled); assertEquals("", saved) }
        compose.onNodeWithText("ВЫБРАТЬ").performClick()
        compose.runOnIdle { assertEquals("09:30", saved) }
        screenshot("dialog-time.png")
    }

    private fun screenshot(name: String) {
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
