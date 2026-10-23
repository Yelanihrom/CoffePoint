package ru.coffeepoint.feature.service.model

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.station.model.CoffeeStation
import ru.coffeepoint.feature.station.model.TechServiceNode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SupplyServiceChecklistTest {
    private val today = LocalDate.of(2026, 10, 23)
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    private fun station(mixers: String = "23.10.2026", container: String = "23.10.2026") =
        CoffeeStation(id = 7, name = "Station", lastServiceDate = "", salesAfterService = 0,
            mixersServiceDate = mixers, liquidWasteContainerServiceDate = container)

    @Test fun `mixers are always mandatory regardless of previous date`() {
        for (date in listOf("23.10.2026", "01.01.2026", "01.01.2027", "", "invalid")) {
            val items = buildSupplyServiceChecklist(station(mixers = date), today)
            assertEquals(listOf(TechServiceNode.MIXERS), items.map { it.node })
            assertFalse(items.canFinishSupplyService())
            val checked = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = true)) }
            assertTrue(checked.canFinishSupplyService())
        }
    }

    @Test fun `container is included only at 60 days or later`() {
        for (days in listOf(59L, 60L, 61L)) {
            val items = buildSupplyServiceChecklist(station(container = today.minusDays(days).format(formatter)), today)
            assertEquals(days >= 60, items.any { it.node == TechServiceNode.LIQUID_WASTE_CONTAINER })
            assertEquals(TechServiceNode.MIXERS, items.first().node)
        }
    }

    @Test fun `empty invalid or future container date does not add container`() {
        for (date in listOf("", "invalid", "31.02.2026", "01.01.2027")) {
            assertEquals(listOf(TechServiceNode.MIXERS), buildSupplyServiceChecklist(station(container = date), today).map { it.node })
        }
    }

    @Test fun `all included nodes must be checked before finishing`() {
        val items = buildSupplyServiceChecklist(station(container = "01.01.2026"), today)
        val onlyMixers = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = it.node == TechServiceNode.MIXERS)) }
        assertFalse(onlyMixers.canFinishSupplyService())
        val allChecked = items.map { it.copy(checklistItem = it.checklistItem.copy(checked = true)) }
        assertTrue(allChecked.canFinishSupplyService())
        assertFalse(emptyList<SupplyServiceChecklistItem>().canFinishSupplyService())
        assertFalse(allChecked.filter { it.node != TechServiceNode.MIXERS }.canFinishSupplyService())
    }
}
