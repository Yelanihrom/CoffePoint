package ru.coffeepoint.feature.station.model

enum class StockLevel {

    EMPTY,
    LOW,
    MEDIUM,
    FULL

}

fun StockLevel.title(): String {

    return when (this) {
        StockLevel.EMPTY -> "Пусто"
        StockLevel.LOW -> "Мало"
        StockLevel.MEDIUM -> "Средне"
        StockLevel.FULL -> "Много"
    }

}

fun StockLevel.next(): StockLevel {

    return when (this) {

        StockLevel.FULL -> StockLevel.FULL

        StockLevel.MEDIUM -> StockLevel.FULL

        StockLevel.LOW -> StockLevel.MEDIUM

        StockLevel.EMPTY -> StockLevel.LOW

    }

}

fun StockLevel.previous(): StockLevel {

    return when (this) {

        StockLevel.FULL -> StockLevel.MEDIUM

        StockLevel.MEDIUM -> StockLevel.LOW

        StockLevel.LOW -> StockLevel.EMPTY

        StockLevel.EMPTY -> StockLevel.EMPTY

    }

}