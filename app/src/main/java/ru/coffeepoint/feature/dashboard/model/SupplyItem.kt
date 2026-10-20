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

fun List<StationDetail>.supplySummaryItems(): List<SupplyItem> {

    val plannedStations = filter { detail ->
        !detail.station.excludeFromSupplyPlanning
    }

    val cups = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.cups < detail.settings.cupsMax) {
            detail.settings.cupsMax - detail.inventory.cups
        } else {
            0
        }
    }

    val lids = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.lids < detail.settings.lidsMax) {
            detail.settings.lidsMax - detail.inventory.lids
        } else {
            0
        }
    }

    val coffee = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.coffee < detail.settings.coffeeMax) {
            detail.settings.coffeeMax - detail.inventory.coffee
        } else {
            0
        }
    }

    val milk = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.milk < detail.settings.milkMax) {
            detail.settings.milkMax - detail.inventory.milk
        } else {
            0
        }
    }

    val chocolate = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.chocolate < detail.settings.chocolateMax) {
            detail.settings.chocolateMax - detail.inventory.chocolate
        } else {
            0
        }
    }

    val topping = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.topping < detail.settings.toppingMax) {
            detail.settings.toppingMax - detail.inventory.topping
        } else {
            0
        }
    }

    val sugar = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.sugar < detail.settings.sugarMax) {
            detail.settings.sugarMax - detail.inventory.sugar
        } else {
            0
        }
    }

    val stirrers = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.stirrers < detail.settings.stirrersMax) {
            detail.settings.stirrersMax - detail.inventory.stirrers
        } else {
            0
        }
    }

    val garbageBags = plannedStations.sumSupplyAmount { detail ->
        if (detail.inventory.garbageBags < detail.settings.garbageBagsMax) {
            detail.settings.garbageBagsMax - detail.inventory.garbageBags
        } else {
            0
        }
    }

    return buildList {
        addPositiveAmount(
            title = "Стаканы",
            amount = cups,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.cups },
                maxSelector = { detail -> detail.settings.cupsMax },
                minSelector = { detail -> detail.settings.cupsMin },
                includeSelector = { detail ->
                    detail.inventory.cups < detail.settings.cupsMax
                }
            )
        )
        addPositiveAmount(
            title = "Крышки",
            amount = lids,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.lids },
                maxSelector = { detail -> detail.settings.lidsMax },
                minSelector = { detail -> detail.settings.lidsMin },
                includeSelector = { detail ->
                    detail.inventory.lids < detail.settings.lidsMax
                }
            )
        )
        addPositiveAmount(
            title = "Кофе",
            amount = coffee,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.coffee },
                maxSelector = { detail -> detail.settings.coffeeMax },
                minSelector = { detail -> detail.settings.coffeeMin },
                includeSelector = { detail ->
                    detail.inventory.coffee < detail.settings.coffeeMax
                }
            )
        )
        addPositiveAmount(
            title = "Молоко",
            amount = milk,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.milk },
                maxSelector = { detail -> detail.settings.milkMax },
                minSelector = { detail -> detail.settings.milkMin },
                includeSelector = { detail ->
                    detail.inventory.milk < detail.settings.milkMax
                }
            )
        )
        addPositiveAmount(
            title = "Шоколад",
            amount = chocolate,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.chocolate },
                maxSelector = { detail -> detail.settings.chocolateMax },
                minSelector = { detail -> detail.settings.chocolateMin },
                includeSelector = { detail ->
                    detail.inventory.chocolate < detail.settings.chocolateMax
                }
            )
        )
        addPositiveAmount(
            title = "Топпинг",
            amount = topping,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.topping },
                maxSelector = { detail -> detail.settings.toppingMax },
                minSelector = { detail -> detail.settings.toppingMin },
                includeSelector = { detail ->
                    detail.inventory.topping < detail.settings.toppingMax
                }
            )
        )
        addPositiveAmount(
            title = "Сахар",
            amount = sugar,
            includeSugarDescription = true,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.sugar },
                maxSelector = { detail -> detail.settings.sugarMax },
                minSelector = { detail -> detail.settings.sugarMin },
                includeSelector = { detail ->
                    detail.inventory.sugar < detail.settings.sugarMax
                },
            )
        )
        addPositiveAmount(
            title = "Мешалки",
            amount = stirrers,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.stirrers },
                maxSelector = { detail -> detail.settings.stirrersMax },
                minSelector = { detail -> detail.settings.stirrersMin },
                includeSelector = { detail ->
                    detail.inventory.stirrers < detail.settings.stirrersMax
                },
            )
        )
        addPositiveAmount(
            title = "Пакеты",
            amount = garbageBags,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.garbageBags },
                maxSelector = { detail -> detail.settings.garbageBagsMax },
                minSelector = { detail -> detail.settings.garbageBagsMin },
                includeSelector = { detail ->
                    detail.inventory.garbageBags < detail.settings.garbageBagsMax
                },
            )
        )
    }

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

private fun MutableList<SupplyItem>.addPositiveAmount(
    title: String,
    amount: Int,
    valueSuffix: String = "",
    includeSugarDescription: Boolean = false,
    urgency: SupplyUrgency
) {

    if (amount > 0) {
        this += SupplyItem(
            itemType = itemTypeByTitle(title),
            title = title,
            value = amount.formatSupplyValue(valueSuffix),
            description = if (includeSugarDescription) sugarSupplyDescription(amount) else null,
            urgency = urgency
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

private fun List<StationDetail>.supplySummaryUrgency(
    currentSelector: (StationDetail) -> Int,
    maxSelector: (StationDetail) -> Int,
    includeSelector: (StationDetail) -> Boolean,
    minSelector: (StationDetail) -> Int
): SupplyUrgency {
    val included = filter(includeSelector)

    return when {
        included.any { detail ->
            val minimum = minSelector(detail)
            minimum > 0 && currentSelector(detail) < minimum
        } ->
            SupplyUrgency.CRITICAL

        included.any { detail ->
            val minimum = minSelector(detail)
            val max = maxSelector(detail)
            minimum > 0 && max > 0 &&
                currentSelector(detail) < (minimum + max) / 2.0
        } -> SupplyUrgency.WARNING

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

private fun List<StationDetail>.sumSupplyAmount(
    selector: (StationDetail) -> Int
): Int {

    return sumOf { detail ->
        selector(detail).coerceAtLeast(0)
    }

}
