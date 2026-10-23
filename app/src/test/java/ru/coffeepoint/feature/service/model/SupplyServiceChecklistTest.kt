package ru.coffeepoint.feature.service.model

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.station.model.TechServiceNode

class SupplyServiceChecklistTest {
    @Test fun `mixers are always mandatory regardless of previous date`() {
        val items = buildSupplyServiceChecklist()
        assertEquals(listOf(TechServiceNode.MIXERS), items.map { it.node })
        assertFalse(items.canFinishSupplyService())
        val checked = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = true)) }
        assertTrue(checked.canFinishSupplyService())
    }

    @Test fun `container is never part of supplier checklist`() {
        assertFalse(buildSupplyServiceChecklist().any { it.node == TechServiceNode.LIQUID_WASTE_CONTAINER })
    }

    @Test fun `new visit starts unchecked independently of previous checklist`() {
        val first = buildSupplyServiceChecklist()
        first.single().checklistItem.checked = true
        assertFalse(buildSupplyServiceChecklist().canFinishSupplyService())
    }

    @Test fun `all included nodes must be checked before finishing`() {
        val items = buildSupplyServiceChecklist()
        val allChecked = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = true)) }
        assertTrue(allChecked.canFinishSupplyService())
        assertFalse(emptyList<SupplyServiceChecklistItem>().canFinishSupplyService())
        assertFalse(listOf(allChecked.single().copy(node = TechServiceNode.LIQUID_WASTE_CONTAINER)).canFinishSupplyService())
    }
}
