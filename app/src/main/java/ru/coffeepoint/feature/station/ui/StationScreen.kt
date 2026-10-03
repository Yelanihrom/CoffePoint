package ru.coffeepoint.feature.station.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.coffeepoint.R
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.status.StationStatusSeverity
import ru.coffeepoint.core.status.statusSeverity
import ru.coffeepoint.feature.dashboard.model.TechServiceDashboardItem
import ru.coffeepoint.feature.dashboard.model.supplyOperatorTechServiceDashboardItems
import ru.coffeepoint.feature.station.component.StockCard
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.TechServiceNode
import ru.coffeepoint.feature.station.model.hasConnectionSyncError
import ru.coffeepoint.feature.station.model.hasConflictSyncError
import ru.coffeepoint.feature.station.model.hasSyncError
import ru.coffeepoint.feature.station.model.syncPartTitles
import ru.coffeepoint.feature.station.model.visitScheduleText
import ru.coffeepoint.feature.station.presentation.StationViewModel
import ru.coffeepoint.feature.supplycatalog.component.SupplyCatalogQuickInfoDialog
import ru.coffeepoint.shared.component.PrimaryActionButton
import ru.coffeepoint.shared.component.SyncSendingErrorDialog
import ru.coffeepoint.shared.component.SyncSendingOverlay
import ru.coffeepoint.shared.component.SyncStatusIcon
import ru.coffeepoint.shared.component.asActionButtonText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val MIXERS_SERVICE_INTERVAL_DAYS = 30
private const val LIQUID_WASTE_CONTAINER_SERVICE_INTERVAL_DAYS = 60
private const val BREW_UNIT_SERVICE_INTERVAL_DAYS = 360
private const val EXHAUST_SERVICE_INTERVAL_DAYS = 360
private const val EXPANSION_TANK_SERVICE_INTERVAL_DAYS = 360
private const val COFFEE_GRINDER_SERVICE_INTERVAL_DAYS = 720

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(
    stationId: Int,
    role: UserRole,
    onBack: () -> Unit,
    onStartService: () -> Unit,
    onStartSupplyService: () -> Unit,
    onSettings: () -> Unit
) {

    val viewModel: StationViewModel = viewModel()

    viewModel.loadStation(stationId)

    val detail = viewModel.station ?: return
    val isManager = role == UserRole.MANAGER
    val isSupplyOperator = role == UserRole.SUPPLY_OPERATOR

    var showRentDialog by remember {
        mutableStateOf(false)
    }

    var showRentDateError by remember {
        mutableStateOf(false)
    }

    var editingTechServiceItem by remember {
        mutableStateOf<StationServiceItem?>(null)
    }

    var showTechServiceDateError by remember {
        mutableStateOf(false)
    }

    var isServiceSectionExpanded by remember {
        mutableStateOf(true)
    }

    var quickCatalogItemType by remember { mutableStateOf<String?>(null) }
    var quickCatalogQuantityText by remember { mutableStateOf<String?>(null) }

    if (showRentDialog && isManager) {
        RentDateDialog(
            rentPaidUntil = detail.station.rentPaidUntil,
            showError = showRentDateError,
            onDismiss = {
                showRentDialog = false
                showRentDateError = false
            },
            onSave = { selectedDate ->
                viewModel.updateRentPaidUntil(
                    date = selectedDate,
                    onSuccess = {
                        showRentDialog = false
                        showRentDateError = false
                    },
                    onError = {
                        showRentDateError = true
                    }
                )
            }
        )
    }

    if (editingTechServiceItem != null && isManager) {
        val item = editingTechServiceItem ?: return

        TechServiceDateDialog(
            serviceDate = item.date,
            showError = showTechServiceDateError,
            onDismiss = {
                editingTechServiceItem = null
                showTechServiceDateError = false
            },
            onSave = { selectedDate ->
                viewModel.updateTechServiceDate(
                    node = item.node ?: return@TechServiceDateDialog,
                    date = selectedDate,
                    onSuccess = {
                        editingTechServiceItem = null
                        showTechServiceDateError = false
                    },
                    onError = {
                        showTechServiceDateError = true
                    }
                )
            }
        )
    }

    if (viewModel.isSaving) {
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

    if (viewModel.showSyncError) {
        SyncSendingErrorDialog(
            message = viewModel.syncErrorMessage,
            onDismiss = {
                viewModel.dismissSyncError()
            },
            onRetry = {
                viewModel.retrySync()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(detail.station.name)
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    if (isManager) {
                        IconButton(
                            onClick = onSettings
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Настройки станции"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                if (isManager) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .padding(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = 12.dp
                            )
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimaryActionButton(
                            text = "Начать\nзаправку",
                            onClick = onStartService,
                            modifier = Modifier.weight(1f)
                        )

                        PrimaryActionButton(
                            text = "Начать\nобслуживание",
                            onClick = onStartSupplyService,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    PrimaryActionButton(
                        text = if (isSupplyOperator) {
                            "Начать обслуживание"
                        } else {
                            "Начать заправку"
                        },
                        onClick = if (isSupplyOperator) {
                            onStartSupplyService
                        } else {
                            onStartService
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .padding(
                                start = 16.dp,
                                top = 8.dp,
                                end = 16.dp,
                                bottom = 12.dp
                            )
                            .navigationBarsPadding()
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            StationInfoPanel(
                terminalText = if (isManager) {
                    detail.station.terminalId?.toString() ?: "Не указан"
                } else {
                    null
                },
                addressText = detail.station.address.ifBlank { "Не указан" },
                scheduleText = detail.station.visitScheduleText()
                    .ifBlank { "Не указаны" },
                rentPaidUntil = if (isManager) {
                    detail.station.rentPaidUntil
                } else {
                    null
                },
                onRentClick = if (isManager) {
                    {
                        showRentDialog = true
                    }
                } else {
                    null
                }
            )

            SyncStatusText(
                syncParts = detail.syncPartTitles(),
                hasError = detail.hasSyncError(),
                hasConflictError = detail.hasConflictSyncError(),
                hasConnectionError = detail.hasConnectionSyncError(),
                onRetryClick = {
                    viewModel.retrySync()
                }
            )

            if (!isSupplyOperator) {
                StationStatusBar(detail = detail)
            }

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                item {
                    if (isManager) {
                        StationServiceCard(
                            items = detail.managerServiceItems(),
                            expanded = isServiceSectionExpanded,
                            onToggleExpanded = {
                                isServiceSectionExpanded = !isServiceSectionExpanded
                            },
                            onItemClick = { item ->
                                editingTechServiceItem = item
                            }
                        )
                    } else if (isSupplyOperator) {
                        StationServiceCard(
                            items = detail.supplyOperatorTechServiceDashboardItems()
                                .map { item ->
                                    StationServiceItem(
                                        title = item.title,
                                        iconRes = supplyOperatorServiceIconRes(item.title),
                                        isOverdue = true
                                    )
                                },
                            expanded = isServiceSectionExpanded,
                            onToggleExpanded = {
                                isServiceSectionExpanded = !isServiceSectionExpanded
                            },
                            onItemClick = null
                        )
                    }

                    StockCard(
                        detail = detail,
                        onStockItemClick = { itemType, quantityText ->
                            quickCatalogItemType = itemType
                            quickCatalogQuantityText = quantityText
                        }
                    )
                }
            }
        }
    }

}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StationServiceCard(
    items: List<StationServiceItem>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onItemClick: ((StationServiceItem) -> Unit)?
) {

    if (items.isEmpty()) return

    val hasOverdueItems = items.any { item -> item.isOverdue }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Обслуживание",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (hasOverdueItems) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                IconButton(
                    onClick = onToggleExpanded
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null
                    )
                }
            }

            HorizontalDivider()

            if (expanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        StationServiceChip(
                            item = item,
                            onClick = onItemClick
                        )
                    }
                }
            }

        }

    }

}

