package ru.coffeepoint.core.sync.google

import ru.coffeepoint.BuildConfig

object GoogleSheetsSyncConfig {

    internal const val PRODUCTION_WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbzQvnaFl8no5VwXMFyNijnLcBqIe8qkss3e1dviwXHIh8DyndsW1FsaL4iyHBHw6ao/exec"

    private const val PRODUCTION_SYNC_TOKEN =
        "be3ae4d8ba4718dd99a8645de8d52a5ba047caeeda420f73f9464484cecaca33"

    val WEB_APP_URL: String = if (BuildConfig.IS_TEST_ENVIRONMENT) {
        BuildConfig.TEST_SHEETS_WEB_APP_URL
    } else {
        PRODUCTION_WEB_APP_URL
    }

    val SYNC_TOKEN: String = if (BuildConfig.IS_TEST_ENVIRONMENT) {
        BuildConfig.TEST_SHEETS_SYNC_TOKEN
    } else {
        PRODUCTION_SYNC_TOKEN
    }

    fun isEnabled(url: String = WEB_APP_URL, token: String = SYNC_TOKEN): Boolean {
        if (BuildConfig.IS_TEST_ENVIRONMENT && url == PRODUCTION_WEB_APP_URL) return false
        return url.startsWith("https://") && token.isNotBlank()
    }

}
