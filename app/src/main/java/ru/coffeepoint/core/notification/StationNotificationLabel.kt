package ru.coffeepoint.core.notification

import ru.coffeepoint.feature.station.model.CoffeeStation

internal fun CoffeeStation.notificationLabel(): String {
    val title = name.trim()
    val location = address.trim()
    return when {
        location.isEmpty() || location == title -> title
        title.isEmpty() -> location
        else -> "$title ($location)"
    }
}
