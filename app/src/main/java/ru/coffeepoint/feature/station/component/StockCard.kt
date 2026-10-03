package ru.coffeepoint.feature.station.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.coffeepoint.R
import ru.coffeepoint.feature.station.model.StationDetail

@Composable
fun StockCard(
    detail: StationDetail,
    onStockItemClick: (itemType: String, quantityText: String) -> Unit = { _, _ -> }
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(
            modifier = Modifier.height(8.dp)
        )

        StockSection(
            title = "\u0420\u0430\u0441\u0445\u043e\u0434\u043d\u0438\u043a\u0438"
        ) {
            StockItem(
                title = "\u0421\u0442\u0430\u043a\u0430\u043d\u044b",
                value = "${detail.inventory.cups} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_cups,
                onClick = {
                    onStockItemClick("cups", detail.inventory.cups.toString())
                }
            )

            StockItem(
                title = "\u041a\u0440\u044b\u0448\u043a\u0438",
                value = "${detail.inventory.lids} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_lids,
                onClick = {
                    onStockItemClick("lids", detail.inventory.lids.toString())
                }
            )

            StockItem(
                title = "\u041c\u0435\u0448\u0430\u043b\u043a\u0438",
                value = "${detail.inventory.stirrers} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_stirrers,
                onClick = {
                    onStockItemClick("stirrers", detail.inventory.stirrers.toString())
                }
            )

            StockItem(
                title = "\u0421\u0430\u0445\u0430\u0440",
                value = "${detail.inventory.sugar} \u043a\u043e\u043d.",
                iconRes = R.drawable.ic_supply_sugar_cubes,
                onClick = {
                    onStockItemClick("sugar", detail.inventory.sugar.toString())
                }
            )

            StockItem(
                title = "\u041c\u0443\u0441\u043e\u0440\u043d\u044b\u0435 \u043f\u0430\u043a\u0435\u0442\u044b",
                value = "${detail.inventory.garbageBags} \u0440\u0443\u043b.",
                iconRes = R.drawable.ic_supply_garbage_bags,
                onClick = {
                    onStockItemClick("garbageBags", detail.inventory.garbageBags.toString())
                }
            )
        }

        StockSection(
            title = "\u0418\u043d\u0433\u0440\u0435\u0434\u0438\u0435\u043d\u0442\u044b"
        ) {
            StockItem(
                title = "\u041a\u043e\u0444\u0435",
                value = "${detail.inventory.coffee} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_coffee_bean,
                onClick = {
                    onStockItemClick("coffee", detail.inventory.coffee.toString())
                }
            )

            StockItem(
                title = "\u041c\u043e\u043b\u043e\u043a\u043e",
                value = "${detail.inventory.milk} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_milk,
                onClick = {
                    onStockItemClick("milk", detail.inventory.milk.toString())
                }
            )

            StockItem(
                title = "\u0428\u043e\u043a\u043e\u043b\u0430\u0434",
                value = "${detail.inventory.chocolate} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_chocolate,
                onClick = {
                    onStockItemClick("chocolate", detail.inventory.chocolate.toString())
                }
            )

            StockItem(
                title = "\u0422\u043e\u043f\u043f\u0438\u043d\u0433",
                value = "${detail.inventory.topping} \u0443\u043f.",
                iconRes = R.drawable.ic_supply_topping_banana,
                onClick = {
                    onStockItemClick("topping", detail.inventory.topping.toString())
                }
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )
    }
}
