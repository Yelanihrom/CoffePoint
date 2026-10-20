package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "warehouse_stock")
data class WarehouseStockEntity(

    @PrimaryKey
    val itemType: String,

    val quantity: Int,

    val minQuantity: Int = 0,

    val maxQuantity: Int = 0,

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
