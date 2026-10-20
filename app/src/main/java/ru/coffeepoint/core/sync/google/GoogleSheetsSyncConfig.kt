package ru.coffeepoint.core.sync.google

object GoogleSheetsSyncConfig {

    const val WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbz5IgNIfZOUDhguNgYMk0u8TOao2Dke850Rkxe1oZxrtMB8UJemLjP45nUGcXDV-dJHNg/exec"

    const val SYNC_TOKEN =
        "be3ae4d8ba4718dd99a8645de8d52a5ba047caeeda420f73f9464484cecaca33"

    fun isEnabled(url: String = WEB_APP_URL, token: String = SYNC_TOKEN): Boolean {
        return url.startsWith("https://") && token.isNotBlank()
    }

}
