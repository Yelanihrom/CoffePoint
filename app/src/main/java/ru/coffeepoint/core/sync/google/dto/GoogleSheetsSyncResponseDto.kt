package ru.coffeepoint.core.sync.google.dto

data class GoogleSheetsSyncResponseDto(
    val success: Boolean,
    val error: GoogleSheetsSyncErrorDto?
)

data class GoogleSheetsSyncErrorDto(
    val status: Int?,
    val message: String?
)
