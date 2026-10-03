package ru.coffeepoint.core.sync.google

object GoogleSheetsSyncDefaults {

    val noErrorType: String
        get() = GoogleSheetsSyncErrorType.NONE.storageValue

}
