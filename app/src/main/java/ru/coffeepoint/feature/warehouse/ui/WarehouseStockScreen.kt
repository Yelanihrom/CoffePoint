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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    canEditSettings: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit
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
                    Text("\u0421\u043a\u043b\u0430\u0434")
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
                },
                actions = {
                    if (canEditSettings) {
                        IconButton(
                            onClick = onSettings,
                            enabled = !viewModel.isEditing && !viewModel.isSending
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438"
                            )
                        }
                    }

                    IconButton(
                        onClick = viewModel::refreshWarehouseStock,
                        enabled = !viewModel.isEditing && !viewModel.isSending
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c"
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                if (viewModel.isEditing) {
                    viewModel.editableItems
                } else {
                    viewModel.items
                }
            ) { item ->
                WarehouseStockCard(
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
private fun WarehouseStockCard(
    item: WarehouseStockItem,
    isEditing: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
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

                Text(
                    text = item.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CounterButton(
                        text = "-",
                        onClick = onMinus
                    )

                    Text(
                        text = item.quantity.toString(),
                        modifier = Modifier.width(44.dp),
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = item.quantityText(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = item.quantityColor(),
                        textAlign = TextAlign.Start
                    )

                    item.limitsText().takeIf { text -> text.isNotBlank() }
                        ?.let { limits ->
                            Text(
                                text = limits,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                }
            }
        }
    }
}

private fun WarehouseStockItem.quantityText(): String {
    return if (maxQuantity > 0) {
        "$quantity / $maxQuantity"
    } else {
        quantity.toString()
    }
}

private fun WarehouseStockItem.limitsText(): String {
    return when {
        minQuantity > 0 && maxQuantity > 0 -> "\u041c\u0438\u043d. $minQuantity / \u041c\u0430\u043a\u0441. $maxQuantity"
        minQuantity > 0 -> "\u041c\u0438\u043d. $minQuantity"
        maxQuantity > 0 -> "\u041c\u0430\u043a\u0441. $maxQuantity"
        else -> ""
    }
}

@Composable
private fun WarehouseStockItem.quantityColor(): Color {
    if (minQuantity > 0 && quantity < minQuantity) {
        return WarehouseCriticalRed
    }

    if (minQuantity > 0 && maxQuantity > 0) {
        val warningThreshold = (minQuantity + maxQuantity) / 2.0

        return if (quantity < warningThreshold) {
            WarehouseWarningOrange
        } else {
            WarehouseOkGreen
        }
    }

    return MaterialTheme.colorScheme.onSurface
}

private val WarehouseCriticalRed = Color(0xFFE53935)
private val WarehouseWarningOrange = Color(0xFFF57C00)
private val WarehouseOkGreen = Color(0xFF2E7D32)
