package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "supply_catalog")
data class SupplyCatalogEntity(

    @PrimaryKey
    val id: Int,

    val itemType: String,

    val title: String,

    val supplier: String,

    val article: String,

    val url: String,

    val maxPrice: Int,

    val isActive: Boolean = true,

    val priority: Int = 1,

    val comment: String = "",

    val updatedAt: String = "",

    val isSynced: Boolean = true,

    val syncError: Boolean = false,

    val syncErrorType: String = ""

)
