package ru.coffeepoint.core.sync.google

import android.util.Log
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi
import ru.coffeepoint.core.sync.google.network.GoogleSheetsApiProvider
import ru.coffeepoint.feature.analytics.model.ANALYTICS_TIME_ZONE
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import ru.coffeepoint.feature.analytics.model.SalesAnalyticsReport
import ru.coffeepoint.feature.analytics.model.StationSales
import java.time.Instant
import java.time.LocalDate

class GoogleSheetsSalesAnalyticsRemoteDataSource(
    private val api: GoogleSheetsSyncApi = GoogleSheetsApiProvider.analyticsApi,
    private val webAppUrl: String = GoogleSheetsSyncConfig.WEB_APP_URL,
    private val syncToken: String = GoogleSheetsSyncConfig.SYNC_TOKEN,
    private val diagnostic: (String) -> Unit = { Log.i("SalesAnalytics", it) }
) {
    suspend fun load(period: AnalyticsPeriod): SalesAnalyticsReport {
        var stage = "configuration"
        diagnostic("Started: action=sales_analytics start=${period.start} end=${period.end ?: "now"}")
        try {
            if (!GoogleSheetsSyncConfig.isEnabled(webAppUrl, syncToken)) {
                throw SalesAnalyticsException(SalesAnalyticsFailure.NOT_CONFIGURED)
            }
            require(period.isValid(LocalDate.now(ANALYTICS_TIME_ZONE)))
            stage = "request"
            val response = api.salesAnalytics(webAppUrl, "sales_analytics", syncToken,
                period.start.toString(), period.end?.toString())
            diagnostic("Response: success=${response.success} serverStatus=${response.error?.status}")
            if (!response.success) {
                throw SalesAnalyticsException(salesAnalyticsServerFailure(response.error?.status, response.error?.message))
            }
            stage = "validate_response"
            val data = checkNotNull(response.data)
            check(data.startDate == period.start.toString() && data.endDate == period.end?.toString())
            val stations = checkNotNull(data.stations).map { item ->
                val id = checkNotNull(item.stationId)
                check(id > 0)
                val name = checkNotNull(item.name)
                check(name.isNotBlank())
                check(if (item.terminalId.isNullOrBlank()) item.salesCount == null
                    else item.salesCount != null && item.salesCount >= 0)
                StationSales(id, name, item.salesCount)
            }
            check(stations.map { it.stationId }.distinct().size == stations.size)
            val total = checkNotNull(data.totalSales)
            check(total >= 0 && total == stations.sumOf { it.salesCount ?: 0 })
            val report = SalesAnalyticsReport(total, stations, Instant.parse(checkNotNull(data.generatedAt)))
            diagnostic("Completed: stations=${stations.size} totalSales=$total")
            return report
        } catch (cancelled: CancellationException) {
            diagnostic("Cancelled: stage=$stage")
            throw cancelled
        } catch (error: Exception) {
            // Exception messages, bodies and URLs can contain credentials; log only bounded metadata.
            diagnostic("Failed: stage=$stage type=${error.javaClass.simpleName}" +
                " reason=${(error as? SalesAnalyticsException)?.reason ?: "unknown"}" +
                " httpStatus=${(error as? HttpException)?.code()}")
            throw error
        }
    }
}
