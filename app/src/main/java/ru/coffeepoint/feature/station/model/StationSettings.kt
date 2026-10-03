package ru.coffeepoint.feature.station.model

data class StationSettings(

    val cupsMax: Int,

    val lidsMax: Int,

    val coffeeMax: Int,

    val milkMax: Int,

    val chocolateMax: Int,

    val toppingMax: Int,

    val sugarMax: Int = 2,

    val stirrersMax: Int = 4,

    val garbageBagsMax: Int = 2,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
