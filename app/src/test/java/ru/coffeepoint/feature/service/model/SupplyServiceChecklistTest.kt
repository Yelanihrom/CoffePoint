package ru.coffeepoint.feature.service.model

import org.junit.Assert.*
import org.junit.Test

class SupplyServiceChecklistTest {
    @Test fun `mixers are always mandatory regardless of previous date`() {
        val items = buildSupplyServiceChecklist()
        assertEquals(listOf(SupplyServiceTask.MIXERS, SupplyServiceTask.BREW_UNIT_CLEANING,
            SupplyServiceTask.SYSTEM_TEST), items.map { it.task })
        assertFalse(items.canFinishSupplyService())
        val checked = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = true)) }
        assertTrue(checked.canFinishSupplyService())
    }

    @Test fun `instructions and warnings belong to the correct tasks`() {
        val items = buildSupplyServiceChecklist()
        assertEquals(listOf("Помыть миксеры", "Продуть и почистить УЗК", "Общий тест системы"),
            items.map { it.checklistItem.title })
        assertEquals("1. Щеткой убрать остатки кофе\n2. Продуть весь механизм", items[1].checklistItem.description)
        assertEquals("Зажать кнопку датчика закрытия двери в верхнем углу дверцы\n\n" +
            "Критерий проверки:\n— УЗК не должен скрипеть\n— вода должна поступать в каждый миксер",
            items[2].checklistItem.description)
        assertEquals("Критерий проверки:", items[2].hintEmphasis)
        assertTrue(items.take(2).all { it.hintEmphasis == null })
        assertNotNull(items.first().warningText)
        assertTrue(items.drop(1).all { it.warningText == null })
    }

    @Test fun `new visit starts unchecked independently of previous checklist`() {
        val first = buildSupplyServiceChecklist()
        first.forEach { it.checklistItem.checked = true }
        assertFalse(buildSupplyServiceChecklist().canFinishSupplyService())
    }

    @Test fun `all three tasks must be checked before finishing`() {
        val items = buildSupplyServiceChecklist()
        val allChecked = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = true)) }
        assertTrue(allChecked.canFinishSupplyService())
        assertFalse(emptyList<SupplyServiceChecklistItem>().canFinishSupplyService())
        for (index in allChecked.indices) {
            assertFalse(allChecked.filterIndexed { i, _ -> i != index }.canFinishSupplyService())
            assertFalse(allChecked.mapIndexed { i, item ->
                if (i == index) item.copy(checklistItem = item.checklistItem.copy(checked = false)) else item
            }.canFinishSupplyService())
        }
        assertFalse(listOf(allChecked[0], allChecked[0], allChecked[2]).canFinishSupplyService())
    }
}
