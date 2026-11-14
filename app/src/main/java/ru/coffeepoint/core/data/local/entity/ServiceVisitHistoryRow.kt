package ru.coffeepoint.core.data.local.entity

data class ServiceVisitHistoryRow(
    val id: String,
    val stationId: Int,
    val stationName: String?,
    val stationAddress: String?,
    val employeeId: Int,
    val employeeName: String?,
    val completedAt: String,
    val isSynced: Boolean,
    val syncError: Boolean
)
