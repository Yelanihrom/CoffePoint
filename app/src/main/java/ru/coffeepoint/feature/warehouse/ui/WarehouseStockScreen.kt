package ru.coffeepoint.feature.warehouse.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem
import ru.coffeepoint.feature.warehouse.presentation.WarehouseStockViewModel
import ru.coffeepoint.feature.supplycatalog.component.SupplyCatalogQuickInfoDialog
import ru.coffeepoint.shared.component.BottomActionBar
import ru.coffeepoint.shared.component.CounterButton
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseStockScreen(
    onBack: () -> Unit
) {
    val viewModel: WarehouseStockViewModel = viewModel()
    val selectedCatalogItemType = remember { mutableStateOf<String?>(null) }
    val selectedCatalogQuantityText = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    if (viewModel.isSending) {
        SyncSendingOverlay()
    }

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = viewModel.syncErrorMessage,
            onDismiss = viewModel::dismissSyncError,
            onRetry = {
                viewModel.retry(onBack)
            }
        )
    }

    selectedCatalogItemType.value?.let { itemType ->
        SupplyCatalogQuickInfoDialog(
            itemType = itemType,
            quantityText = selectedCatalogQuantityText.value,
            items = viewModel.supplyCatalogItems,
            onDismiss = {
                selectedCatalogItemType.value = null
                selectedCatalogQuantityText.value = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("\u0421\u043a\u043b\u0430\u0434")
                        Text(
                            text = "\u0418\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = if (viewModel.isEditing) {
                            viewModel::cancelEditing
                        } else {
                            onBack
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomActionBar(
                text = if (viewModel.isEditing) {
                    "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c"
                } else {
                    "\u041f\u043e\u043f\u043e\u043b\u043d\u0438\u0442\u044c \u0437\u0430\u043f\u0430\u0441\u044b"
                },
                onClick = {
                    if (viewModel.isEditing) {
                        viewModel.save(onBack)
                    } else {
                        viewModel.startEditing()
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                if (viewModel.isEditing) {
                    viewModel.editableItems
                } else {
                    viewModel.items
                }
            ) { item ->
                WarehouseStockRow(
                    item = item,
                    isEditing = viewModel.isEditing,
                    onMinus = {
                        viewModel.decrement(item.itemType)
                    },
                    onPlus = {
                        viewModel.increment(item.itemType)
                    },
                    onClick = {
                        selectedCatalogItemType.value = item.itemType
                        selectedCatalogQuantityText.value = item.quantity.toString()
                    }
                )
            }
        }
    }
}

@Composable
private fun WarehouseStockRow(
    item: WarehouseStockItem,
    isEditing: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onClick: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(34.dp)
                )

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CounterButton(
                        text = "-",
                        onClick = onMinus
                    )

                    Text(
                        text = item.quantity.toString(),
                        modifier = Modifier.padding(horizontal = 18.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    CounterButton(
                        text = "+",
                        onClick = onPlus
                    )
                }
            } else {
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
