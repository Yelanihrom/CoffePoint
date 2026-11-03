package ru.coffeepoint.feature.service.model

enum class SupplyServiceTask { MIXERS, BREW_UNIT_CLEANING, SYSTEM_TEST }

data class SupplyServiceChecklistItem(
    val task: SupplyServiceTask,
    val checklistItem: ServiceChecklistItem,
    val warningText: String? = null,
    val hintEmphasis: String? = null
)

fun buildSupplyServiceChecklist(): List<SupplyServiceChecklistItem> = listOf(
    SupplyServiceChecklistItem(
        task = SupplyServiceTask.MIXERS,
        checklistItem = ServiceChecklistItem(
            title = "Помыть миксеры",
            description = "1. Снять миксеры\n2. Промыть в горячей воде убрав наросты\n3. Остатки воды убрать тряпкой\n4. Установить в обратном порядке"
        ),
        warningText = "Не забыть одеть силиконовые шланги"
    ),
    SupplyServiceChecklistItem(
        task = SupplyServiceTask.BREW_UNIT_CLEANING,
        checklistItem = ServiceChecklistItem(
            title = "Продуть и почистить УЗК",
            description = "1. Щеткой убрать остатки кофе\n2. Продуть весь механизм"
        )
    ),
    SupplyServiceChecklistItem(
        task = SupplyServiceTask.SYSTEM_TEST,
        checklistItem = ServiceChecklistItem(
            title = "Общий тест системы",
            description = "Зажать кнопку датчика закрытия двери в верхнем углу дверцы\n\n" +
                "Критерий проверки:\n— УЗК не должен скрипеть\n— вода должна поступать в каждый миксер"
        ),
        hintEmphasis = "Критерий проверки:"
    )
)

fun List<SupplyServiceChecklistItem>.canFinishSupplyService(): Boolean =
    size == SupplyServiceTask.entries.size &&
        map { it.task }.toSet() == SupplyServiceTask.entries.toSet() &&
        all { it.checklistItem.checked }
