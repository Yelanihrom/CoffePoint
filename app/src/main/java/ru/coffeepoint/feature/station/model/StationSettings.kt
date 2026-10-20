package ru.coffeepoint.feature.station.model

data class StationSettings(

    val cupsMin: Int = 3,

    val cupsMax: Int,

    val lidsMin: Int = 3,

    val lidsMax: Int,

    val coffeeMin: Int = 3,

    val coffeeMax: Int,

    val milkMin: Int = 3,

    val milkMax: Int,

    val chocolateMin: Int = 3,

    val chocolateMax: Int,

    val toppingMin: Int = 3,

    val toppingMax: Int,

    val sugarMin: Int = 1,

    val sugarMax: Int = 2,

    val stirrersMin: Int = 1,

    val stirrersMax: Int = 4,

    val garbageBagsMin: Int = 1,

    val garbageBagsMax: Int = 2,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
