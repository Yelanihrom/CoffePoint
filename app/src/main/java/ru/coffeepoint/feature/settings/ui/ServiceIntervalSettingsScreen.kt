package ru.coffeepoint.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.settings.presentation.ServiceIntervalSettingsViewModel
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.shared.component.AppNumberInput
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingOverlay

@Composable
fun ServiceIntervalSettingsScreen(onBack: () -> Unit) {
    val model: ServiceIntervalSettingsViewModel = viewModel()
    ServiceIntervalSettingsContent(model.values, model::update, { model.save(onBack) }, onBack,
        model.valid && model.changed && !model.saving, model.saving, model.error)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceIntervalSettingsContent(
    values: Map<TechServiceNode, String>, onChange: (TechServiceNode, String) -> Unit,
    onSave: () -> Unit, onBack: () -> Unit, canSave: Boolean, saving: Boolean = false, error: String? = null
) {
    if (saving) SyncSendingOverlay()
    Scaffold(topBar = {
        TopAppBar(title = { Text("Обслуживание узлов", maxLines = 1, overflow = TextOverflow.Ellipsis) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
        })
    }, bottomBar = { BottomActionBar("Сохранить", onSave, enabled = canSave && !saving) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (error != null) item { Text(error, color = MaterialTheme.colorScheme.error) }
            items(TechServiceNode.entries, key = { it.name }) { node ->
                val value = values[node].orEmpty()
                val invalid = (value.toIntOrNull() ?: 0) <= 0
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val fieldModifier = Modifier.testTag("service_days_${node.name}")
                    if (maxWidth < 128.dp + (150 * LocalDensity.current.fontScale).dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(node.title, style = MaterialTheme.typography.bodyLarge)
                            AppNumberInput(value, { onChange(node, it) }, fieldModifier,
                                label = "Дней", isError = invalid, enabled = !saving)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(node.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            AppNumberInput(value, { onChange(node, it) }, fieldModifier.width(112.dp),
                                label = "Дней", isError = invalid, enabled = !saving)
                        }
                    }
                }
                if (invalid) Text("Укажите число дней больше 0", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
