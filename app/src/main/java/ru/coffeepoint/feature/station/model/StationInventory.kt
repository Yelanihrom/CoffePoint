package ru.coffeepoint.feature.station.model

data class StationInventory(

    val cups: Int,

    val lids: Int,

    val coffee: Int,

    val milk: Int,

    val chocolate: Int,

    val topping: Int,

    val sugar: Int,

    val stirrers: Int,

    val garbageBags: Int = 2,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