@Composable
private fun StationServiceChip(
    item: StationServiceItem,
    onClick: ((StationServiceItem) -> Unit)?
) {

    Surface(
        modifier = if (onClick != null) {
            Modifier.clickable {
                onClick(item)
            }
        } else {
            Modifier
        },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.iconRes != null) {
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isOverdue) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

}

private data class StationServiceItem(
    val title: String,
    val node: TechServiceNode? = null,
    val date: String = "",
    val iconRes: Int? = null,
    val serviceIntervalDays: Int = 0,
    val isOverdue: Boolean = false
)

private fun StationDetail.managerServiceItems(): List<StationServiceItem> {
    return listOf(
        StationServiceItem(
            title = "Миксеры",
            node = TechServiceNode.MIXERS,
            date = station.mixersServiceDate,
            iconRes = R.drawable.ic_tech_mixers,
            serviceIntervalDays = MIXERS_SERVICE_INTERVAL_DAYS
        ),
        StationServiceItem(
            title = "Кофемолка",
            node = TechServiceNode.COFFEE_GRINDER,
            date = station.coffeeGrinderServiceDate,
            iconRes = R.drawable.ic_tech_coffee_grinder,
            serviceIntervalDays = COFFEE_GRINDER_SERVICE_INTERVAL_DAYS
        ),
        StationServiceItem(
            title = "Контейнер жидких отходов",
            node = TechServiceNode.LIQUID_WASTE_CONTAINER,
            date = station.liquidWasteContainerServiceDate,
            iconRes = R.drawable.ic_tech_liquid_waste_container,
            serviceIntervalDays = LIQUID_WASTE_CONTAINER_SERVICE_INTERVAL_DAYS
        ),
        StationServiceItem(
            title = "УЗК",
            node = TechServiceNode.BREW_UNIT,
            date = station.brewUnitServiceDate,
            iconRes = R.drawable.ic_tech_brew_unit,
            serviceIntervalDays = BREW_UNIT_SERVICE_INTERVAL_DAYS
        ),
        StationServiceItem(
            title = "Вытяжка",
            node = TechServiceNode.EXHAUST,
            date = station.exhaustServiceDate,
            iconRes = R.drawable.ic_tech_exhaust,
            serviceIntervalDays = EXHAUST_SERVICE_INTERVAL_DAYS
        ),
        StationServiceItem(
            title = "Расширительный бочок",
            node = TechServiceNode.EXPANSION_TANK,
            date = station.expansionTankServiceDate,
            iconRes = R.drawable.ic_tech_expansion_tank,
            serviceIntervalDays = EXPANSION_TANK_SERVICE_INTERVAL_DAYS
        )
    ).map { item ->
        item.copy(
            isOverdue = isTechServiceOverdue(
                date = item.date,
                serviceIntervalDays = item.serviceIntervalDays
            )
        )
    }
}

