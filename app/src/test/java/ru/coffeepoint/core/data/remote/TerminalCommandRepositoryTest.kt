package ru.coffeepoint.core.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import ru.coffeepoint.core.data.remote.api.TerminalCommandApi
import ru.coffeepoint.core.data.remote.repository.TerminalCommandRepository
import ru.coffeepoint.core.network.ApiTestServer
import ru.coffeepoint.core.network.jsonBody

class TerminalCommandRepositoryTest {
    private lateinit var http: ApiTestServer
    private lateinit var repository: TerminalCommandRepository

    @Before fun setUp() {
        http = ApiTestServer()
        repository = TerminalCommandRepository(http.api<TerminalCommandApi>(), "test-vendista-token")
    }

    @After fun tearDown() = http.close()

    @Test fun `reboot posts command 2 to the selected terminal`() = runBlocking {
        http.enqueue()
        repository.rebootTerminal(258576)
        val request = http.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/terminals/258576/commands", request.requestUrl!!.encodedPath)
        assertEquals("test-vendista-token", request.requestUrl!!.queryParameter("token"))
        val body = request.jsonBody()
        assertEquals(2, body["command_id"].asInt)
        assertFalse(body.has("parameter1"))
    }

    @Test fun `turn screen off sends command 15 with parameter 1`() = runBlocking {
        http.enqueue()
        repository.turnScreenOff(258576)
        val body = http.takeRequest().jsonBody()
        assertEquals(15, body["command_id"].asInt)
        assertEquals(1, body["parameter1"].asInt)
    }

    @Test fun `turn screen on sends command 15 with parameter 0`() = runBlocking {
        http.enqueue()
        repository.turnScreenOn(258576)
        val body = http.takeRequest().jsonBody()
        assertEquals(15, body["command_id"].asInt)
        assertEquals(0, body["parameter1"].asInt)
    }

    @Test fun `HTTP failure propagates instead of confirming terminal command`() {
        http.enqueue("""{"success":false}""", 503)
        val error = assertThrows(HttpException::class.java) {
            runBlocking { repository.rebootTerminal(258576) }
        }
        assertEquals(503, error.code())
        assertEquals(1, http.server.requestCount)
    }
}
