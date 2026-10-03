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
        "\u0421\u0442\u0430\u043a\u0430\u043d\u044b" -> R.drawable.ic_supply_cups
        "\u041a\u0440\u044b\u0448\u043a\u0438" -> R.drawable.ic_supply_lids
        "\u0421\u0430\u0445\u0430\u0440" -> R.drawable.ic_supply_sugar_cubes
        "\u041c\u0435\u0448\u0430\u043b\u043a\u0438" -> R.drawable.ic_supply_stirrers
        "\u041c\u0443\u0441\u043e\u0440\u043d\u044b\u0435 \u043f\u0430\u043a\u0435\u0442\u044b" -> R.drawable.ic_supply_garbage_bags
        "\u041a\u043e\u0444\u0435" -> R.drawable.ic_supply_coffee_bean
        "\u041c\u043e\u043b\u043e\u043a\u043e" -> R.drawable.ic_supply_milk
        "\u0428\u043e\u043a\u043e\u043b\u0430\u0434" -> R.drawable.ic_supply_chocolate
        "\u0422\u043e\u043f\u043f\u0438\u043d\u0433" -> R.drawable.ic_supply_topping_banana
        else -> null
    }
}

fun StationDetail.supplyItems(): List<SupplyItem> {

    if (station.excludeFromSupplyPlanning) return emptyList()

    val items = mutableListOf<SupplyItem>()

    if (inventory.cups < settings.cupsMax) {
        items.addSupplyItem("Стаканы", settings.cupsMax, inventory.cups)
    }

    if (inventory.lids < settings.lidsMax) {
        items.addSupplyItem("Крышки", settings.lidsMax, inventory.lids)
    }

    if (inventory.coffee < settings.coffeeMax) {
        items.addSupplyItem("Кофе", settings.coffeeMax, inventory.coffee)
    }

    if (inventory.milk < settings.milkMax) {
        items.addSupplyItem("Молоко", settings.milkMax, inventory.milk)
    }

    if (inventory.chocolate < settings.chocolateMax) {
        items.addSupplyItem("Шоколад", settings.chocolateMax, inventory.chocolate)
    }

    if (inventory.topping < settings.toppingMax) {
        items.addSupplyItem("Топпинг", settings.toppingMax, inventory.topping)
    }

    if (inventory.sugar < settings.sugarMax) {
        items.addSupplyItem(
            title = "Сахар",
            max = settings.sugarMax,
            current = inventory.sugar,
            valueSuffix = "кон.",
            includeSugarDescription = true,
            criticalThreshold = 0
        )
    }

    if (inventory.stirrers < settings.stirrersMax) {
        items.addSupplyItem(
            title = "Мешалки",
            max = settings.stirrersMax,
            current = inventory.stirrers,
            criticalThreshold = 0
        )
    }

    if (inventory.garbageBags < settings.garbageBagsMax) {
        items.addSupplyItem(
            title = "Мусорные пакеты",
            max = settings.garbageBagsMax,
            current = inventory.garbageBags,
            criticalThreshold = 0
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
                includeSelector = { detail ->
                    detail.inventory.sugar < detail.settings.sugarMax
                },
                criticalThreshold = 0
            )
        )
        addPositiveAmount(
            title = "Мешалки",
            amount = stirrers,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.stirrers },
                maxSelector = { detail -> detail.settings.stirrersMax },
                includeSelector = { detail ->
                    detail.inventory.stirrers < detail.settings.stirrersMax
                },
                criticalThreshold = 0
            )
        )
        addPositiveAmount(
            title = "Мусорные пакеты",
            amount = garbageBags,
            urgency = plannedStations.supplySummaryUrgency(
                currentSelector = { detail -> detail.inventory.garbageBags },
                maxSelector = { detail -> detail.settings.garbageBagsMax },
                includeSelector = { detail ->
                    detail.inventory.garbageBags < detail.settings.garbageBagsMax
                },
                criticalThreshold = 0
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
    criticalThreshold: Int = 2
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
                criticalThreshold = criticalThreshold
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
        "\u0421\u0442\u0430\u043a\u0430\u043d\u044b" -> "cups"
        "\u041a\u0440\u044b\u0448\u043a\u0438" -> "lids"
        "\u0421\u0430\u0445\u0430\u0440" -> "sugar"
        "\u041c\u0435\u0448\u0430\u043b\u043a\u0438" -> "stirrers"
        "\u041c\u0443\u0441\u043e\u0440\u043d\u044b\u0435 \u043f\u0430\u043a\u0435\u0442\u044b" -> "garbageBags"
        "\u041a\u043e\u0444\u0435" -> "coffee"
        "\u041c\u043e\u043b\u043e\u043a\u043e" -> "milk"
        "\u0428\u043e\u043a\u043e\u043b\u0430\u0434" -> "chocolate"
        "\u0422\u043e\u043f\u043f\u0438\u043d\u0433" -> "topping"
        else -> ""
    }
}

private fun supplyUrgency(
    current: Int,
    max: Int,
    criticalThreshold: Int = 2
): SupplyUrgency {
    return when {
        current <= criticalThreshold -> SupplyUrgency.CRITICAL
        current * 2 <= max -> SupplyUrgency.WARNING
        else -> SupplyUrgency.NORMAL
    }
}

private fun List<StationDetail>.supplySummaryUrgency(
    currentSelector: (StationDetail) -> Int,
    maxSelector: (StationDetail) -> Int,
    includeSelector: (StationDetail) -> Boolean,
    criticalThreshold: Int = 2
): SupplyUrgency {
    val included = filter(includeSelector)

    return when {
        included.any { detail -> currentSelector(detail) <= criticalThreshold } ->
            SupplyUrgency.CRITICAL

        included.any { detail ->
            currentSelector(detail) * 2 <= maxSelector(detail)
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