private fun supplyOperatorServiceIconRes(title: String): Int? {
    return when (title) {
        "Миксеры" -> R.drawable.ic_tech_mixers
        "Контейнер жидких отходов" -> R.drawable.ic_tech_liquid_waste_container
        else -> null
    }
}

private fun isTechServiceOverdue(
    date: String,
    serviceIntervalDays: Int
): Boolean {

    if (serviceIntervalDays <= 0) return false

    val serviceDate = parseRentDateToMillis(date) ?: return false
    val dueDate = serviceDate + TimeUnit.DAYS.toMillis(
        serviceIntervalDays.toLong()
    )

    return dueDate <= todayStartMillis()

}

@Composable
private fun StationStatusBar(
    detail: StationDetail
) {

    val backgroundColor = when (detail.statusSeverity()) {
        StationStatusSeverity.CRITICAL -> Color(0xFFD32F2F)
        StationStatusSeverity.WARNING -> Color(0xFFF57C00)
        StationStatusSeverity.OK -> Color(0xFF2E7D32)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${detail.station.salesAfterService} продаж",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = detail.station.lastServiceDate.ifBlank {
                            "нет данных"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "последняя заправка",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

}

@Composable
private fun SyncStatusText(
    syncParts: List<String>,
    hasError: Boolean,
    hasConflictError: Boolean,
    hasConnectionError: Boolean,
    onRetryClick: () -> Unit
) {

    if (syncParts.isEmpty()) return

    var expanded by remember {
        mutableStateOf(false)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        val statusColor = if (hasError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.tertiary
        }

        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            )
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.weight(1f)
                ) {

                    SyncStatusIcon(
                        contentDescription = null,
                        arrowTint = statusColor,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .padding(top = 2.dp)
                            .size(33.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "Синхронизация",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (hasError) {
                                "Не удалось сохранить изменения"
                            } else {
                                "Ожидает отправки изменений"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )

                    }

                }

                IconButton(
                    onClick = {
                        expanded = !expanded
                    }
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (expanded) {
                            "Скрыть описание"
                        } else {
                            "Показать описание"
                        }
                    )
                }

            }

            if (expanded) {
                Text(
                    text = syncDescriptionText(
                        hasError = hasError,
                        hasConflictError = hasConflictError,
                        hasConnectionError = hasConnectionError
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 32.dp,
                        top = 8.dp
                    )
                )
            }

            OutlinedButton(
                onClick = onRetryClick,
                modifier = Modifier
                    .padding(
                        start = 32.dp,
                        top = 10.dp
                    )
            ) {
                Text("Повторить отправку".asActionButtonText())
            }

        }

    }

}

