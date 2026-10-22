package ru.coffeepoint.core.sync.google

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.coffeepoint.core.data.local.entity.*
import ru.coffeepoint.core.network.ApiTestServer
import ru.coffeepoint.core.network.fixture
import ru.coffeepoint.core.network.jsonBody
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi
import ru.coffeepoint.core.sync.google.dto.GoogleSheetsSyncRequestDto
import ru.coffeepoint.core.sync.google.dto.GoogleSheetsSyncResponseDto
import ru.coffeepoint.feature.station.model.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.InetAddress

class GoogleSheetsRemoteDataSourceTest {
    private lateinit var http: ApiTestServer
    private lateinit var remote: GoogleSheetsRemoteDataSource

    @Before fun setUp() {
        http = ApiTestServer()
        remote = dataSource()
    }

    @After fun tearDown() = http.close()

    private fun dataSource(client: OkHttpClient = http.client, token: String = "test-token") =
        GoogleSheetsRemoteDataSource(
            api = http.api<GoogleSheetsSyncApi>(client),
            webAppUrl = http.server.url("/exec").newBuilder().host("localhost").build().toString(),
            syncToken = token,
            reportFailure = { _, _ -> }
        )

    @Test fun `operator service start excludes warehouse and settings`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        val snapshot = remote.loadServiceStart(7, includeWarehouse = false)
        val request = http.takeRequest().requestUrl!!
        assertEquals("full_sync", request.queryParameter("action"))
        assertEquals("7", request.queryParameter("stationId"))
        assertEquals("coffee_station,station_inventory", request.queryParameter("entities"))
        assertEquals(7, snapshot.station.id)
        assertEquals(6, snapshot.inventory.cups)
        assertTrue(snapshot.warehouse.isEmpty())
    }

    @Test fun `supplier service start includes warehouse but not settings`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        val snapshot = remote.loadServiceStart(7, includeWarehouse = true)
        val request = http.takeRequest().requestUrl!!
        assertEquals("coffee_station,station_inventory,warehouse_stock", request.queryParameter("entities"))
        assertEquals("7", request.queryParameter("stationId"))
        assertEquals(36, snapshot.warehouse.single().quantity)
    }

    @Test fun `service start rejects missing station or inventory`() = runBlocking {
        for (response in listOf(
            """{"success":true,"data":{"stations":[],"inventories":[]}}""",
            """{"success":false,"error":{"status":500,"message":"Unavailable"}}""",
            fixture("full-sync.json").replace("\"cups\":6,", "")
        )) {
            http.enqueue(response)
            try { remote.loadServiceStart(7, false); fail("Incomplete data accepted") }
            catch (_: IllegalStateException) { }
            catch (_: NoSuchElementException) { }
        }
    }

    @Test fun `full import requests all entities and maps the server contract`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        val bundle = remote.loadFullSyncBundle()
        val request = http.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/exec", request.requestUrl!!.encodedPath)
        assertEquals("full_sync", request.requestUrl!!.queryParameter("action"))
        assertEquals("test-token", request.requestUrl!!.queryParameter("token"))
        assertNull(request.requestUrl!!.queryParameter("entities"))
        assertEquals(54, bundle.stations.single().station.salesAfterService)
        assertEquals(258576L, bundle.stations.single().station.terminalId)
        assertEquals(6, bundle.stations.single().inventory.cups)
        assertEquals(3, bundle.stations.single().settings.cupsMin)
        assertEquals(12, bundle.stations.single().settings.cupsMax)
        assertEquals("1747490839", bundle.supplyCatalog.single().article)
        assertEquals(24, bundle.warehouseStock.single().minQuantity)
        assertEquals(48, bundle.warehouseStock.single().maxQuantity)
        assertEquals("2026-09-13", bundle.employees.single().workStartDate)
        assertNull(bundle.employeeStationAssignments.single().validTo)
        assertEquals("auto-earning-1-2026-10-18", bundle.employeeTransactions.single().id)
        assertEquals(180, bundle.employeeTransactions.single().amount)
    }

    @Test fun `dashboard import selects stations inventory settings and warehouse`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        remote.loadFullSyncBundle(includeSupplyCatalog = false, includeEmployeeData = false)
        assertEquals("coffee_station,station_inventory,station_settings,warehouse_stock",
            http.takeRequest().requestUrl!!.queryParameter("entities"))
    }

    @Test fun `warehouse refresh requests only warehouse rows`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        remote.loadFullSyncBundle(includeStations = false, includeSettings = false,
            includeSupplyCatalog = false, includeEmployeeData = false)
        assertEquals("warehouse_stock", http.takeRequest().requestUrl!!.queryParameter("entities"))
    }

    @Test fun `earnings refresh requests all three employee entities only`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        remote.loadFullSyncBundle(includeStations = false, includeSettings = false,
            includeSupplyCatalog = false, includeWarehouseStock = false)
        assertEquals("employees,employee_station_assignments,employees_transactions",
            http.takeRequest().requestUrl!!.queryParameter("entities"))
    }

    @Test fun `catalog refresh requests only catalog and preserves numeric articles`() = runBlocking {
        http.enqueue(fixture("full-sync.json"))
        val items = remote.loadSupplyCatalog()
        assertEquals("supply_catalog", http.takeRequest().requestUrl!!.queryParameter("entities"))
        assertEquals("1747490839", items.single().article)
    }

    @Test fun `operator completion sends service fields without the full station row`() = runBlocking {
        http.enqueue()
        assertTrue(remote.syncOperatorServiceFields(detail().station).synced)
        val request = http.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        val body = request.jsonBody()
        assertEquals("complete_service", body["action"].asString)
        assertEquals("coffee_station", body["entity"].asString)
        assertEquals("test-token", body["token"].asString)
        val data = body.getAsJsonObject("data")
        assertEquals(setOf("id", "lastServiceDate", "salesAfterService", "salesUpdatedAt"), data.keySet())
        assertEquals(0, data["salesAfterService"].asInt)
        assertEquals(SERVICE_DATE, data["salesUpdatedAt"].asString)
        assertEquals(SERVICE_DATE, data["lastServiceDate"].asString)
    }

    @Test fun `supplier completion sends only checked maintenance nodes`() = runBlocking {
        http.enqueue()
        val result = remote.syncSupplyServiceFields(detail().station, true, false)
        assertTrue(result.synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("complete_service", body["action"].asString)
        assertEquals(setOf("id", "mixersServiceDate"), body.getAsJsonObject("data").keySet())
        assertEquals("2026-10-19", body.getAsJsonObject("data")["mixersServiceDate"].asString)
    }

    @Test fun `supplier can send both maintenance nodes without operator fields`() = runBlocking {
        http.enqueue()
        remote.syncSupplyServiceFields(detail().station, true, true)
        assertEquals(setOf("id", "mixersServiceDate", "liquidWasteContainerServiceDate"),
            http.takeRequest().jsonBody().getAsJsonObject("data").keySet())
    }

    @Test fun `supplier without checked nodes makes no request`() = runBlocking {
        val result = remote.syncSupplyServiceFields(detail().station, false, false)
        assertFalse(result.synced)
        assertEquals(GoogleSheetsSyncErrorType.NONE, result.errorType)
        assertEquals(0, http.server.requestCount)
    }

    @Test fun `inventory upload is isolated and keeps the local update timestamp`() = runBlocking {
        http.enqueue()
        val result = remote.syncStationDetail(detail(), false, true, false)
        assertTrue(result.inventory.synced)
        assertFalse(result.station.synced)
        assertFalse(result.settings.synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("upsert", body["action"].asString)
        assertEquals("station_inventory", body["entity"].asString)
        assertEquals(LOCAL_TIMESTAMP, body.getAsJsonObject("data")["updatedAt"].asString)
        assertEquals(1, http.server.requestCount)
    }

    @Test fun `settings upload includes all minimum maximum pairs and no other entities`() = runBlocking {
        http.enqueue()
        val result = remote.syncStationDetail(detail(), false, false, true)
        assertTrue(result.settings.synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("station_settings", body["entity"].asString)
        val data = body.getAsJsonObject("data")
        for (item in ITEMS) {
            assertTrue("Missing ${item}Min", data.has("${item}Min"))
            assertTrue("Missing ${item}Max", data.has("${item}Max"))
        }
        assertEquals(LOCAL_TIMESTAMP, data["updatedAt"].asString)
        assertEquals(1, http.server.requestCount)
    }

    @Test fun `station settings save does not upload server sales counters`() = runBlocking {
        http.enqueue()
        remote.syncStationDetail(detail(), true, false, false)
        val body = http.takeRequest().jsonBody()
        assertEquals("coffee_station", body["entity"].asString)
        assertFalse(body.getAsJsonObject("data").has("salesAfterService"))
        assertFalse(body.getAsJsonObject("data").has("salesUpdatedAt"))
    }

    @Test fun `nine warehouse rows are sent in a single batch including limits`() = runBlocking {
        http.enqueue()
        assertTrue(remote.syncWarehouseStock(ITEMS.map { warehouse(it) }).synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("upsert_batch", body["action"].asString)
        assertEquals("warehouse_stock", body["entity"].asString)
        val items = body.getAsJsonObject("data").getAsJsonArray("items")
        assertEquals(9, items.size())
        for (item in items) {
            val row = item.asJsonObject
            assertEquals(LOCAL_TIMESTAMP, row["updatedAt"].asString)
            assertEquals(2, row["minQuantity"].asInt)
            assertEquals(12, row["maxQuantity"].asInt)
            assertEquals(setOf("itemType", "quantity", "minQuantity", "maxQuantity", "updatedAt"), row.keySet())
        }
        assertEquals(1, http.server.requestCount)
    }

    @Test fun `empty warehouse batch makes no request`() = runBlocking {
        remote.syncWarehouseStock(emptyList())
        assertEquals(0, http.server.requestCount)
    }

    @Test fun `employee and assignment use separate entities and preserve identifiers`() = runBlocking {
        http.enqueue()
        http.enqueue()
        val employee = EmployeeEntity(1, "Test Operator", "OPERATOR", "2026-09-13", true,
            LOCAL_TIMESTAMP, LOCAL_TIMESTAMP)
        val assignment = EmployeeStationAssignmentEntity("assignment-1", 1, 7, "2026-09-13", null,
            LOCAL_TIMESTAMP, LOCAL_TIMESTAMP)
        assertTrue(remote.syncEmployeesAndAssignments(listOf(employee), listOf(assignment)).synced)
        val first = http.takeRequest().jsonBody()
        val second = http.takeRequest().jsonBody()
        assertEquals("employees", first["entity"].asString)
        assertEquals("employee_station_assignments", second["entity"].asString)
        assertEquals("assignment-1", second.getAsJsonObject("data")["id"].asString)
        assertFalse(first.getAsJsonObject("data").has("isSynced"))
    }

    @Test fun `payment upload preserves UUID amount and local timestamp`() = runBlocking {
        http.enqueue()
        val transaction = EmployeeTransactionEntity("payment-uuid", 1, "PAYMENT", LOCAL_TIMESTAMP,
            null, null, null, 15000, "", LOCAL_TIMESTAMP, LOCAL_TIMESTAMP)
        assertTrue(remote.syncEmployeeTransactions(listOf(transaction)).synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("employees_transactions", body["entity"].asString)
        val data = body.getAsJsonObject("data")
        assertEquals("payment-uuid", data["id"].asString)
        assertEquals(15000, data["amount"].asInt)
        assertEquals(LOCAL_TIMESTAMP, data["updatedAt"].asString)
        assertFalse(data.has("isSynced"))
        assertFalse(data.has("syncError"))
    }

    @Test fun `catalog upsert preserves optional strings and caller timestamp`() = runBlocking {
        http.enqueue()
        val option = SupplyCatalogEntity(10, "cups", "Paper cups", "Supplier", "55011", "", 200)
        assertTrue(remote.upsertSupplyCatalogOption(option, LOCAL_TIMESTAMP).synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("upsert", body["action"].asString)
        assertEquals("supply_catalog", body["entity"].asString)
        assertEquals("55011", body.getAsJsonObject("data")["article"].asString)
        assertEquals("", body.getAsJsonObject("data")["url"].asString)
        assertEquals(LOCAL_TIMESTAMP, body.getAsJsonObject("data")["updatedAt"].asString)
    }

    @Test fun `catalog delete sends action delete and only the option id`() = runBlocking {
        http.enqueue()
        assertTrue(remote.deleteSupplyCatalogOption(10).synced)
        val body = http.takeRequest().jsonBody()
        assertEquals("delete", body["action"].asString)
        assertEquals("supply_catalog", body["entity"].asString)
        assertEquals(setOf("id"), body.getAsJsonObject("data").keySet())
    }

    @Test fun `HTTP 200 with embedded 409 is a conflict not a connection error`() = runBlocking {
        http.enqueue("""{"success":false,"error":{"status":409,"message":"Remote is newer"}}""")
        assertEquals(GoogleSheetsSyncErrorType.CONFLICT,
            remote.syncWarehouseStock(listOf(warehouse())).errorType)
    }

    @Test fun `missing server token is not reported as an internet failure`() = runBlocking {
        http.enqueue("""{"success":false,"error":{"status":500,"message":"SYNC_TOKEN is not configured"}}""")
        val result = remote.syncWarehouseStock(listOf(warehouse()))
        assertFalse(result.synced)
        assertEquals(GoogleSheetsSyncErrorType.UNKNOWN, result.errorType)
    }

    @Test fun `HTTP authorization failure cannot be treated as a successful write`() = runBlocking {
        http.enqueue("""{"error":"Unauthorized"}""", 401)
        val result = remote.syncWarehouseStock(listOf(warehouse()))
        assertFalse(result.synced)
        assertEquals(GoogleSheetsSyncErrorType.UNKNOWN, result.errorType)
    }

    @Test fun `HTML response is rejected instead of marking rows synced`() = runBlocking {
        http.enqueue("<html>Sign in</html>")
        assertFalse(remote.syncWarehouseStock(listOf(warehouse())).synced)
    }

    @Test fun `DNS resolution failure is classified as connection error`() = runBlocking {
        val client = http.client.newBuilder().dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = throw UnknownHostException("test DNS")
        }).build()
        assertEquals(GoogleSheetsSyncErrorType.CONNECTION,
            dataSource(client).syncWarehouseStock(listOf(warehouse())).errorType)
    }

    @Test fun `socket timeout is classified as connection error`() = runBlocking {
        val client = http.client.newBuilder().addInterceptor { throw SocketTimeoutException("test timeout") }.build()
        assertEquals(GoogleSheetsSyncErrorType.CONNECTION,
            dataSource(client).syncWarehouseStock(listOf(warehouse())).errorType)
    }

    @Test fun `coroutine cancellation propagates to the caller`() {
        val api = object : GoogleSheetsSyncApi by http.api<GoogleSheetsSyncApi>() {
            override suspend fun upsert(url: String, request: GoogleSheetsSyncRequestDto): GoogleSheetsSyncResponseDto {
                throw CancellationException("cancelled")
            }
        }
        val cancelledRemote = GoogleSheetsRemoteDataSource(api, http.server.url("/exec").toString(),
            "test-token", reportFailure = { _, _ -> })
        assertThrows(CancellationException::class.java) {
            runBlocking { cancelledRemote.syncWarehouseStock(listOf(warehouse())) }
        }
    }

    @Test fun `missing local timestamp blocks upload before any request`() = runBlocking {
        val result = remote.syncWarehouseStock(listOf(warehouse().copy(updatedAt = "")))
        assertFalse(result.synced)
        assertEquals(0, http.server.requestCount)
    }

    @Test fun `header mismatch during import reaches caller with server reason`() {
        http.enqueue("""{"success":false,"error":{"status":500,"message":"Sheet headers mismatch: warehouse_stock"}}""")
        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { remote.loadFullSyncBundle() }
        }
        assertEquals("Sheet headers mismatch: warehouse_stock", error.message)
    }

    @Test fun `empty token blocks upload without accessing the network`() = runBlocking {
        val result = dataSource(token = "").syncWarehouseStock(listOf(warehouse()))
        assertEquals(GoogleSheetsSyncErrorType.CONNECTION, result.errorType)
        assertEquals(0, http.server.requestCount)
    }

    private fun warehouse(itemType: String = "coffee") = WarehouseStockEntity(
        itemType, quantity = 6, minQuantity = 2, maxQuantity = 12, updatedAt = LOCAL_TIMESTAMP)

    private fun detail() = StationDetail(
        CoffeeStation(7, "Test Station", lastServiceDate = SERVICE_DATE, salesAfterService = 0,
            mixersServiceDate = "2026-10-19", liquidWasteContainerServiceDate = "2026-10-19",
            updatedAt = LOCAL_TIMESTAMP),
        StationInventory(6, 4, 3, 2, 1, 0, 1, 2, updatedAt = LOCAL_TIMESTAMP),
        StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10,
            chocolateMax = 10, toppingMax = 10, updatedAt = LOCAL_TIMESTAMP)
    )

    companion object {
        private const val LOCAL_TIMESTAMP = "2026-10-18T10:07:00"
        private const val SERVICE_DATE = "19.10.2026 10:07"
        private val ITEMS = listOf("cups", "lids", "coffee", "milk", "chocolate", "topping", "sugar", "stirrers", "garbageBags")
    }
}
