package ru.coffeepoint.feature.supplycatalog.model

enum class SupplyCatalogItemType(
    val code: String,
    val label: String,
    val order: Int
) {
    Cups("cups", "Стаканы", 1),
    Lids("lids", "Крышки", 2),
    Sugar("sugar", "Сахар", 3),
    Stirrers("stirrers", "Мешалки", 4),
    GarbageBags("garbageBags", "Мусорные пакеты", 5),
    Coffee("coffee", "Кофе", 6),
    Milk("milk", "Молоко", 7),
    Chocolate("chocolate", "Шоколад", 8),
    Topping("topping", "Топпинг", 9);

    companion object {
        fun fromCode(code: String): SupplyCatalogItemType {
            return entries.firstOrNull { item -> item.code == code } ?: Cups
        }

        fun labelOf(code: String): String {
            return entries.firstOrNull { item -> item.code == code }?.label ?: code
        }

        fun orderOf(code: String): Int {
            return entries.firstOrNull { item -> item.code == code }?.order ?: 100
        }
    }
}
