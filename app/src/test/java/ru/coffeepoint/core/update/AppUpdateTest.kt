package ru.coffeepoint.core.update

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.network.ApiTestServer
import java.nio.file.Files
import java.security.MessageDigest

class AppUpdateTest {
    private val certificate = "a".repeat(64)
    private val installed = InstalledUpdateApp("ru.coffeepoint", 15, 37, setOf(certificate))
    private fun manifest(validator: UpdateManifestValidator, body: String = "apk") = UpdateManifest(
        1, installed.applicationId, 16, "1.0.16", 26, "CoffeePoint-1.0.16.apk",
        validator.assetUrl("v1.0.16", "CoffeePoint-1.0.16.apk"), body.toByteArray().size.toLong(),
        MessageDigest.getInstance("SHA-256").digest(body.toByteArray()).toHex(), certificate,
        "b".repeat(40), validator.assetUrl("v1.0.16", "placeholder").replace("download/v1.0.16/placeholder", "tag/v1.0.16")
    )

    @Test fun `version signature sdk and application must match`() {
        val validator = UpdateManifestValidator()
        val update = manifest(validator)
        assertEquals(update, validator.parse(update.toJson(), installed))
        assertNull(validator.parse(update.toJson(), installed.copy(versionCode = 16)))
        assertNull(validator.parse(update.toJson(), installed.copy(sdk = 25)))
        assertNull(validator.parse(update.toJson(), installed.copy(certificates = setOf("c".repeat(64)))))
        assertThrows(InvalidUpdateException::class.java) { validator.parse(update.copy(applicationId = "other").toJson(), installed) }
        assertThrows(InvalidUpdateException::class.java) { validator.parse(update.copy(apkUrl = "https://example.com/app.apk").toJson(), installed) }
        assertThrows(InvalidUpdateException::class.java) { validator.parse(update.toJson().replace("\"schemaVersion\":1", "\"schemaVersion\":4294967297"), installed) }
    }

    @Test fun `successful checks are daily and failures retry after hour`() {
        val now = 10 * UpdateCheckPolicy.DAY
        assertTrue(UpdateCheckPolicy.shouldCheck(now, 0, 0))
        assertFalse(UpdateCheckPolicy.shouldCheck(now, now - 1000, 0))
        assertFalse(UpdateCheckPolicy.shouldCheck(now, 0, now - 1000))
        assertTrue(UpdateCheckPolicy.shouldCheck(now, now - UpdateCheckPolicy.DAY, now - UpdateCheckPolicy.RETRY))
        assertTrue(UpdateCheckPolicy.shouldCheck(now, now + 1, now + 1))
    }

    @Test fun `manual check bypasses success and retry intervals`() {
        val now = 10 * UpdateCheckPolicy.DAY
        assertFalse(UpdateCheckPolicy.shouldCheck(now, now, now))
        assertTrue(UpdateCheckPolicy.shouldCheck(now, now, now, manual = true))
        assertTrue(UpdateCheckPolicy.shouldCheck(now, 0, now, manual = true))
    }

    @Test fun `latest published manifest is read without credentials`() = runBlocking {
        ApiTestServer().use { http ->
            val source = AppUpdateRemoteSource(http.client, apiBase = http.server.url("/"), downloadBase = http.server.url("/"))
            val update = manifest(source.validator)
            val assets = JsonArray().apply {
                add(JsonObject().apply { addProperty("name", "update.json"); addProperty("browser_download_url", source.validator.assetUrl("v1.0.16", "update.json")) })
                add(JsonObject().apply { addProperty("name", update.apkName); addProperty("browser_download_url", update.apkUrl); addProperty("size", update.apkSize) })
            }
            http.enqueue(JsonObject().apply {
                addProperty("draft", false); addProperty("prerelease", false); addProperty("tag_name", "v1.0.16"); add("assets", assets)
            }.toString())
            http.enqueue(update.toJson())
            assertEquals(update, source.check(installed))
            assertEquals("/repos/Yelanihrom/CoffePoint/releases/latest", http.takeRequest().path)
            assertNull(http.takeRequest().getHeader("Authorization"))
        }
    }

    @Test fun `old releases without manifest are ignored`() = runBlocking {
        ApiTestServer().use { http ->
            http.enqueue("""{"draft":false,"prerelease":false,"tag_name":"v1.0.15","assets":[]}""")
            assertNull(AppUpdateRemoteSource(http.client, apiBase = http.server.url("/")).check(installed))
        }
    }

    @Test fun `private or unavailable release does not become an update`() = runBlocking {
        ApiTestServer().use { http ->
            http.enqueue("{}", 404)
            try {
                AppUpdateRemoteSource(http.client, apiBase = http.server.url("/")).check(installed)
                fail("HTTP error accepted")
            } catch (_: java.io.IOException) { }
        }
    }

    @Test fun `download validates checksum and removes failed partial file`() = runBlocking {
        ApiTestServer().use { http ->
            val source = AppUpdateRemoteSource(http.client, downloadBase = http.server.url("/"))
            val update = manifest(source.validator)
            val directory = Files.createTempDirectory("app-update-test").toFile()
            try {
                http.server.enqueue(MockResponse().setBody("apk"))
                val progress = mutableListOf<Int>()
                val file = source.download(update, directory, progress::add)
                assertEquals("apk", file.readText())
                assertEquals(100, progress.last())
                file.delete()
                http.server.enqueue(MockResponse().setBody("bad"))
                try { source.download(update, directory) {}; fail("Corrupt APK accepted") }
                catch (_: InvalidUpdateException) { }
                assertTrue(directory.listFiles().orEmpty().isEmpty())
                http.server.enqueue(MockResponse().setResponseCode(404))
                try { source.download(update, directory) {}; fail("HTTP error accepted") }
                catch (_: java.io.IOException) { }
                assertTrue(directory.listFiles().orEmpty().isEmpty())
            } finally { directory.deleteRecursively() }
        }
    }
}
