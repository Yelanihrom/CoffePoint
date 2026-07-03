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

    val lastServiceDate: String,

    val salesAfterService: Int,

    val salesLimit: Int,

    val machinePhotoPath: String?,

    val overviewPhotoPath: String?

)