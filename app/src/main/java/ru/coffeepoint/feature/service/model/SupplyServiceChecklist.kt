package ru.coffeepoint.feature.service.model

import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.TechServiceNode
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

data class SupplyServiceChecklistItem(
    val node: TechServiceNode,
    val checklistItem: ServiceChecklistItem
)

fun buildSupplyServiceChecklist(
    station: CoffeeStation,
    today: LocalDate = LocalDate.now(ZoneOffset.UTC)
): List<SupplyServiceChecklistItem> = buildList {
    add(SupplyServiceChecklistItem(
        node = TechServiceNode.MIXERS,
        checklistItem = ServiceChecklistItem(
            title = "Обслужить миксеры",
            description = "1. Снять миксеры\n2. Промыть в горячей воде убрав наросты\n3. Остатки воды убрать тряпкой\n4. Установить в обратном порядке"
        )
    ))
    val containerDate = runCatching {
        LocalDate.parse(station.liquidWasteContainerServiceDate,
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT))
    }.getOrNull()
    if (containerDate != null && !containerDate.plusDays(60).isAfter(today)) {
        add(SupplyServiceChecklistItem(
            node = TechServiceNode.LIQUID_WASTE_CONTAINER,
            checklistItem = ServiceChecklistItem(
                title = "Обслужить контейнер для жидких отходов",
                description = "1. Открутить силиконовый шланг\n2. Слив закрыть пробкой.\n3. Промыть горячей водой убрав шлак\n4. Установить в обратном порядке"
            )
        ))
    }
}

fun List<SupplyServiceChecklistItem>.canFinishSupplyService(): Boolean =
    any { it.node == TechServiceNode.MIXERS } && all { it.checklistItem.checked }
