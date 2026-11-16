package ru.coffeepoint.feature.service.supply.model

import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.feature.service.model.maximum
import ru.coffeepoint.feature.service.model.quantity
import ru.coffeepoint.feature.station.model.StationInventory
import ru.coffeepoint.feature.station.model.StationSettings
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType

class SupplierRefillDraftTest {
    private val original = StationInventory(4, 1, 1, 1, 1, 1, 1, 1)
    private val settings = StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10)

    @Test fun `verification starts with system stock and prefills addition to maximum`() {
        val lines = createSupplierRefillLines(original, settings)
        assertEquals(9, lines.size)
        for (line in lines) {
            assertEquals(original.quantity(line.type), line.verified)
            assertEquals((settings.maximum(line.type) - line.verified).coerceAtLeast(0), line.added)
            assertEquals(line.verified + line.added, line.actual)
        }
    }

    @Test fun `four in system verified as three and eight added gives eleven and writes off eight`() {
        val lines = createSupplierRefillLines(original, settings).map { line ->
            if (line.type == WarehouseItemType.Cups) line.withVerified(3).withAdded(8) else line.withAdded(0)
        }
        assertEquals(11, lines.actualInventory(original).cups)
        assertEquals(mapOf("cups" to 8), lines.warehouseWriteOff(RefillSource.Warehouse))
    }

    @Test fun `verification updates proposed addition unless addition was manually edited`() {
        val cups = createSupplierRefillLines(original, settings).first()
        assertEquals(9, cups.withVerified(3).added)
        val edited = cups.withVerified(3).withAdded(8).withVerified(2)
        assertEquals(8, edited.added)
        assertEquals(10, edited.actual)
    }

    @Test fun `source changes only affect warehouse not station result`() {
        val lines = createSupplierRefillLines(original, settings)
        val before = lines.actualInventory(original)
        assertTrue(lines.warehouseWriteOff(RefillSource.ExternalPurchase).isEmpty())
        assertEquals(8, lines.warehouseWriteOff(RefillSource.Warehouse)["cups"])
        assertEquals(before, lines.actualInventory(original))
    }

    @Test fun `verification without addition changes station but not warehouse`() {
        val lines = createSupplierRefillLines(original, settings).map { it.withAdded(0) }.map {
            if (it.type == WarehouseItemType.Cups) it.withVerified(3) else it
        }
        assertTrue(lines.warehouseWriteOff(RefillSource.Warehouse).isEmpty())
        assertEquals(3, lines.actualInventory(original).cups)
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
        assertEquals(0, lines.first().withVerified(30).added)
    }

    @Test fun `quantities cannot become negative and final count does not overflow`() {
        val cups = createSupplierRefillLines(original, settings).first()
        assertThrows(IllegalArgumentException::class.java) { cups.withAdded(-1) }
        assertThrows(IllegalArgumentException::class.java) { cups.withVerified(-1) }
        assertEquals(Int.MAX_VALUE, cups.withAdded(Int.MAX_VALUE).actual)
        assertEquals(Int.MAX_VALUE, cups.withVerified(Int.MAX_VALUE).actual)
    }
}
