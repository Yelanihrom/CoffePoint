package ru.coffeepoint.core.sync.google

object GoogleSheetsSyncConfig {

    const val WEB_APP_URL =
        "https://script.google.com/macros/s/AKfycbyLgwJiFYmAjtLrXCr_rVdYlgFLOhrDrjupqz5uBLMmeZ_EFb6PFIuxj24xXwKUNO71Xw/exec"

    const val SYNC_TOKEN =
        "be3ae4d8ba4718dd99a8645de8d52a5ba047caeeda420f73f9464484cecaca33"

    fun isEnabled(): Boolean {
        return WEB_APP_URL.startsWith("https://") && SYNC_TOKEN.isNotBlank()
    }

}
