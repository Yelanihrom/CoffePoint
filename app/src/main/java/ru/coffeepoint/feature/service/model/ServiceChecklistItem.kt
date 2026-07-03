package ru.coffeepoint.feature.service.model

data class ServiceChecklistItem(
    val title: String,
    val description: String,
    var checked: Boolean = false
)