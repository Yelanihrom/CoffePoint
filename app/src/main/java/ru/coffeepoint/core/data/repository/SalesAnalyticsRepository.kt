package ru.coffeepoint.core.data.repository

import ru.coffeepoint.core.sync.google.GoogleSheetsSalesAnalyticsRemoteDataSource
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod

class SalesAnalyticsRepository(
    private val remote: GoogleSheetsSalesAnalyticsRemoteDataSource = GoogleSheetsSalesAnalyticsRemoteDataSource()
) {
    suspend fun load(period: AnalyticsPeriod) = remote.load(period)
}
