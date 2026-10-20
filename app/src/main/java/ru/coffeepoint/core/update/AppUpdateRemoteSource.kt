package ru.coffeepoint.core.update

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class AppUpdateRemoteSource(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .followSslRedirects(false)
        .build(),
    private val repository: String = "Yelanihrom/CoffePoint",
    private val apiBase: HttpUrl = "https://api.github.com/".toHttpUrl(),
    private val downloadBase: HttpUrl = "https://github.com/".toHttpUrl(),
    val validator: UpdateManifestValidator = UpdateManifestValidator(repository, downloadBase)
) {
    private val downloadCall = AtomicReference<Call?>(null)

    suspend fun check(installed: InstalledUpdateApp): UpdateManifest? = withContext(Dispatchers.IO) {
        val latestUrl = apiBase.newBuilder().addPathSegments("repos/$repository/releases/latest").build()
        val release = JsonParser.parseString(readJson(latestUrl, 512 * 1024)).asJsonObject
        if (release.get("draft")?.asBoolean != false || release.get("prerelease")?.asBoolean != false) {
            return@withContext null
        }
        val tag = release.text("tag_name")
        if (!tag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+"))) return@withContext null
        val assets = release.getAsJsonArray("assets") ?: throw InvalidUpdateException()
        val manifests = assets.map { it.asJsonObject }.filter { it.text("name") == "update.json" }
        if (manifests.isEmpty()) return@withContext null
        if (manifests.size != 1) throw InvalidUpdateException()
        val manifestUrl = validator.assetUrl(tag, "update.json")
        if (manifests.single().text("browser_download_url") != manifestUrl) throw InvalidUpdateException()
        val manifest = validator.parse(readJson(manifestUrl.toHttpUrl(), 32 * 1024), installed)
            ?: return@withContext null
        if (tag != "v${manifest.versionName}") throw InvalidUpdateException()
        val apks = assets.map { it.asJsonObject }.filter { it.text("name") == manifest.apkName }
        if (apks.size != 1 || apks.single().text("browser_download_url") != manifest.apkUrl ||
            apks.single().number("size") != manifest.apkSize) throw InvalidUpdateException()
        manifest
    }

    private fun readJson(url: HttpUrl, limit: Int): String {
        val call = client.newCall(request(url))
        call.timeout().timeout(15, TimeUnit.SECONDS)
        return call.execute().use { response ->
            if (!response.isSuccessful) throw IOException("Update HTTP ${response.code}")
            validateResponseUrl(response.request.url)
            val body = response.body ?: throw IOException("Empty update response")
            body.byteStream().use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > limit) throw InvalidUpdateException()
                    output.write(buffer, 0, count)
                }
                output.toString(Charsets.UTF_8.name())
            }
        }
    }

    suspend fun download(manifest: UpdateManifest, directory: File, progress: (Int) -> Unit): File =
        withContext(Dispatchers.IO) {
            if (manifest.apkUrl != validator.assetUrl("v${manifest.versionName}", manifest.apkName) ||
                manifest.apkSize !in 1..UpdateManifestValidator.MAX_APK_SIZE) throw InvalidUpdateException()
            if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Update directory unavailable")
            val temporary = File(directory, "${UUID.randomUUID()}.part")
            val ready = File(directory, "${UUID.randomUUID()}.apk")
            val call = client.newCall(request(manifest.apkUrl.toHttpUrl()))
            if (!downloadCall.compareAndSet(null, call)) throw IOException("Download already running")
            try {
                currentCoroutineContext().ensureActive()
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("APK HTTP ${response.code}")
                    validateResponseUrl(response.request.url)
                    val body = response.body ?: throw IOException("Empty APK response")
                    if (body.contentLength() >= 0 && body.contentLength() != manifest.apkSize) throw InvalidUpdateException()
                    copyApk(body, temporary, manifest, progress)
                }
                currentCoroutineContext().ensureActive()
                if (!temporary.renameTo(ready)) throw IOException("Cannot finalize APK")
                ready
            } catch (error: Exception) {
                ready.delete()
                throw error
            } finally {
                call.cancel()
                downloadCall.compareAndSet(call, null)
                temporary.delete()
            }
        }

    private suspend fun copyApk(body: ResponseBody, target: File, manifest: UpdateManifest, progress: (Int) -> Unit) {
        val digest = MessageDigest.getInstance("SHA-256")
        var total = 0L
        var reported = -1
        body.byteStream().use { input ->
            target.outputStream().use { output ->
                val buffer = ByteArray(32768)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > manifest.apkSize) throw InvalidUpdateException()
                    output.write(buffer, 0, count)
                    digest.update(buffer, 0, count)
                    val percent = (total * 100 / manifest.apkSize).toInt()
                    if (percent != reported) { progress(percent); reported = percent }
                }
            }
        }
        if (total != manifest.apkSize || digest.digest().toHex() != manifest.sha256) throw InvalidUpdateException()
    }

    fun cancelDownload() { downloadCall.getAndSet(null)?.cancel() }

    private fun request(url: HttpUrl) = Request.Builder().url(url)
        .header("User-Agent", "CoffeePoint-Android")
        .header("Accept", if (url.host == apiBase.host && url.encodedPath.startsWith("/repos/")) "application/vnd.github+json" else "application/octet-stream")
        .build()

    private fun validateResponseUrl(url: HttpUrl) {
        val allowed = setOf(downloadBase.host, apiBase.host, "release-assets.githubusercontent.com", "objects.githubusercontent.com", "github-releases.githubusercontent.com")
        if (!url.isHttps || url.host !in allowed) throw InvalidUpdateException()
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
