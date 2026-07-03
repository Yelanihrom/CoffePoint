package ru.coffeepoint.feature.station.model

data class CoffeeStation(

    val id: Int,

    val name: String,

    val terminalId: Long? = null,

    val address: String = "",

    val lastServiceDate: String,

    val salesAfterService: Int,

    val salesLimit: Int = 120,

    val machinePhotoPath: String? = null,

    val overviewPhotoPath: String? = null
)