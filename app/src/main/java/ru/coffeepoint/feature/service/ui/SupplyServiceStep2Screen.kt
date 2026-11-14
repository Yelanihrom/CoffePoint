package ru.coffeepoint.feature.service.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.service.component.ChecklistRow
import ru.coffeepoint.feature.service.presentation.ServiceStep1ViewModel
import ru.coffeepoint.feature.service.presentation.SupplyServiceStep2ViewModel
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.shared.component.savedLocallySyncMessage
import ru.coffeepoint.shared.component.AppDialog
import ru.coffeepoint.shared.component.AppDialogButton
import ru.coffeepoint.core.auth.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplyServiceStep2Screen(
    stationId: Int,
    role: UserRole,
    onBack: () -> Unit,
    onFinish: () -> Unit
) {

    val stationViewModel: ServiceStep1ViewModel = viewModel()
    stationViewModel.loadStation(stationId)
    val station = stationViewModel.station ?: return

    val viewModel: SupplyServiceStep2ViewModel = viewModel()
    viewModel.loadStation(stationId)

    if (viewModel.isSaving) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = savedLocallySyncMessage(viewModel.syncErrorMessage),
            dismissText = "\u0413\u043e\u0442\u043e\u0432\u043e",
            retryText = "Повторить",
            onDismiss = {
                viewModel.dismissSyncError()
                onFinish()
            },
            onRetry = {
                viewModel.finishSupplyService(stationId, role) {
                    onFinish()
                }
            }
        )
    }

    viewModel.localSaveError?.let { message ->
        AppDialog(
            onDismissRequest = viewModel::dismissLocalSaveError,
            title = { Text("Не удалось сохранить обслуживание") },
            text = { Text(message) },
            confirmButton = { AppDialogButton("Закрыть", onClick = viewModel::dismissLocalSaveError) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = station.station.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Шаг 4 из 4",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        enabled = !viewModel.isSaving,
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                text = "\u0417\u0430\u0432\u0435\u0440\u0448\u0438\u0442\u044c \u043e\u0431\u0441\u043b\u0443\u0436\u0438\u0432\u0430\u043d\u0438\u0435",
                enabled = viewModel.canFinish(),
                onClick = {
                    viewModel.finishSupplyService(stationId, role) {
                        onFinish()
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Обслуживание",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    viewModel.checklist.forEachIndexed { index, item ->
                        ChecklistRow(
                            checked = item.checklistItem.checked,
                            title = item.checklistItem.title,
                            hint = item.checklistItem.description,
                            warningText = item.warningText,
                            hintStartPadding = 12.dp,
                            hintEmphasis = item.hintEmphasis,
                            onCheckedChange = { checked ->
                                viewModel.toggleChecklist(index, checked)
                            }
                        )
                    }
                }
            }
        }
    }

}
