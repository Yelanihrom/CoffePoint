package ru.coffeepoint.core.sync.google.dto

data class SalesAnalyticsResponseDto(
    val success: Boolean,
    val data: SalesAnalyticsDataDto?,
    val error: GoogleSheetsSyncErrorDto?
)

data class SalesAnalyticsDataDto(
    val startDate: String?,
    val endDate: String?,
    val generatedAt: String?,
    val totalSales: Long?,
    val stations: List<SalesAnalyticsStationDto>?
)

data class SalesAnalyticsStationDto(
    val stationId: Int?,
    val name: String?,
    val terminalId: String?,
    val salesCount: Long?
)