private fun syncDescriptionText(
    hasError: Boolean,
    hasConflictError: Boolean,
    hasConnectionError: Boolean
): String {
    if (!hasError) {
        return "Изменения сохранены на устройстве и будут автоматически отправлены при появлении интернета."
    }

    return when {
        hasConflictError ->
            "На сервере сохранены более свежие данные. Обратитесь к управляющему."

        hasConnectionError ->
            "Проверьте подключение к интернету или отключите VPN. При возобновление связи изменения будут отправлены автоматически"

        else ->
            "Не удалось сохранить изменения. Обратитесь к управляющему."
    }
}

@Composable
private fun StationInfoPanel(
    terminalText: String?,
    addressText: String,
    scheduleText: String,
    rentPaidUntil: String?,
    onRentClick: (() -> Unit)?
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 10.dp
            )
        ) {

            val rows = buildList {
                terminalText?.let { value ->
                    add(
                        StationInfoItem(
                            icon = Icons.Default.Info,
                            label = "Терминал",
                            value = value
                        )
                    )
                }
                add(
                    StationInfoItem(
                        icon = Icons.Default.Place,
                        label = "Адрес",
                        value = addressText
                    )
                )
                add(
                    StationInfoItem(
                        icon = Icons.Default.AccessTime,
                        label = "Часы посещения",
                        value = scheduleText
                    )
                )
                rentPaidUntil?.let { value ->
                    add(
                        StationInfoItem(
                            icon = Icons.Default.CalendarMonth,
                            label = "Аренда оплачена до",
                            value = value.ifBlank { "не указано" },
                            isError = isRentDueOrOverdue(value),
                            actionText = "Обновить",
                            onClick = onRentClick
                        )
                    )
                }
            }

            rows.forEachIndexed { index, item ->
                StationInfoRow(item = item)

                if (index != rows.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }

        }

    }

}

@Composable
private fun StationInfoRow(
    item: StationInfoItem
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (item.onClick != null) {
            Modifier
                .fillMaxWidth()
                .clickable { item.onClick.invoke() }
        } else {
            Modifier.fillMaxWidth()
        }
    ) {

        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = item.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (item.isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (item.actionText != null && item.onClick != null) {
            TextButton(
                onClick = item.onClick,
                modifier = Modifier
                    .height(32.dp)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = item.actionText,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

    }

}

private data class StationInfoItem(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val isError: Boolean = false,
    val actionText: String? = null,
    val onClick: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RentDateDialog(
    rentPaidUntil: String,
    showError: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {

    val todayMillis = todayStartMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseRentDateToMillis(rentPaidUntil)
            ?: todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {
                return utcTimeMillis >= todayMillis
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null &&
                    datePickerState.selectedDateMillis!! >= todayMillis,
                onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                        ?.let { millis ->
                            formatRentDate(millis)
                        }
                        ?: return@TextButton

                    onSave(selectedDate)
                }
            ) {
                Text("Сохранить".asActionButtonText())
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Отмена".asActionButtonText())
            }
        }
    ) {
        Column {
            DatePicker(
                state = datePickerState
            )

            if (showError) {
                Text(
                    text = "Дата не может быть меньше текущей даты",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechServiceDateDialog(
    serviceDate: String,
    showError: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {

    val todayMillis = todayStartMillis()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = parseRentDateToMillis(serviceDate)
            ?: todayMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {
                return utcTimeMillis <= todayMillis
            }
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null &&
                    datePickerState.selectedDateMillis!! <= todayMillis,
                onClick = {
                    val selectedDate = datePickerState.selectedDateMillis
                        ?.let { millis ->
                            formatRentDate(millis)
                        }
                        ?: return@TextButton

                    onSave(selectedDate)
                }
            ) {
                Text("Сохранить".asActionButtonText())
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Отмена".asActionButtonText())
            }
        }
    ) {
        Column {
            DatePicker(
                state = datePickerState
            )

            if (showError) {
                Text(
                    text = "Дата не может быть больше текущей даты",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }

}

private fun isRentDueOrOverdue(
    rentPaidUntil: String
): Boolean {

    if (rentPaidUntil.isBlank()) return false

    val rentDate = parseRentDateToMillis(rentPaidUntil) ?: return false

    return rentDate <= todayStartMillis()

}

private fun parseRentDateToMillis(
    date: String
): Long? {

    if (date.isBlank()) return null

    return rentDateFormatter().parse(date)?.time

}

private fun formatRentDate(
    millis: Long
): String {

    return rentDateFormatter().format(Date(millis))

}

private fun todayStartMillis(): Long {

    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

}

private fun rentDateFormatter(): SimpleDateFormat {

    return SimpleDateFormat(
        "dd.MM.yyyy",
        Locale.getDefault()
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }

}
