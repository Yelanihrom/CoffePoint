package ru.coffeepoint.feature.dashboard.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.status.StationStatusSeverity
import ru.coffeepoint.core.status.statusSeverity
import ru.coffeepoint.feature.dashboard.component.StationCard
import ru.coffeepoint.feature.dashboard.component.SupplySummaryCard
import ru.coffeepoint.feature.dashboard.component.WarehouseStockCard
import ru.coffeepoint.feature.dashboard.model.DashboardMode
import ru.coffeepoint.feature.dashboard.model.SupplyItem
import ru.coffeepoint.feature.dashboard.model.isCritical
import ru.coffeepoint.feature.dashboard.model.isWarning
import ru.coffeepoint.feature.dashboard.model.supplyItems
import ru.coffeepoint.feature.dashboard.model.supplySummaryItems
import ru.coffeepoint.feature.dashboard.model.supplyOperatorTechServiceDashboardItems
import ru.coffeepoint.feature.dashboard.presentation.DashboardViewModel
import ru.coffeepoint.feature.supplycatalog.component.SupplyCatalogQuickInfoDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.shared.component.asActionButtonText

@Composable
fun DashboardScreen(
    role: UserRole,
    onStationClick: (Int) -> Unit,
    onAddStation: () -> Unit,
    onChangeRole: () -> Unit,
    onCloseApp: () -> Unit,
    onSupplyCatalog: () -> Unit,
    onWarehouseStock: () -> Unit,
    onMonthlyEarnings: () -> Unit
) {

    val viewModel: DashboardViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.loadStations()
    }

    val stations = viewModel.stations
    val isManager = role == UserRole.MANAGER
    val isSupplyOperator = role == UserRole.SUPPLY_OPERATOR
    val canViewMonthlyEarnings =
        role == UserRole.MANAGER || role == UserRole.OPERATOR
    val canViewSupplyCatalog =
        role == UserRole.MANAGER || role == UserRole.SUPPLY_OPERATOR
    var menuExpanded by remember { mutableStateOf(false) }
    var selectedMode by remember { mutableStateOf(DashboardMode.ALL_STATIONS) }
    var selectedSupplyFilter by remember { mutableStateOf(SupplyStockFilter.ALL) }
    var warehouseStockExpanded by remember { mutableStateOf(false) }
    var supplySummaryExpanded by remember { mutableStateOf(false) }
    var quickCatalogItemType by remember { mutableStateOf<String?>(null) }
    var quickCatalogQuantityText by remember { mutableStateOf<String?>(null) }
    val currentMode = when {
        isManager -> {
            when (selectedMode) {
                DashboardMode.SUPPLY -> DashboardMode.SUPPLY
                else -> DashboardMode.ALL_STATIONS
            }
        }

        isSupplyOperator -> DashboardMode.SUPPLY

        else -> {
            when (selectedMode) {
                DashboardMode.ALL_STATIONS,
                DashboardMode.REFILL -> selectedMode

                DashboardMode.SUPPLY -> DashboardMode.ALL_STATIONS
            }
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val isSupplyView = isSupplyOperator ||
        (isManager && currentMode == DashboardMode.SUPPLY)
    val supplyItemMatches: (SupplyItem) -> Boolean = when (selectedSupplyFilter) {
        SupplyStockFilter.ALL -> { _ -> true }
        SupplyStockFilter.WARNING -> { item -> item.isWarning || item.isCritical }
        SupplyStockFilter.CRITICAL -> { item -> item.isCritical }
    }

    val visibleStations = when (currentMode) {
        DashboardMode.ALL_STATIONS -> stations

        DashboardMode.REFILL -> stations.filter { detail ->
            detail.statusSeverity() != StationStatusSeverity.OK
        }

        DashboardMode.SUPPLY -> {
            if (isSupplyView) {
                stations.filter { detail ->
                    detail.supplyItems().any(supplyItemMatches) ||
                        (
                            selectedSupplyFilter == SupplyStockFilter.ALL &&
                                detail.supplyOperatorTechServiceDashboardItems().isNotEmpty()
                            )
                }
            } else {
                stations.filter { detail ->
                    detail.supplyItems().isNotEmpty()
                }
            }
        }

    }

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    LaunchedEffect(viewModel.showSalesRefreshSuccess) {
        if (viewModel.showSalesRefreshSuccess) {
            delay(5000)
            viewModel.dismissSalesRefreshSuccess()
        }
    }

    if (viewModel.showConnectionError) {

        AlertDialog(
            onDismissRequest = {
                viewModel.dismissConnectionError()
            },
            title = {
                Text("\u041e\u0448\u0438\u0431\u043a\u0430 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f")
            },
            text = {
                Text("\u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0443 \u0438 \u043e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u0435 VPN")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissConnectionError()
                    }
                ) {
                    Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c".asActionButtonText())
                }
            }
        )

    } else if (viewModel.showGoogleSheetsSyncError) {

        AlertDialog(
            onDismissRequest = {
                viewModel.dismissGoogleSheetsSyncError()
            },
            title = {
                Text("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u044c \u0432\u0441\u0435 \u0434\u0430\u043d\u043d\u044b\u0435")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("\u041c\u043e\u0436\u043d\u043e \u043e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u0442\u0430\u0442\u0438\u0441\u0442\u0438\u043a\u0443 \u043f\u0440\u043e\u0434\u0430\u0436 \u0431\u0435\u0437 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u0434\u0440\u0443\u0433\u0438\u0445 \u0434\u0430\u043d\u043d\u044b\u0445")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            onClick = {
                                viewModel.dismissGoogleSheetsSyncError()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("\u041e\u0442\u043c\u0435\u043d\u0430".asActionButtonText())
                        }

                        Button(
                            onClick = {
                                viewModel.refreshSalesWithoutGoogleSheets()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c".asActionButtonText())
                        }
                    }
                }
            },
            confirmButton = {}
        )

    } else if (viewModel.missingTerminalErrorText != null) {

        AlertDialog(
            onDismissRequest = {
                viewModel.dismissMissingTerminalError()
            },
            title = {
                Text("\u041d\u0435 \u0432\u0441\u0435 \u0434\u0430\u043d\u043d\u044b\u0435 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u044b")
            },
            text = {
                Text(viewModel.missingTerminalErrorText ?: "")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.dismissMissingTerminalError()
                    }
                ) {
                    Text("\u0417\u0430\u043a\u0440\u044b\u0442\u044c".asActionButtonText())
                }
            }
        )

    }

    if (viewModel.isRefreshingSales) {
        SyncSendingOverlay()
    }

    quickCatalogItemType?.let { itemType ->
        SupplyCatalogQuickInfoDialog(
            itemType = itemType,
            quantityText = quickCatalogQuantityText,
            items = viewModel.supplyCatalogItems,
            onDismiss = {
                quickCatalogItemType = null
                quickCatalogQuantityText = null
            }
        )
    }

    Scaffold(
        topBar = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onChangeRole
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "\u0421\u043c\u0435\u043d\u0438\u0442\u044c \u0440\u043e\u043b\u044c"
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "\u0421\u043f\u0438\u0441\u043e\u043a \u043a\u043e\u0444\u0435-\u0441\u0442\u0430\u043d\u0446\u0438\u0439",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = MaterialTheme.typography.headlineSmall.fontSize * 0.8f
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = viewModel.lastSalesRefreshText
                                ?.let { "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u043e: $it" }
                                ?: "\u041e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u043e: \u043d\u0435\u0442 \u0434\u0430\u043d\u043d\u044b\u0445",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    }

                    Row {

                        IconButton(
                            enabled = !viewModel.isRefreshingSales,
                            onClick = {
                                viewModel.refreshSales()
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c",
                                modifier = Modifier.rotate(
                                    if (viewModel.isRefreshingSales) rotation else 0f
                                )
                            )

                        }

                        IconButton(
                            onClick = {
                                menuExpanded = true
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "\u041c\u0435\u043d\u044e"
                            )

                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = {
                                menuExpanded = false
                            }
                        ) {

                            if (canViewMonthlyEarnings) {

                                DropdownMenuItem(
                                    text = {
                                        Text("\u0417\u0430\u0440\u0430\u0431\u043e\u0442\u043e\u043a")
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onMonthlyEarnings()
                                    }
                                )

                            }

                            if (isManager) {

                                DropdownMenuItem(
                                    text = {
                                        Text("\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0441\u0442\u0430\u043d\u0446\u0438\u044e")
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onAddStation()
                                    }
                                )

                            }

                            if (canViewSupplyCatalog) {

                                DropdownMenuItem(
                                    text = {
                                        Text("\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u0437\u0430\u043a\u0443\u043f\u043a\u0438")
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        onSupplyCatalog()
                                    }
                                )

                            }

                            DropdownMenuItem(
                                text = {
                                    Text("\u0412\u044b\u0439\u0442\u0438 \u0438\u0437 \u043f\u0440\u0438\u043b\u043e\u0436\u0435\u043d\u0438\u044f")
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCloseApp()
                                }
                            )
                        }

                    }

                }

                if (isManager) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {


                        FilterChip(
                            selected = currentMode == DashboardMode.ALL_STATIONS,
                            onClick = {
                                selectedMode = DashboardMode.ALL_STATIONS
                            },
                            label = {
                                Text("\u041e\u043f\u0435\u0440\u0430\u0442\u043e\u0440")
                            }
                        )

                        if (isManager) {

                            FilterChip(
                                selected = currentMode == DashboardMode.SUPPLY,
                                onClick = {
                                    selectedMode = DashboardMode.SUPPLY
                                },
                                label = {
                                    Text("\u0421\u043d\u0430\u0431\u0436\u0435\u043d\u0435\u0446")
                                }
                            )

                        }

                    }
                }

                if (isSupplyView) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SupplyFilterChip(
                            text = "\u0412\u0441\u0435 \u043e\u0441\u0442\u0430\u0442\u043a\u0438",
                            selected = selectedSupplyFilter == SupplyStockFilter.ALL,
                            onClick = {
                                selectedSupplyFilter = SupplyStockFilter.ALL
                            }
                        )

                        SupplyFilterChip(
                            text = "\u041f\u043e\u043b\u043e\u0432\u0438\u043d\u0430",
                            selected = selectedSupplyFilter == SupplyStockFilter.WARNING,
                            onClick = {
                                selectedSupplyFilter = SupplyStockFilter.WARNING
                            }
                        )

                        SupplyFilterChip(
                            text = "\u041a\u0440\u0438\u0442\u0438\u0447\u043d\u043e",
                            selected = selectedSupplyFilter == SupplyStockFilter.CRITICAL,
                            onClick = {
                                selectedSupplyFilter = SupplyStockFilter.CRITICAL
                            }
                        )
                    }
                }

            }

        },
        snackbarHost = {
            if (viewModel.showSalesRefreshSuccess) {
                Snackbar(
                    action = {
                        IconButton(
                            onClick = {
                                viewModel.dismissSalesRefreshSuccess()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "\u0417\u0430\u043a\u0440\u044b\u0442\u044c"
                            )
                        }
                    }
                ) {
                    Text("\u0423\u0441\u043f\u0435\u0448\u043d\u043e \u043e\u0431\u043d\u043e\u0432\u0438\u043b\u0438 \u0432\u0441\u0435 \u0434\u0430\u043d\u043d\u044b\u0435")
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            when (currentMode) {
                DashboardMode.ALL_STATIONS,
                DashboardMode.REFILL -> Unit

                DashboardMode.SUPPLY -> {

                    item {

                        WarehouseStockCard(
                            items = viewModel.warehouseStock,
                            expanded = warehouseStockExpanded,
                            onExpandedChange = { expanded ->
                                warehouseStockExpanded = expanded
                            },
                            onClick = onWarehouseStock,
                            onStockItemClick = { item ->
                                quickCatalogItemType = item.itemType
                                quickCatalogQuantityText = item.quantity.toString()
                            }
                        )

                    }

                    item {

                        SupplySummaryCard(
                            items = visibleStations
                                .supplySummaryItems()
                                .filter(supplyItemMatches),
                            expanded = supplySummaryExpanded,
                            onExpandedChange = { expanded ->
                                supplySummaryExpanded = expanded
                            },
                            onSupplyItemClick = { item ->
                                quickCatalogItemType = item.itemType
                                quickCatalogQuantityText = item.value
                            }
                        )

                    }

                }
            }

            items(visibleStations) { detail ->

                StationCard(
                    detail = detail,
                    mode = currentMode,
                    showSyncStatus = true,
                    showStationStatus = !isSupplyView,
                    showTerminalInfo = isManager && !isSupplyView,
                    showSupplyOperatorTechService = isSupplyView,
                    supplyItemFilter = supplyItemMatches,
                    onSupplyItemClick = { item ->
                        quickCatalogItemType = item.itemType
                        quickCatalogQuantityText = item.value
                    },
                    onClick = {
                        onStationClick(detail.station.id)
                    }
                )

            }

        }

    }

}

private enum class SupplyStockFilter {
    ALL,
    WARNING,
    CRITICAL
}

@Composable
private fun SupplyFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(text)
        }
    )
}

