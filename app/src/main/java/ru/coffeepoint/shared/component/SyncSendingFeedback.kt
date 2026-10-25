package ru.coffeepoint.shared.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun SyncSendingOverlay() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun SyncSendingErrorDialog(
    message: String = "\u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0443 \u0438\u043b\u0438 VPN",
    title: String = "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c \u0438\u0437\u043c\u0435\u043d\u0435\u043d\u0438\u044f",
    dismissText: String = "\u0417\u0430\u043a\u0440\u044b\u0442\u044c",
    retryText: String = "Повторить",
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AppDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(message)
        },
        confirmButton = {
            AppDialogActions(
                first = { AppDialogButton(dismissText, onClick = onDismiss, style = DialogButtonStyle.Secondary) },
                second = { AppDialogButton(retryText, onClick = onRetry) }
            )
        }
    )
}

fun savedLocallySyncMessage(
    message: String
): String {
    return if (message.contains("VPN", ignoreCase = true)) {
        "Проверьте подключение к интернету или отключите VPN\n\n" +
            "Изменения сохранены на устройстве и будут отправлены автоматически при появлении интернета"
    } else {
        message.trimEnd().trimEnd('.')
    }
}
