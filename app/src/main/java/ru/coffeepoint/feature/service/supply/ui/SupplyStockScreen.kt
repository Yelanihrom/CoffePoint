package ru.coffeepoint.feature.service.supply.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.service.component.ServiceStockCounters
import ru.coffeepoint.feature.service.model.withQuantity
import ru.coffeepoint.feature.service.supply.model.RefillSource
import ru.coffeepoint.feature.service.supply.presentation.SupplyStockViewModel
import ru.coffeepoint.shared.component.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyStockScreen(stationId: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val viewModel: SupplyStockViewModel = viewModel()
    LaunchedEffect(stationId) { viewModel.loadStation(stationId) }
    val station = viewModel.station ?: return
    val isRefilling = viewModel.isRefilling
    val locked = viewModel.hasSavedLocally
    val colors = MaterialTheme.colorScheme
    val inventory = viewModel.refillLines.fold(station.inventory) { result, line ->
        result.withQuantity(line.type, if (isRefilling) line.added else line.verified)
    }
    val goBack = {
        if (isRefilling && !locked) viewModel.backToVerification() else onBack()
    }
    BackHandler(enabled = isRefilling && !locked && !viewModel.isSaving) { viewModel.backToVerification() }
    if (viewModel.isSaving) SyncSendingOverlay()
    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            title = if (locked) "Изменения сохранены на устройстве" else "Не удалось сохранить изменения",
            message = if (locked) savedLocallySyncMessage(viewModel.syncErrorMessage) else viewModel.syncErrorMessage,
            dismissText = if (locked) "Продолжить" else "Закрыть", retryText = "Повторить",
            onDismiss = { viewModel.dismissSyncError(); if (locked) onNext() },
            onRetry = { viewModel.retrySave(onNext) }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(station.station.name, style = MaterialTheme.typography.titleLarge)
                    Text(if (isRefilling) "Шаг 2 из 4 · Пополнение" else "Шаг 1 из 4 · Проверка остатков",
                        style = MaterialTheme.typography.bodyMedium)
                }
            }, navigationIcon = {
                IconButton(onClick = goBack, enabled = !viewModel.isSaving) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            })
        },
        bottomBar = {
            BottomActionBar(text = "Далее", enabled = viewModel.canProceed, onClick = { viewModel.proceed(onNext) })
        }
    ) { padding ->
        val scroll = rememberScrollState()
        LaunchedEffect(isRefilling) { scroll.scrollTo(0) }
        Column(Modifier.padding(padding).verticalScroll(scroll).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (isRefilling) "Сколько добавили?" else "Проверка остатков", style = MaterialTheme.typography.titleLarge)
                if (isRefilling) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!locked) expanded = !expanded }) {
                        OutlinedTextField(value = viewModel.selectedSource.title, onValueChange = {}, readOnly = true,
                            enabled = !locked, label = { Text("Источник списания") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            RefillSource.entries.forEach { source ->
                                DropdownMenuItem(text = { Text(source.title) }, onClick = {
                                    viewModel.selectSource(source); expanded = false
                                })
                            }
                        }
                    }
                    Text("Добавлено", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (viewModel.shortages.isNotEmpty() || viewModel.warehouseError != null) {
                        Text(viewModel.warehouseError ?: "Не хватает остатков на складе", color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                        Text("Проверить и указать фактические остатки до пополнения",
                            modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (locked) Text("Остатки сохранены", style = MaterialTheme.typography.bodyMedium)
            }
            ServiceStockCounters(inventory, station.settings, viewModel::changeQuantity, enabled = !locked,
                valueColor = { type ->
                    val line = viewModel.refillLines.first { it.type == type }
                    when {
                        !isRefilling || viewModel.selectedSource != RefillSource.Warehouse -> Color.Unspecified
                        type.code in viewModel.shortages -> colors.error
                        line.added == 0 -> colors.onSurface
                        else -> Color(0xFF43A86B)
                    }
                }, belowRow = { type ->
                    val line = viewModel.refillLines.first { it.type == type }
                    if (isRefilling) {
                        Text("После пополнения: ${line.actual}", style = MaterialTheme.typography.bodySmall)
                    } else if (line.verified != line.original) {
                        val difference = line.verified.toLong() - line.original
                        Text("Корректировка: ${if (difference > 0) "+" else ""}$difference",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                })
        }
    }
}
