package ru.coffeepoint.feature.dashboard.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.coffeepoint.R
import ru.coffeepoint.core.status.StationStatusSeverity
import ru.coffeepoint.core.status.statusSeverity
import ru.coffeepoint.feature.dashboard.model.DashboardMode
import ru.coffeepoint.feature.dashboard.model.SupplyItem
import ru.coffeepoint.feature.dashboard.model.iconRes
import ru.coffeepoint.feature.dashboard.model.isCritical
import ru.coffeepoint.feature.dashboard.model.isWarning
import ru.coffeepoint.feature.dashboard.model.supplyItems
import ru.coffeepoint.feature.dashboard.model.supplyOperatorTechServiceDashboardItems
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.hasSyncError
import ru.coffeepoint.feature.station.model.isFullySynced
import ru.coffeepoint.feature.station.model.isSyncPending
import ru.coffeepoint.feature.station.model.visitScheduleText
import ru.coffeepoint.shared.component.SyncStatusIcon

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StationCard(
    detail: StationDetail,
    mode: DashboardMode,
    showSyncStatus: Boolean,
    showStationStatus: Boolean,
    showTerminalInfo: Boolean,
    showSupplyOperatorTechService: Boolean = false,
    supplyItemFilter: (SupplyItem) -> Boolean = { true },
    onSupplyItemClick: (SupplyItem) -> Unit = {},
    onClick: () -> Unit = {}
) {

    val station = detail.station

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {

        Column {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = station.address.ifBlank { station.name },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    if (showSyncStatus) {
                        SyncStatusIcon(
                            contentDescription = when {
                                detail.isFullySynced() -> "Синхронизировано"
                                detail.hasSyncError() -> "Ошибка синхронизации"
                                else -> "Ожидает синхронизации"
                            },
                            arrowTint = when {
                                detail.isFullySynced() -> Color(0xFF2E7D32)
                                detail.isSyncPending() -> Color(0xFFF57C00)
                                else -> Color(0xFFD32F2F)
                            },
                            modifier = Modifier.size(30.dp)
                        )
                    }

                }

                if (showTerminalInfo && station.terminalId != null) {

                    Text(
                        text = "Терминал: ${station.terminalId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                }

                station.visitScheduleText().takeIf { value -> value.isNotBlank() }
                    ?.let { schedule ->
                        Text(
                            text = schedule,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

            }

            when (mode) {
                DashboardMode.ALL_STATIONS,
                DashboardMode.REFILL -> SalesInfo(
                    detail = detail,
                    showStatusColor = showStationStatus
                )

                DashboardMode.SUPPLY -> {
                    Column(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                    ) {
                        Spacer(modifier = Modifier.height(14.dp))
                        SupplyInfo(
                            detail = detail,
                            supplyItemFilter = supplyItemFilter,
                            onSupplyItemClick = onSupplyItemClick
                        )
                        if (showSupplyOperatorTechService) {
                            SupplyOperatorTechServiceInfo(detail)
                        }
                    }
                }

            }
        }

    }

}

@Composable
private fun SalesInfo(
    detail: StationDetail,
    showStatusColor: Boolean
) {

    val statusColor = if (showStatusColor) {
        when (detail.statusSeverity()) {
            StationStatusSeverity.CRITICAL -> Color(0xFFD32F2F)
            StationStatusSeverity.WARNING -> Color(0xFFF57C00)
            StationStatusSeverity.OK -> Color(0xFF2E7D32)
        }
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {

        Surface(
            modifier = Modifier.weight(1f),
            color = statusColor,
            contentColor = if (showStatusColor) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            shape = RoundedCornerShape(
                topEnd = 16.dp,
                bottomStart = 12.dp
            )
        ) {

            Column {

                Text(
                    text = "${detail.station.salesAfterService} продаж",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    )
                )

            }

        }

        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 12.dp,
                    end = 16.dp,
                    bottom = 12.dp
                )
        ) {

            Text(
                text = detail.station.lastServiceDate.ifBlank { "нет данных" },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "последняя заправка",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplyInfo(
    detail: StationDetail,
    supplyItemFilter: (SupplyItem) -> Boolean,
    onSupplyItemClick: (SupplyItem) -> Unit
) {

    val supplyItems = detail.supplyItems().filter(supplyItemFilter)

    Text(
        text = "\u041d\u0435\u043e\u0431\u0445\u043e\u0434\u0438\u043c\u043e \u0437\u0430\u043a\u0443\u043f\u0438\u0442\u044c",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (supplyItems.isEmpty()) {

        Text(
            text = "Поставка не требуется",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    } else {

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            supplyItems.forEach { item ->
                SupplyItemChip(
                    item = item,
                    onClick = {
                        onSupplyItemClick(item)
                    }
                )
            }
        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupplyOperatorTechServiceInfo(
    detail: StationDetail
) {

    val techServiceItems = detail.supplyOperatorTechServiceDashboardItems()

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = "К обслуживанию",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (techServiceItems.isEmpty()) {

        Text(
            text = "Обслуживание не требуется",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

    } else {

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            techServiceItems.forEach { item ->
                TechServiceChip(title = item.title)
            }

        }

    }

}

@Composable
private fun TechServiceChip(
    title: String
) {

    val iconRes = techServiceIconRes(title)

    Surface(
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
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(30.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

    }

}

private fun techServiceIconRes(title: String): Int? {
    return when (title) {
        "\u041c\u0438\u043a\u0441\u0435\u0440\u044b" -> R.drawable.ic_tech_mixers
        "\u041a\u043e\u043d\u0442\u0435\u0439\u043d\u0435\u0440 \u0436\u0438\u0434\u043a\u0438\u0445 \u043e\u0442\u0445\u043e\u0434\u043e\u0432" -> R.drawable.ic_tech_liquid_waste_container
        else -> null
    }
}

@Composable
private fun SupplyItemChip(
    item: SupplyItem,
    onClick: () -> Unit
) {

    val iconRes = item.iconRes()
    val valueColor = when {
        item.isCritical -> Color(0xFFE53935)
        item.isWarning -> Color(0xFFF57C00)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.small
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 8.dp
            ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconRes != null) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = item.title,
                        modifier = Modifier.size(30.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(30.dp))
                }

                Text(
                    text = item.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueColor
                )
            }

        }

    }

}
