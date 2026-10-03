package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val role: String,
    val workStartDate: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)
