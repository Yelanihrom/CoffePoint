package ru.coffeepoint.core.sync.google.dto

data class GoogleSheetsFullSyncResponseDto(
    val success: Boolean,
    val data: GoogleSheetsFullSyncDataDto?,
    val error: GoogleSheetsSyncErrorDto?
)

data class GoogleSheetsFullSyncDataDto(
    val stations: List<Map<String, Any?>>,
    val inventories: List<Map<String, Any?>>,
    val settings: List<Map<String, Any?>> = emptyList(),
    val supplyCatalog: List<Map<String, Any?>> = emptyList(),
    val warehouseStock: List<Map<String, Any?>> = emptyList()
)
