package ru.coffeepoint.core.update

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

data class InstalledUpdateApp(
    val applicationId: String,
    val versionCode: Long,
    val sdk: Int,
    val certificates: Set<String>
)

data class UpdateManifest(
    val schemaVersion: Int,
    val applicationId: String,
    val versionCode: Long,
    val versionName: String,
    val minSdk: Int,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String,
    val signingCertificateSha256: String,
    val sourceCommit: String,
    val releaseUrl: String
) {
    fun toJson(): String = Gson().toJson(this)
}

class InvalidUpdateException : Exception("Update validation failed")

object UpdateCheckPolicy {
    const val DAY = 24 * 60 * 60 * 1000L
    const val RETRY = 60 * 60 * 1000L

    fun shouldCheck(now: Long, lastSuccess: Long, lastAttempt: Long): Boolean {
        fun due(time: Long, interval: Long) = time <= 0 || now < time || now - time >= interval
        return due(lastSuccess, DAY) && due(lastAttempt, RETRY)
    }
}

class UpdateManifestValidator(
    private val repository: String = "Yelanihrom/CoffePoint",
    private val downloadBase: HttpUrl = "https://github.com/".toHttpUrl()
) {
    companion object { const val MAX_APK_SIZE = 200 * 1024 * 1024L }

    fun assetUrl(tag: String, name: String): String {
        if (!tag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+")) ||
            !name.matches(Regex("[A-Za-z0-9._-]+"))) throw InvalidUpdateException()
        return downloadBase.newBuilder()
            .addPathSegments("$repository/releases/download/$tag")
            .addPathSegment(name).build().toString()
    }

    fun parse(json: String, installed: InstalledUpdateApp): UpdateManifest? {
        try {
            val value = JsonParser.parseString(json).asJsonObject
            val manifest = UpdateManifest(
                schemaVersion = Math.toIntExact(value.number("schemaVersion")),
                applicationId = value.text("applicationId"),
                versionCode = value.number("versionCode"),
                versionName = value.text("versionName"),
                minSdk = Math.toIntExact(value.number("minSdk")),
                apkName = value.text("apkName"),
                apkUrl = value.text("apkUrl"),
                apkSize = value.number("apkSize"),
                sha256 = value.text("sha256"),
                signingCertificateSha256 = value.text("signingCertificateSha256"),
                sourceCommit = value.text("sourceCommit"),
                releaseUrl = value.text("releaseUrl")
            )
            if (manifest.schemaVersion != 1 || manifest.applicationId != installed.applicationId ||
                manifest.minSdk < 1 || manifest.apkSize !in 1..MAX_APK_SIZE ||
                !manifest.versionName.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")) ||
                manifest.apkName != "CoffeePoint-${manifest.versionName}.apk" ||
                !manifest.sha256.matches(Regex("[a-f0-9]{64}")) ||
                !manifest.signingCertificateSha256.matches(Regex("[a-f0-9]{64}")) ||
                !manifest.sourceCommit.matches(Regex("[a-f0-9]{40}")) ||
                manifest.apkUrl != assetUrl("v${manifest.versionName}", manifest.apkName)) {
                throw InvalidUpdateException()
            }
            val expectedReleaseUrl = downloadBase.newBuilder()
                .addPathSegments("$repository/releases/tag/v${manifest.versionName}").build().toString()
            if (manifest.releaseUrl != expectedReleaseUrl) throw InvalidUpdateException()
            if (manifest.versionCode <= installed.versionCode || manifest.minSdk > installed.sdk ||
                installed.certificates != setOf(manifest.signingCertificateSha256)) return null
            return manifest
        } catch (error: InvalidUpdateException) {
            throw error
        } catch (_: Exception) {
            throw InvalidUpdateException()
        }
    }
}

internal fun JsonObject.text(name: String): String {
    val value = get(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
    if (value == null || !value.isString) throw InvalidUpdateException()
    return value.asString
}

internal fun JsonObject.number(name: String): Long {
    val value = get(name)?.takeIf { it.isJsonPrimitive }?.asJsonPrimitive
    if (value == null || !value.isNumber) throw InvalidUpdateException()
    return value.asBigDecimal.longValueExact()
}
