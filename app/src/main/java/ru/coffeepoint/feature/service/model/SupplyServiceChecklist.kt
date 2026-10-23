package ru.coffeepoint.feature.service.model

import ru.coffeepoint.feature.station.model.TechServiceNode

data class SupplyServiceChecklistItem(
    val node: TechServiceNode,
    val checklistItem: ServiceChecklistItem
)

fun buildSupplyServiceChecklist(): List<SupplyServiceChecklistItem> = buildList {
    add(SupplyServiceChecklistItem(
        node = TechServiceNode.MIXERS,
        checklistItem = ServiceChecklistItem(
            title = "Обслужить миксеры",
            description = "1. Снять миксеры\n2. Промыть в горячей воде убрав наросты\n3. Остатки воды убрать тряпкой\n4. Установить в обратном порядке"
        )
    ))
}

fun List<SupplyServiceChecklistItem>.canFinishSupplyService(): Boolean =
    size == 1 && single().node == TechServiceNode.MIXERS && single().checklistItem.checked
