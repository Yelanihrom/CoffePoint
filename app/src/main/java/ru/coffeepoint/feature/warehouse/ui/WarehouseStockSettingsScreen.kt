package ru.coffeepoint.feature.warehouse.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem
import ru.coffeepoint.feature.warehouse.presentation.WarehouseStockSettingsViewModel
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseStockSettingsScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val viewModel: WarehouseStockSettingsViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (viewModel.isSaving) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = viewModel.syncErrorMessage,
            onDismiss = viewModel::dismissSyncError,
            onRetry = {
                viewModel.retry(onSaved)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0441\u043a\u043b\u0430\u0434\u0430")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !viewModel.isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "\u041d\u0430\u0437\u0430\u0434"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                text = "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c",
                onClick = {
                    viewModel.save(onSaved)
                },
                enabled = !viewModel.isSaving
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(viewModel.items) { item ->
                WarehouseStockSettingsCard(
                    item = item,
                    onMinChanged = { value ->
                        viewModel.updateMin(item.itemType, value)
                    },
                    onMaxChanged = { value ->
                        viewModel.updateMax(item.itemType, value)
                    }
                )
            }
        }
    }
}

@Composable
private fun WarehouseStockSettingsCard(
    item: WarehouseStockItem,
    onMinChanged: (String) -> Unit,
    onMaxChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(34.dp)
                )

                Column {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "\u0421\u0435\u0439\u0447\u0430\u0441: ${item.quantity}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WarehouseLimitInput(
                    title = "\u041c\u0438\u043d.",
                    value = item.minQuantity,
                    onValueChanged = onMinChanged,
                    modifier = Modifier.weight(1f)
                )

                WarehouseLimitInput(
                    title = "\u041c\u0430\u043a\u0441\u0438\u043c\u0443\u043c",
                    value = item.maxQuantity,
                    onValueChanged = onMaxChanged,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WarehouseLimitInput(
    title: String,
    value: Int,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = value.toString(),
            onValueChange = { rawValue ->
                onValueChanged(
                    rawValue.filter { char ->
                        char.isDigit()
                    }
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        )
    }
}
