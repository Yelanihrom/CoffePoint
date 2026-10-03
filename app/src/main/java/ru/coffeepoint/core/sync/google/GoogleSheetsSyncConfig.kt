package ru.coffeepoint.core.sync.google

object GoogleSheetsSyncConfig {

    const val WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbwTOd69Y5NC9-ZN060ZBe8zw3R2cY_ViV3QTDaeLMCqo_oJp1jxNMzaZZDwwCxPzHBC6w/exec"

    const val SYNC_TOKEN =
        "be3ae4d8ba4718dd99a8645de8d52a5ba047caeeda420f73f9464484cecaca33"

    fun isEnabled(): Boolean {
        return WEB_APP_URL.startsWith("https://") && SYNC_TOKEN.isNotBlank()
    }

}
