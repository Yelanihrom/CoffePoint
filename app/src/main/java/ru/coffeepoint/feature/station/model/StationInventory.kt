package ru.coffeepoint.feature.station.model

data class StationInventory(

    val cups: Int,

    val lids: Int,

    val coffee: Int,

    val milk: Int,

    val chocolate: Int,

    val topping: Int,

    val sugar: StockLevel,

    val stirrers: StockLevel

)