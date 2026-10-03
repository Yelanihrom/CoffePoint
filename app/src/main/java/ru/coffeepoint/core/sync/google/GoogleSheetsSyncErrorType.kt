package ru.coffeepoint.core.sync.google

enum class GoogleSheetsSyncErrorType(
    val storageValue: String
) {
    NONE(""),
    CONNECTION("CONNECTION"),
    CONFLICT("CONFLICT"),
    UNKNOWN("UNKNOWN")
}
