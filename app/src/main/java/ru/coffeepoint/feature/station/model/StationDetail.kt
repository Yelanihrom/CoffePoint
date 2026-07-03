package ru.coffeepoint.feature.station.model

data class StationDetail(

    val station: CoffeeStation,

    val inventory: StationInventory,

    val settings: StationSettings

)