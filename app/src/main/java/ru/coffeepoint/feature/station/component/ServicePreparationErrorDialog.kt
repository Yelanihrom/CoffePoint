package ru.coffeepoint.feature.station.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import ru.coffeepoint.shared.component.AppDialog
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.shared.component.AppDialogActions
import ru.coffeepoint.shared.component.DialogButtonStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ServicePreparationErrorDialog(
    lastUpdated: String,
    onRetry: () -> Unit,
    onContinueLocally: () -> Unit,
    onCancel: () -> Unit,
    hasPendingChanges: Boolean = false
) {
    AppDialog(
        onDismissRequest = onCancel,
        title = {
            Text("Не удалось получить актуальные данные", style = MaterialTheme.typography.headlineSmall)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Данные от: $lastUpdated", color = Color(0xFF43A86B))
                Text("Повторите запрос или продолжите работу с текущими данными")
                if (hasPendingChanges) {
                    Text("Есть локальные изменения. Они сохранены и не заменены данными сервера.")
                }
            }
        },
        confirmButton = {
            AppDialogActions(
                first = { AppDialogButton("Повторить", onClick = onRetry) },
                second = { AppDialogButton("Продолжить", onClick = onContinueLocally, style = DialogButtonStyle.Secondary) },
                cancel = { AppDialogButton("Отменить", onClick = onCancel, style = DialogButtonStyle.Cancel) }
            )
        }
    )
}
