package ru.coffeepoint.shared.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.coffeepoint.core.update.AppUpdatePlatform
import ru.coffeepoint.core.update.AppUpdateViewModel

@Composable
fun AppUpdateHost(viewModel: AppUpdateViewModel) {
    val state = viewModel.state
    val update = state.update ?: return
    val context = LocalContext.current
    val installer = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    fun install() {
        val file = viewModel.state.file ?: return
        try { installer.launch(AppUpdatePlatform.installIntent(context, file)) }
        catch (_: Exception) { viewModel.installationError() }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (AppUpdatePlatform.canInstall(context)) install()
    }
    LaunchedEffect(state.file) {
        if (state.file != null && viewModel.consumeAutoInstall() && AppUpdatePlatform.canInstall(context)) install()
    }
    AlertDialog(
        onDismissRequest = viewModel::dismiss,
        title = { Text("Доступно обновление ${update.versionName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    state.downloading -> {
                        Text("Скачивание: ${state.progress}%")
                        LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                    state.file != null -> Text("Обновление скачано и проверено. Подтвердите установку в системном окне. При необходимости разрешите установку из этого источника.")
                    else -> Text("Обновить CoffeePoint? Данные приложения сохранятся.")
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            if (!state.downloading) TextButton(onClick = {
                if (state.file == null) viewModel.download()
                else if (AppUpdatePlatform.canInstall(context)) install()
                else try { permission.launch(AppUpdatePlatform.permissionIntent(context)) }
                catch (_: Exception) { viewModel.installationError() }
            }) { Text((if (state.file != null) "Установить" else if (state.error != null) "Повторить" else "Обновить").asActionButtonText()) }
        },
        dismissButton = { TextButton(onClick = viewModel::dismiss) { Text((if (state.downloading) "Отмена" else "Позже").asActionButtonText()) } }
    )
}
