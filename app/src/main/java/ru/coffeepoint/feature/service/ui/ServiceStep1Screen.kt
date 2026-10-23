package ru.coffeepoint.feature.service.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.feature.service.model.RefillSource
import ru.coffeepoint.feature.service.model.maximum
import ru.coffeepoint.feature.service.model.quantity
import ru.coffeepoint.feature.service.presentation.ServiceStep1ViewModel
import ru.coffeepoint.feature.station.component.StockSection
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.CounterRow
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.shared.component.savedLocallySyncMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceStep1Screen(
    stationId: Int,
    role: UserRole,
    onBack: () -> Unit,
    onNext: () -> Unit,
    stepLabel: String = "Шаг 1 из 2",
    actionText: String = "Далее",
    syncImmediatelyOnSave: Boolean = false,
    isSupplyServiceFlow: Boolean = false
) {
    val viewModel: ServiceStep1ViewModel = viewModel()
    LaunchedEffect(stationId, isSupplyServiceFlow) { viewModel.loadStation(stationId, isSupplyServiceFlow) }
    val station = viewModel.station ?: return
    val inventory = viewModel.displayInventory ?: return
    val isAdding = isSupplyServiceFlow && !viewModel.isReview
    val showAvailability = isAdding && viewModel.selectedSource == RefillSource.Warehouse
    val locked = isSupplyServiceFlow && viewModel.hasSavedLocally
    val effectiveLabel = if (isSupplyServiceFlow) {
        if (viewModel.isReview) "Шаг 1 из 3 · Проверка остатков" else "Шаг 1 из 3 · Пополнение"
    } else stepLabel
    val goBack = {
        if (isSupplyServiceFlow && viewModel.isReview && !locked) viewModel.backToRefill() else onBack()
    }
    BackHandler(enabled = isSupplyServiceFlow && viewModel.isReview && !locked && !viewModel.isSaving) {
        viewModel.backToRefill()
    }
    if (viewModel.isSaving) SyncSendingOverlay()
    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            title = if (viewModel.hasSavedLocally) "Изменения сохранены на устройстве" else "Не удалось сохранить изменения",
            message = if (viewModel.hasSavedLocally) savedLocallySyncMessage(viewModel.syncErrorMessage) else viewModel.syncErrorMessage,
            dismissText = if (viewModel.hasSavedLocally) "Готово" else "Закрыть",
            retryText = "Повторить сейчас",
            onDismiss = {
                viewModel.dismissSyncError()
                if (viewModel.hasSavedLocally) onNext()
            },
            onRetry = { viewModel.retrySave(syncImmediatelyOnSave, onNext) }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(station.station.name, style = MaterialTheme.typography.titleLarge)
                        Text(effectiveLabel, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = goBack, enabled = !viewModel.isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                text = if (isAdding) "Проверить остатки" else actionText,
                enabled = viewModel.canProceed,
                onClick = { viewModel.proceed(syncImmediatelyOnSave, onNext) }
            )
        }
    ) { padding ->
        val scroll = rememberScrollState()
        LaunchedEffect(viewModel.isReview) { scroll.scrollTo(0) }
        Column(
            modifier = Modifier.padding(padding).verticalScroll(scroll).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isSupplyServiceFlow) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (isAdding) "Сколько добавили?" else "Остаток после пополнения", style = MaterialTheme.typography.titleLarge)
                    if (isAdding) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!locked) expanded = !expanded }) {
                            OutlinedTextField(
                                value = viewModel.selectedSource.title,
                                onValueChange = {}, readOnly = true, enabled = !locked,
                                label = { Text("Источник списания") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                RefillSource.entries.forEach { source ->
                                    DropdownMenuItem(text = { Text(source.title) }, onClick = {
                                        viewModel.selectSource(source); expanded = false
                                    })
                                }
                            }
                        }
                        Text(if (showAvailability) "Добавлено / На складе" else "Добавлено",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("Источник: ${viewModel.selectedSource.title}", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (viewModel.shortages.isNotEmpty() || viewModel.warehouseError != null) {
                        Text(viewModel.warehouseError ?: "Не хватает остатков на складе", color = MaterialTheme.colorScheme.error)
                    }
                    if (locked) Text("Остатки сохранены", style = MaterialTheme.typography.bodyMedium)
                }
            }
            listOf("Расходники" to WarehouseItemType.entries.take(5), "Ингредиенты" to WarehouseItemType.entries.drop(5)).forEach { (title, types) ->
                StockSection(title = title) {
                    types.forEach { type ->
                        val line = viewModel.refillLines.firstOrNull { it.type == type }
                        val available = viewModel.warehouseQuantities[type.code] ?: 0
                        val valueColor = when {
                            !showAvailability -> Color.Unspecified
                            inventory.quantity(type) > available -> MaterialTheme.colorScheme.error
                            inventory.quantity(type) == 0 -> MaterialTheme.colorScheme.onSurface
                            else -> Color(0xFF43A86B)
                        }
                        CounterRow(
                            title = type.title, value = inventory.quantity(type),
                            max = if (showAvailability) available else station.settings.maximum(type),
                            showMax = if (isSupplyServiceFlow) showAvailability else role != UserRole.OPERATOR,
                            iconRes = type.iconRes,
                            valueColor = valueColor,
                            enabled = !locked,
                            modifier = Modifier.testTag("stock-${type.code}"),
                            onMinus = { viewModel.changeQuantity(type, -1) },
                            onPlus = { viewModel.changeQuantity(type, 1) }
                        )
                        if (isSupplyServiceFlow && viewModel.isReview && line != null) {
                            val difference = line.actual.toLong() - line.expected
                            Text(
                                text = "Добавлено: ${line.added} · Ожидалось: ${line.expected}" +
                                    if (difference != 0L) " · Корректировка: ${if (difference > 0) "+" else ""}$difference" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (difference != 0L) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
