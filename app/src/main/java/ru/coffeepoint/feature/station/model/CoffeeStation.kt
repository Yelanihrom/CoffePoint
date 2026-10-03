package ru.coffeepoint.feature.station.model

data class CoffeeStation(

    val id: Int,

    val name: String,

    val terminalId: Long? = null,

    val address: String = "",

    val visitDays: String = "1,2,3,4,5",

    val visitTimeFrom: String = "09:00",

    val visitTimeTo: String = "18:00",

    val lastServiceDate: String,

    val salesAfterService: Int,

    val rentPaidUntil: String = "",

    val excludeFromSupplyPlanning: Boolean = false,

    val excludeFromOperatorEarnings: Boolean = false,

    val liquidWasteContainerServiceDate: String = "",

    val brewUnitServiceDate: String = "",

    val coffeeGrinderServiceDate: String = "",

    val mixersServiceDate: String = "",

    val expansionTankServiceDate: String = "",

    val exhaustServiceDate: String = "",

    val machinePhotoPath: String? = null,

    val overviewPhotoPath: String? = null,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""
)
