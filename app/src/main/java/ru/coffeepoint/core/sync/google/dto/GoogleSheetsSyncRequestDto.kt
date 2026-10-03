package ru.coffeepoint.core.sync.google.dto

data class GoogleSheetsSyncRequestDto(
    val action: String,
    val token: String,
    val entity: String,
    val data: Map<String, Any?>
)
