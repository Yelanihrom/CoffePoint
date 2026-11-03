package ru.coffeepoint.core.sync.google

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.network.ApiTestServer
import ru.coffeepoint.core.network.fixture
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi
import ru.coffeepoint.core.sync.google.dto.SalesAnalyticsResponseDto
import ru.coffeepoint.feature.analytics.model.AnalyticsPeriod
import java.time.LocalDate

class SalesAnalyticsRemoteDataSourceTest {
    private val http = ApiTestServer()
    private val api = http.api<GoogleSheetsSyncApi>()
    private val period = AnalyticsPeriod(LocalDate.parse("2026-09-01"))
    private val diagnostics = mutableListOf<String>()
    private fun remote(token: String = "test-token") = GoogleSheetsSalesAnalyticsRemoteDataSource(
        api, http.server.url("/exec").toString(), token, diagnostics::add)
    @After fun close() = http.close()

    @Test fun `open period omits end and maps zero versus missing terminal`() = runBlocking {
        http.enqueue(fixture("sales-analytics.json"))
        val report = remote().load(period)
        val request = http.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("sales_analytics", request.requestUrl!!.queryParameter("action"))
        assertEquals("2026-09-01", request.requestUrl!!.queryParameter("startDate"))
        assertNull(request.requestUrl!!.queryParameter("endDate"))
        assertEquals("test-token", request.requestUrl!!.queryParameter("token"))
        assertEquals(12L, report.totalSales)
        assertEquals(listOf(12L, 0L, null), report.stations.map { it.salesCount })
    }

    @Test fun `closed period sends end date`() = runBlocking {
        http.enqueue(fixture("sales-analytics.json").replace("\"endDate\": null", "\"endDate\": \"2026-09-02\""))
        remote().load(period.copy(end = LocalDate.parse("2026-09-02")))
        assertEquals("2026-09-02", http.takeRequest().requestUrl!!.queryParameter("endDate"))
    }

    @Test fun `server error incomplete data and mismatched period are rejected`() {
        for (body in listOf(
            """{"success":false,"error":{"status":500}}""",
            """{"success":true,"data":{}}""",
            fixture("sales-analytics.json").replace("\"totalSales\": 12", "\"totalSales\": 13"),
            fixture("sales-analytics.json").replace("2026-09-01", "2026-09-02"),
            fixture("sales-analytics.json").replace("\"salesCount\": 12", "\"salesCount\": -12")
        )) {
            http.enqueue(body)
            assertThrows(IllegalStateException::class.java) { runBlocking { remote().load(period) } }
        }
    }

    @Test fun `missing credentials and invalid periods do not hit network`() {
        assertThrows(IllegalStateException::class.java) { runBlocking { remote("").load(period) } }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { remote().load(period.copy(end = period.start.minusDays(1))) }
        }
        assertEquals(0, http.server.requestCount)
    }

    @Test fun `HTTP failure and malformed responses cannot appear as zero sales`() {
        http.enqueue("Unauthorized", 401)
        assertThrows(retrofit2.HttpException::class.java) { runBlocking { remote().load(period) } }
        http.enqueue("<html>Sign in</html>")
        assertThrows(Exception::class.java) { runBlocking { remote().load(period) } }
    }

    @Test fun `cancellation propagates`() {
        val cancelledApi = object : GoogleSheetsSyncApi by api {
            override suspend fun salesAnalytics(url: String, action: String, token: String,
                startDate: String, endDate: String?): SalesAnalyticsResponseDto {
                throw CancellationException("test")
            }
        }
        val remote = GoogleSheetsSalesAnalyticsRemoteDataSource(cancelledApi, http.server.url("/exec").toString(), "test-token", diagnostics::add)
        assertThrows(CancellationException::class.java) { runBlocking { remote.load(period) } }
    }

    @Test fun `unsupported action explains outdated deployment and logs safe metadata`() {
        http.enqueue("""{"success":false,"error":{"status":400,"message":"Unsupported action"}}""")
        val error = assertThrows(SalesAnalyticsException::class.java) { runBlocking { remote().load(period) } }
        assertEquals(SalesAnalyticsFailure.API_OUTDATED, error.reason)
        assertTrue(salesAnalyticsErrorMessage(error).contains("Обновите развёртывание"))
        assertTrue(diagnostics.any { it.contains("serverStatus=400") })
        assertTrue(diagnostics.any { it.contains("reason=API_OUTDATED") })
        assertFalse(diagnostics.joinToString().contains("test-token"))
        assertFalse(diagnostics.joinToString().contains("http://"))
        assertFalse(diagnostics.joinToString().contains("https://"))
    }

    @Test fun `arbitrary server message is not copied into diagnostics or user text`() {
        val secret = "private-token-in-upstream-message"
        http.enqueue("""{"success":false,"error":{"status":500,"message":"$secret"}}""")
        val error = assertThrows(SalesAnalyticsException::class.java) { runBlocking { remote().load(period) } }
        assertEquals(SalesAnalyticsFailure.SERVER_ERROR, error.reason)
        assertFalse(diagnostics.joinToString().contains(secret))
        assertFalse(salesAnalyticsErrorMessage(error).contains(secret))
    }

    @Test fun `authorization configuration and network failures have distinct explanations`() {
        assertEquals(SalesAnalyticsFailure.UNAUTHORIZED, salesAnalyticsServerFailure(401, "Unauthorized"))
        assertEquals(SalesAnalyticsFailure.SERVER_CONFIGURATION,
            salesAnalyticsServerFailure(500, "Missing Script Property: VENDISTA_TOKEN"))
        assertEquals(SalesAnalyticsFailure.API_OUTDATED,
            salesAnalyticsServerFailure(500, "getSalesAnalytics is not defined"))
        assertTrue(salesAnalyticsErrorMessage(java.net.UnknownHostException("secret URL")).contains("интернет"))
        assertTrue(salesAnalyticsErrorMessage(java.net.SocketTimeoutException()).contains("не успел"))
    }
}
