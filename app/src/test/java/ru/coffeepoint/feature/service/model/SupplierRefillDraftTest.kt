package ru.coffeepoint.feature.service.model

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

class SupplierRefillDraftTest {
    private val original = StationInventory(4, 1, 1, 1, 1, 1, 1, 1)
    private val settings = StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10)

    @Test fun `four in system plus eight added corrected to eleven writes off eight`() {
        val lines = createSupplierRefillLines(original, settings).map { line ->
            if (line.type == WarehouseItemType.Cups) line.withActual(11) else line.withAdded(0)
        }
        assertEquals(11, lines.actualInventory(original).cups)
        assertEquals(mapOf("cups" to 8), lines.warehouseWriteOff(RefillSource.Warehouse))
        assertEquals(12, lines.first().expected)
    }

    @Test fun `all nine amounts are prefilled to local maximum`() {
        val lines = createSupplierRefillLines(original, settings)
        assertEquals(9, lines.size)
        for (line in lines) {
            assertEquals((settings.maximum(line.type) - original.quantity(line.type)).coerceAtLeast(0), line.added)
            assertEquals(original.quantity(line.type) + line.added, line.actual)
        }
    }

    @Test fun `source changes do not change actual or added quantities`() {
        val lines = createSupplierRefillLines(original, settings)
        val before = lines.actualInventory(original)
        assertTrue(lines.warehouseWriteOff(RefillSource.ExternalPurchase).isEmpty())
        assertEquals(8, lines.warehouseWriteOff(RefillSource.Warehouse)["cups"])
        assertEquals(before, lines.actualInventory(original))
    }

    @Test fun `zero addition and factual correction do not affect warehouse`() {
        val lines = createSupplierRefillLines(original, settings).map { it.withAdded(0) }.map {
            if (it.type == WarehouseItemType.Cups) it.withActual(3) else it
        }
        assertTrue(lines.warehouseWriteOff(RefillSource.Warehouse).isEmpty())
        assertEquals(3, lines.actualInventory(original).cups)
    }

    @Test fun `actual correction never changes write off even if below original`() {
        val cups = createSupplierRefillLines(original, settings).first().withActual(2)
        assertEquals(8, cups.added)
        assertEquals(2, listOf(cups).actualInventory(original).cups)
        assertEquals(mapOf("cups" to 8), listOf(cups).warehouseWriteOff(RefillSource.Warehouse))
    }

    @Test fun `untouched actual follows added but manually corrected actual stays unchanged`() {
        val cups = createSupplierRefillLines(original, settings).first()
        assertEquals(10, cups.withAdded(6).actual)
        val edited = cups.withActual(11).withAdded(6)
        assertEquals(11, edited.actual)
        assertEquals(6, edited.added)
    }

    @Test fun `warehouse shortage handles exact stock zero and missing stock`() {
        assertTrue(warehouseShortages(mapOf("cups" to 8), mapOf("cups" to 8)).isEmpty())
        assertEquals(setOf("cups"), warehouseShortages(mapOf("cups" to 8), mapOf("cups" to 7)))
        assertEquals(setOf("cups"), warehouseShortages(mapOf("cups" to 8), emptyMap()))
        assertTrue(warehouseShortages(emptyMap(), emptyMap()).isEmpty())
    }

    @Test fun `unset max and stock above max never propose negative refill`() {
        val lines = createSupplierRefillLines(original.copy(cups = 20), settings.copy(cupsMax = 12, lidsMax = 0))
        assertEquals(0, lines.first().added)
        assertEquals(20, lines.first().actual)
        assertEquals(0, lines[1].added)
        assertEquals(1, lines[1].actual)
    }

    @Test fun `quantities cannot become negative and expected count does not overflow`() {
        val cups = createSupplierRefillLines(original, settings).first()
        assertThrows(IllegalArgumentException::class.java) { cups.withAdded(-1) }
        assertThrows(IllegalArgumentException::class.java) { cups.withActual(-1) }
        assertEquals(Int.MAX_VALUE, cups.withAdded(Int.MAX_VALUE).expected)
    }
}
