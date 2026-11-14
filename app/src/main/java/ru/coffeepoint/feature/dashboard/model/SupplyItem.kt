package ru.coffeepoint.feature.dashboard.model

import ru.coffeepoint.R
import ru.coffeepoint.feature.station.model.StationDetail

data class SupplyItem(
    val itemType: String,
    val title: String,
    val value: String,
    val description: String? = null,
    val urgency: SupplyUrgency = SupplyUrgency.NORMAL
)

enum class SupplyUrgency {
    NORMAL,
    WARNING,
    CRITICAL
}

val SupplyItem.isCritical: Boolean
    get() = urgency == SupplyUrgency.CRITICAL

val SupplyItem.isWarning: Boolean
    get() = urgency == SupplyUrgency.WARNING

fun SupplyItem.iconRes(): Int? {
    return when (title) {
        "Стаканы" -> R.drawable.ic_supply_cups
        "Крышки" -> R.drawable.ic_supply_lids
        "Сахар" -> R.drawable.ic_supply_sugar_cubes
        "Мешалки" -> R.drawable.ic_supply_stirrers
        "Пакеты" -> R.drawable.ic_supply_garbage_bags
        "Кофе" -> R.drawable.ic_supply_coffee_bean
        "Молоко" -> R.drawable.ic_supply_milk
        "Шоколад" -> R.drawable.ic_supply_chocolate
        "Топпинг" -> R.drawable.ic_supply_topping_banana
        else -> null
    }
}

fun StationDetail.supplyItems(): List<SupplyItem> {

    if (station.excludeFromSupplyPlanning) return emptyList()

    val items = mutableListOf<SupplyItem>()

    if (inventory.cups < settings.cupsMax) {
        items.addSupplyItem("Стаканы", settings.cupsMax, inventory.cups, minimum = settings.cupsMin)
    }

    if (inventory.lids < settings.lidsMax) {
        items.addSupplyItem("Крышки", settings.lidsMax, inventory.lids, minimum = settings.lidsMin)
    }

    if (inventory.coffee < settings.coffeeMax) {
        items.addSupplyItem("Кофе", settings.coffeeMax, inventory.coffee, minimum = settings.coffeeMin)
    }

    if (inventory.milk < settings.milkMax) {
        items.addSupplyItem("Молоко", settings.milkMax, inventory.milk, minimum = settings.milkMin)
    }

    if (inventory.chocolate < settings.chocolateMax) {
        items.addSupplyItem("Шоколад", settings.chocolateMax, inventory.chocolate, minimum = settings.chocolateMin)
    }

    if (inventory.topping < settings.toppingMax) {
        items.addSupplyItem("Топпинг", settings.toppingMax, inventory.topping, minimum = settings.toppingMin)
    }

    if (inventory.sugar < settings.sugarMax) {
        items.addSupplyItem(
            title = "Сахар",
            max = settings.sugarMax,
            current = inventory.sugar,
            valueSuffix = "кон.",
            includeSugarDescription = true,
            minimum = settings.sugarMin
        )
    }

    if (inventory.stirrers < settings.stirrersMax) {
        items.addSupplyItem(
            title = "Мешалки",
            max = settings.stirrersMax,
            current = inventory.stirrers,
            minimum = settings.stirrersMin
        )
    }

    if (inventory.garbageBags < settings.garbageBagsMax) {
        items.addSupplyItem(
            title = "Пакеты",
            max = settings.garbageBagsMax,
            current = inventory.garbageBags,
            minimum = settings.garbageBagsMin
        )
    }

    return items

}

private fun MutableList<SupplyItem>.addSupplyItem(
    title: String,
    max: Int,
    current: Int,
    valueSuffix: String = "",
    includeSugarDescription: Boolean = false,
    minimum: Int
) {

    val amount = (max - current).coerceAtLeast(0)

    if (amount > 0) {
        this += SupplyItem(
            itemType = itemTypeByTitle(title),
            title = title,
            value = amount.formatSupplyValue(valueSuffix),
            description = if (includeSugarDescription) sugarSupplyDescription(amount) else null,
            urgency = supplyUrgency(
                current = current,
                max = max,
                minimum = minimum
            )
        )
    }

}

private fun itemTypeByTitle(title: String): String {
    return when (title) {
        "Стаканы" -> "cups"
        "Крышки" -> "lids"
        "Сахар" -> "sugar"
        "Мешалки" -> "stirrers"
        "Пакеты" -> "garbageBags"
        "Кофе" -> "coffee"
        "Молоко" -> "milk"
        "Шоколад" -> "chocolate"
        "Топпинг" -> "topping"
        else -> ""
    }
}

private fun supplyUrgency(
    current: Int,
    max: Int,
    minimum: Int
): SupplyUrgency {
    return when {
        minimum > 0 && current < minimum -> SupplyUrgency.CRITICAL
        minimum > 0 && max > 0 && current < (minimum + max) / 2.0 ->
            SupplyUrgency.WARNING
        else -> SupplyUrgency.NORMAL
    }
}

private fun sugarSupplyDescription(amount: Int): String {
    val sticks = amount * 500
    val grams = amount * 2500
    return "$sticks стиков / $grams г"
}

private fun Int.formatSupplyValue(suffix: String): String {
    return if (suffix.isBlank()) toString() else "$this $suffix"
}
