package ru.coffeepoint.feature.service.refill.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.feature.service.component.ServiceStockCounters
import ru.coffeepoint.feature.service.refill.presentation.RefillStockViewModel
import ru.coffeepoint.shared.component.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RefillStockScreen(stationId: Int, role: UserRole, onBack: () -> Unit, onNext: () -> Unit) {
    val viewModel: RefillStockViewModel = viewModel()
    LaunchedEffect(stationId) { viewModel.loadStation(stationId) }
    val station = viewModel.station ?: return
    val inventory = viewModel.inventory ?: return
    if (viewModel.isSaving) SyncSendingOverlay()
    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            title = if (viewModel.hasSavedLocally) "Изменения сохранены на устройстве" else "Не удалось сохранить изменения",
            message = if (viewModel.hasSavedLocally) savedLocallySyncMessage(viewModel.syncErrorMessage) else viewModel.syncErrorMessage,
            dismissText = if (viewModel.hasSavedLocally) "Продолжить" else "Закрыть", retryText = "Повторить",
            onDismiss = { viewModel.dismissSyncError(); if (viewModel.hasSavedLocally) onNext() },
            onRetry = { viewModel.save(onNext) }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(station.station.name, style = MaterialTheme.typography.titleLarge)
                    Text("Шаг 1 из 2", style = MaterialTheme.typography.bodyMedium)
                }
            }, navigationIcon = {
                IconButton(onClick = onBack, enabled = !viewModel.isSaving) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            })
        },
        bottomBar = { BottomActionBar(text = "Далее", enabled = !viewModel.isSaving, onClick = { viewModel.save(onNext) }) }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ServiceStockCounters(inventory, station.settings, viewModel::changeQuantity,
                showMax = role != UserRole.OPERATOR, enabled = !viewModel.isSaving)
        }
    }
}
