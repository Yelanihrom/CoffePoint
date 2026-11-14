package ru.coffeepoint

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.room.withTransaction
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.mapper.*
import ru.coffeepoint.feature.service.presentation.SupplyServiceStep2ViewModel
import ru.coffeepoint.feature.station.model.*
import kotlin.random.Random

class SupplierServiceCompletionViewModelTest {
    @Test fun offlineRetryAndRestoredScreenKeepOneVisitAndItsOriginalTime(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT && BuildConfig.TEST_SHEETS_WEB_APP_URL.isBlank())
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val db = DatabaseProvider.getDatabase(app)
        val stationId = Random.nextInt(1_000_000, Int.MAX_VALUE)
        assertNull(db.coffeeStationDao().getById(stationId))
        val detail = StationDetail(
            CoffeeStation(stationId, "Local visit test", lastServiceDate = "", salesAfterService = 0),
            StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
            StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10, chocolateMax = 10, toppingMax = 10)
        )
        val state = SavedStateHandle()
        try {
            db.withTransaction {
                db.coffeeStationDao().insert(detail.toStationEntity())
                db.stationInventoryDao().insert(detail.toInventoryEntity())
                db.stationSettingsDao().insert(detail.toSettingsEntity())
            }
            val model = withContext(Dispatchers.Main) {
                SupplyServiceStep2ViewModel(app, state).also { it.loadStation(stationId) }
            }
            withTimeout(10000) {
                while (!withContext(Dispatchers.Main) { model.checklist.size == 2 }) delay(20)
            }
            suspend fun finishAndAwaitError(vm: SupplyServiceStep2ViewModel) {
                withContext(Dispatchers.Main) {
                    vm.finishSupplyService(stationId, UserRole.SUPPLY_OPERATOR) { fail("Offline sync must not succeed") }
                }
                withTimeout(10000) {
                    while (!withContext(Dispatchers.Main) { vm.showSyncError && !vm.isSaving }) delay(20)
                }
                assertNull(withContext(Dispatchers.Main) { vm.localSaveError })
            }
            withContext(Dispatchers.Main) {
                model.checklist.indices.forEach { model.toggleChecklist(it, true) }
            }
            finishAndAwaitError(model)
            val visitId = withContext(Dispatchers.Main) { state.get<String>("completionId")!! }
            val original = db.stationServiceVisitDao().getById(visitId)!!
            finishAndAwaitError(model)
            val restored = withContext(Dispatchers.Main) {
                val restoredState = SavedStateHandle(state.keys().associateWith { state.get<Any?>(it) })
                SupplyServiceStep2ViewModel(app, restoredState).also { it.loadStation(stationId) }
            }
            finishAndAwaitError(restored)
            assertEquals(listOf(original), db.stationServiceVisitDao().getByStation(stationId))
            assertEquals(2000, db.employeeTransactionDao().getBySource("SUPPLY_SERVICE", visitId)!!.amount)
        } finally {
            db.withTransaction {
                val sqlite = db.openHelper.writableDatabase
                sqlite.execSQL("DELETE FROM employees_transactions WHERE sourceType = 'SUPPLY_SERVICE' AND sourceId IN (SELECT id FROM station_service_visits WHERE stationId = ?)", arrayOf(stationId))
                sqlite.execSQL("DELETE FROM station_service_visits WHERE stationId = ?", arrayOf(stationId))
                sqlite.execSQL("DELETE FROM station_inventory WHERE stationId = ?", arrayOf(stationId))
                sqlite.execSQL("DELETE FROM station_settings WHERE stationId = ?", arrayOf(stationId))
                db.coffeeStationDao().deleteById(stationId)
            }
        }
    }
}
