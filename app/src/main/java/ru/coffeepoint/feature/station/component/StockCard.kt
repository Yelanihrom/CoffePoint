package ru.coffeepoint.feature.station.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.title

@Composable
fun StockCard(
    detail: StationDetail
) {

    Column(

        verticalArrangement = Arrangement.spacedBy(16.dp)

    ) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        StockSection(
            title = "Расходники"
        ) {

            StockItem(
                title = "Стаканы",
                value = "${detail.inventory.cups} уп."
            )

            StockItem(
                title = "Крышки",
                value = "${detail.inventory.lids} уп."
            )

            StockItem(
                title = "Мешалки",
                value = detail.inventory.stirrers.title()
            )

            StockItem(
                title = "Сахар",
                value = detail.inventory.sugar.title()
            )

        }

        StockSection(
            title = "Ингредиенты"
        ) {

            StockItem(
                title = "Кофе",
                value = "${detail.inventory.coffee} уп."
            )

            StockItem(
                title = "Молоко",
                value = "${detail.inventory.milk} уп."
            )

            StockItem(
                title = "Шоколад",
                value = "${detail.inventory.chocolate} уп."
            )

            StockItem(
                title = "Топпинг",
                value = "${detail.inventory.topping} уп."
            )

        }

        Spacer(
            modifier = Modifier.height(16.dp)
        )

    }

}