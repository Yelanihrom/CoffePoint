package ru.coffeepoint.feature.dashboard.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.coffeepoint.R
import ru.coffeepoint.feature.dashboard.model.SupplyItem
import ru.coffeepoint.feature.dashboard.model.iconRes
import ru.coffeepoint.feature.dashboard.model.isCritical
import ru.coffeepoint.feature.dashboard.model.isWarning
import ru.coffeepoint.feature.warehouse.model.WarehouseStockItem

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SupplySummaryCard(
    items: List<SupplyItem>,
    title: String = "\u041a \u043f\u043e\u0441\u0442\u0430\u0432\u043a\u0435",
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSupplyItemClick: (SupplyItem) -> Unit = {}
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            SummaryCardHeader(
                title = title,
                iconRes = R.drawable.ic_supply_vehicle,
                expanded = expanded,
                onExpandedChange = onExpandedChange
            )

            if (expanded && items.isEmpty()) {

                Text(
                    text = "Поставка не требуется",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

            } else if (expanded) {

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        SummaryItemChip(
                            item = item,
                            onClick = {
                                onSupplyItemClick(item)
                            }
                        )
                    }
                }

            }

        }

    }

}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WarehouseStockCard(
    items: List<WarehouseStockItem>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onStockItemClick: (WarehouseStockItem) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryCardHeader(
                title = "\u0421\u043a\u043b\u0430\u0434",
                iconRes = R.drawable.ic_warehouse_stock,
                expanded = expanded,
                onExpandedChange = onExpandedChange
            )

            if (expanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        WarehouseStockChip(
                            item = item,
                            onClick = {
                                onStockItemClick(item)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCardHeader(
    title: String,
    iconRes: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        IconButton(
            onClick = {
                onExpandedChange(!expanded)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun WarehouseStockChip(
    item: WarehouseStockItem,
    onClick: () -> Unit
) {
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
                Image(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(30.dp)
                )

                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryItemChip(
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
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(30.dp))
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
