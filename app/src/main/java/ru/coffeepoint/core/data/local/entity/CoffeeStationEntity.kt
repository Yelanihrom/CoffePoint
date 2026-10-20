package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coffee_station")
data class CoffeeStationEntity(

    @PrimaryKey
    val id: Int,

    val name: String,

    val terminalId: Long?,

    val address: String,

    val visitDays: String = "1,2,3,4,5",

    val visitTimeFrom: String = "09:00",

    val visitTimeTo: String = "18:00",

    val lastServiceDate: String,

    val salesAfterService: Int,

    val rentPaidUntil: String,

    val excludeFromSupplyPlanning: Boolean,

    val excludeFromOperatorEarnings: Boolean,

    val liquidWasteContainerServiceDate: String,

    val brewUnitServiceDate: String,

    val coffeeGrinderServiceDate: String,

    val mixersServiceDate: String,

    val expansionTankServiceDate: String,

    val exhaustServiceDate: String,

    val machinePhotoPath: String?,

    val overviewPhotoPath: String?,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = "",

    val pendingServiceOnly: Boolean = false,

    val pendingFullStationSync: Boolean = false,

    val pendingSupplyServiceOnly: Boolean = false,

    val pendingMixersServiceDate: Boolean = false,

    val pendingLiquidWasteContainerServiceDate: Boolean = false

)
