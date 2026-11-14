package ru.coffeepoint.core.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "employees_transactions",
    indices = [
        Index(
            value = [
                "employeeId",
                "type",
                "periodFrom",
                "periodTo",
            ]
        ),
        Index(value = ["sourceType", "sourceId"], unique = true)
    ]
)
data class EmployeeTransactionEntity(
    @PrimaryKey
    val id: String,
    val employeeId: Int,
    val type: String,
    val operationDate: String,
    val periodFrom: String?,
    val periodTo: String?,
    val salesCount: Int?,
    val amount: Int,
    val comment: String,
    val createdAt: String,
    val updatedAt: String,
    val isSynced: Boolean = true,
    val syncError: Boolean = false,
    val syncErrorType: String = "",
    val sourceType: String? = null,
    val sourceId: String? = null,
    @ColumnInfo(defaultValue = "0")
    val isLocalOnly: Boolean = false
)
