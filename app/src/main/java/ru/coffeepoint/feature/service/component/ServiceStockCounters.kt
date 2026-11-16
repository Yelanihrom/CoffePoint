package ru.coffeepoint.feature.service.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import ru.coffeepoint.feature.service.model.maximum
import ru.coffeepoint.feature.service.model.quantity
import ru.coffeepoint.feature.station.component.StockSection
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType
import ru.coffeepoint.shared.component.CounterRow

@Composable
fun ServiceStockCounters(
    inventory: StationInventory,
    settings: StationSettings,
    onChange: (WarehouseItemType, Int) -> Unit,
    showMax: Boolean = false,
    enabled: Boolean = true,
    valueColor: (WarehouseItemType) -> Color = { Color.Unspecified },
    belowRow: @Composable (WarehouseItemType) -> Unit = {}
) {
    listOf("Расходники" to WarehouseItemType.entries.take(5), "Ингредиенты" to WarehouseItemType.entries.drop(5)).forEach { (title, types) ->
        StockSection(title = title) {
            types.forEach { type ->
                CounterRow(
                    title = type.title, value = inventory.quantity(type), max = settings.maximum(type),
                    showMax = showMax, iconRes = type.iconRes, enabled = enabled,
                    valueColor = valueColor(type), modifier = Modifier.testTag("stock-${type.code}"),
                    onMinus = { onChange(type, -1) }, onPlus = { onChange(type, 1) }
                )
                belowRow(type)
            }
        }
    }
}
