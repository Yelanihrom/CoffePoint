package ru.coffeepoint.core.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

object AppUpdatePlatform {
    @Suppress("DEPRECATION")
    private val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES

    @Suppress("DEPRECATION")
    private fun certificates(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures.orEmpty().map { MessageDigest.getInstance("SHA-256").digest(it.toByteArray()).toHex() }.toSet()
    }

    @Suppress("DEPRECATION")
    private fun version(info: PackageInfo): Long = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    @Suppress("DEPRECATION")
    fun installed(context: Context): InstalledUpdateApp {
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        return InstalledUpdateApp(context.packageName, version(info), Build.VERSION.SDK_INT, certificates(info))
    }

    @Suppress("DEPRECATION")
    fun verify(context: Context, file: File, manifest: UpdateManifest) {
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: throw InvalidUpdateException()
        val current = installed(context)
        if (info.packageName != current.applicationId || version(info) != manifest.versionCode ||
            info.versionName != manifest.versionName || version(info) <= current.versionCode ||
            certificates(info) != current.certificates ||
            certificates(info) != setOf(manifest.signingCertificateSha256) ||
            info.applicationInfo?.minSdkVersion != manifest.minSdk || manifest.minSdk > Build.VERSION.SDK_INT) throw InvalidUpdateException()
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun permissionIntent(context: Context) = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun installIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        return Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply { clipData = ClipData.newRawUri("APK", uri) }
    }
}
