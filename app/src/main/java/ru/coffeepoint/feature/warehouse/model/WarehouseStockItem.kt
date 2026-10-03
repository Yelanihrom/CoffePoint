package ru.coffeepoint.feature.warehouse.model

import ru.coffeepoint.R

data class WarehouseStockItem(
    val itemType: String,
    val title: String,
    val quantity: Int,
    val iconRes: Int
)

enum class WarehouseItemType(
    val code: String,
    val title: String,
    val iconRes: Int
) {
    Coffee(
        code = "coffee",
        title = "\u041a\u043e\u0444\u0435",
        iconRes = R.drawable.ic_supply_coffee_bean
    ),
    Milk(
        code = "milk",
        title = "\u041c\u043e\u043b\u043e\u043a\u043e",
        iconRes = R.drawable.ic_supply_milk
    ),
    Chocolate(
        code = "chocolate",
        title = "\u0428\u043e\u043a\u043e\u043b\u0430\u0434",
        iconRes = R.drawable.ic_supply_chocolate
    ),
    Topping(
        code = "topping",
        title = "\u0422\u043e\u043f\u043f\u0438\u043d\u0433",
        iconRes = R.drawable.ic_supply_topping_banana
    );

    companion object {
        fun fromCode(code: String): WarehouseItemType? {
            return entries.firstOrNull { item -> item.code == code }
        }
    }
}
