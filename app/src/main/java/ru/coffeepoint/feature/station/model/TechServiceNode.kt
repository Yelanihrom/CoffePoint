package ru.coffeepoint.feature.station.model

enum class TechServiceNode(val title: String, val defaultIntervalDays: Int) {
    LIQUID_WASTE_CONTAINER("Контейнер жидких отходов", 180),
    BREW_UNIT("УЗК", 180),
    COFFEE_GRINDER("Кофемолка", 720),
    MIXERS("Миксеры", 30),
    EXPANSION_TANK("Расширительный бачок", 360),
    EXHAUST("Вытяжка", 720)
}
