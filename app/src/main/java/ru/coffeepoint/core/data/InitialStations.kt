package ru.coffeepoint.core.data

import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.StationDetail
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.station.model.StockLevel

val InitialStations = listOf(

    StationDetail(
        station = CoffeeStation(
            id = 1,
            name = "Автосалон",
            address = "",
            lastServiceDate = "19.06.2026 15:05",
            salesAfterService = 124
        ),

        inventory = StationInventory(
            cups = 2,
            lids = 0,
            coffee = 1,
            milk = 2,
            chocolate = 3,
            topping = 1,
            sugar = StockLevel.EMPTY,
            stirrers = StockLevel.LOW
        ),

        settings = StationSettings(
            cupsMax = 15,
            lidsMax = 6,
            coffeeMax = 6,
            milkMax = 10,
            chocolateMax = 12,
            toppingMax = 10
        )
    ),

    StationDetail(
        station = CoffeeStation(
            id = 2,
            name = "Поликлиника №1",
            address = "",
            lastServiceDate = "20.06.2026 09:14",
            salesAfterService = 103
        ),

        inventory = StationInventory(
            cups = 4,
            lids = 1,
            coffee = 2,
            milk = 5,
            chocolate = 8,
            topping = 3,
            sugar = StockLevel.LOW,
            stirrers = StockLevel.MEDIUM
        ),

        settings = StationSettings(
            cupsMax = 10,
            lidsMax = 4,
            coffeeMax = 6,
            milkMax = 10,
            chocolateMax = 12,
            toppingMax = 10
        )
    ),

    StationDetail(
        station = CoffeeStation(
            id = 3,
            name = "БЦ Альфа",
            address = "",
            lastServiceDate = "20.06.2026 11:42",
            salesAfterService = 31
        ),

        inventory = StationInventory(
            cups = 11,
            lids = 5,
            coffee = 5,
            milk = 8,
            chocolate = 12,
            topping = 9,
            sugar = StockLevel.FULL,
            stirrers = StockLevel.EMPTY
        ),

        settings = StationSettings(
            cupsMax = 15,
            lidsMax = 6,
            coffeeMax = 6,
            milkMax = 10,
            chocolateMax = 12,
            toppingMax = 10
        )
    )

)