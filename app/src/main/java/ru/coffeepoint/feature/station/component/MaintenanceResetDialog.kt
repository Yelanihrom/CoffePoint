package ru.coffeepoint.feature.station.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ru.coffeepoint.shared.component.AppDialog
import ru.coffeepoint.shared.component.AppDialogActions
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.shared.component.DialogButtonStyle

@Composable
fun MaintenanceResetDialog(date: String, onChoice: (Boolean) -> Unit, onDismiss: () -> Unit) {
    AppDialog(onDismissRequest = onDismiss,
        title = { Text("Обнулить счётчик продаж?") },
        text = { Text("Дата обслуживания: $date\nБез обнуления текущее значение сохранится до следующего расчёта") },
        confirmButton = {
            AppDialogActions(
                first = { AppDialogButton("Не обнулять", { onChoice(false) }, style = DialogButtonStyle.Secondary) },
                second = { AppDialogButton("Обнулить", { onChoice(true) }) },
                cancel = { AppDialogButton("Отменить", onDismiss, style = DialogButtonStyle.Cancel) })
        })
}
