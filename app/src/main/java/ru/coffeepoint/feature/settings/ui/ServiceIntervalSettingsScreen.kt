package ru.coffeepoint.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.settings.presentation.ServiceIntervalSettingsViewModel
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.shared.component.AppNumberInput
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.core.sync.google.MAX_MAINTENANCE_SALES_LIMIT

@Composable
fun ServiceIntervalSettingsScreen(onBack: () -> Unit) {
    val model: ServiceIntervalSettingsViewModel = viewModel()
    ServiceIntervalSettingsContent(model.values, model::update, { model.save(onBack) }, onBack,
        model.valid && model.changed && !model.saving, model.saving || model.loading, model.error,
        model.salesValues, model::updateSales)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceIntervalSettingsContent(
    values: Map<TechServiceNode, String>, onChange: (TechServiceNode, String) -> Unit,
    onSave: () -> Unit, onBack: () -> Unit, canSave: Boolean, saving: Boolean = false, error: String? = null,
    salesValues: Map<TechServiceNode, String> = TechServiceNode.entries.associateWith { "0" },
    onSalesChange: (TechServiceNode, String) -> Unit = { _, _ -> }
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
                val sales = salesValues[node].orEmpty()
                val invalidSales = (sales.toLongOrNull() ?: -1L) !in 0..MAX_MAINTENANCE_SALES_LIMIT
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(node.title, style = MaterialTheme.typography.bodyLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AppNumberInput(value, { onChange(node, it) },
                            Modifier.weight(1f).testTag("service_days_${node.name}"),
                            label = "Дней", isError = invalid, enabled = !saving)
                        AppNumberInput(sales, { onSalesChange(node, it) },
                            Modifier.weight(1f).testTag("service_sales_${node.name}"),
                            label = "Продаж", isError = invalidSales, enabled = !saving)
                    }
                }
                if (invalid) Text("Укажите число дней больше 0", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
                if (invalidSales) Text("Укажите число продаж от 0 до $MAX_MAINTENANCE_SALES_LIMIT", color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
