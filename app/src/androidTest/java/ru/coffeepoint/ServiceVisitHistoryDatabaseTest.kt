package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.StationServiceVisitEntity
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.feature.station.model.*

class ServiceVisitHistoryDatabaseTest {
    @Test fun historyJoinsReferencesOrdersInstantsAndIncludesPendingVisits(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.stationServiceVisitDao()
            assertTrue(dao.observeHistory().first().isEmpty())
            db.employeeDao().insert(EmployeeEntity(2, "Денис", "SUPPLY_OPERATOR", "", true, "", ""))
            val station = StationDetail(CoffeeStation(7, "Станция", address = "Адрес",
                lastServiceDate = "", salesAfterService = 0), StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
                StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10,
                    chocolateMax = 10, toppingMax = 10)).toStationEntity()
            db.coffeeStationDao().insert(station)
            dao.insert(StationServiceVisitEntity("old", 2, 7, "2026-11-11T00:00:00+03:00", updatedAt = ""))
            dao.insert(StationServiceVisitEntity("new", 2, 7, "2026-11-10T21:30:00Z", updatedAt = "",
                isSynced = true))
            val rows = dao.observeHistory().first()
            assertEquals(listOf("new", "old"), rows.map { it.id })
            assertEquals("Денис", rows[0].employeeName)
            assertEquals("Адрес", rows[0].stationAddress)
            assertFalse(rows[1].isSynced)
            db.coffeeStationDao().insert(station.copy(name = "Новое имя"))
            assertEquals("Новое имя", dao.observeHistory().first()[0].stationName)
            db.coffeeStationDao().deleteById(7)
            db.employeeDao().deleteAll()
            val missing = dao.observeHistory().first()
            assertEquals(2, missing.size)
            assertNull(missing[0].stationName)
            assertNull(missing[0].employeeName)
        } finally { db.close() }
    }
}
