package ru.coffeepoint.core.sync.google.dto

data class GoogleSheetsSyncResponseDto(
    val success: Boolean,
    val error: GoogleSheetsSyncErrorDto?,
    val data: GoogleSheetsSyncDataDto? = null
)

data class GoogleSheetsSyncDataDto(
    val visit: Map<String, Any?>? = null,
    val earning: Map<String, Any?>? = null
)

data class GoogleSheetsSyncErrorDto(
    val status: Int?,
    val message: String?
)
