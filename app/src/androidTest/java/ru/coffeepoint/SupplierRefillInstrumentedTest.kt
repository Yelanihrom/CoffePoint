package ru.coffeepoint

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.work.WorkManager
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.WarehouseStockEntity
import ru.coffeepoint.core.data.mapper.*
import ru.coffeepoint.core.data.repository.SupplierRefillRepository
import ru.coffeepoint.core.data.repository.SupplierRefillSaveResult
import ru.coffeepoint.feature.service.ui.ServiceStep1Screen
import ru.coffeepoint.feature.station.model.*
import ru.coffeepoint.feature.warehouse.model.WarehouseItemType
import ru.coffeepoint.ui.theme.CoffeePointTheme
import java.io.File

class SupplierRefillInstrumentedTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun detail() = StationDetail(
        CoffeeStation(id = 908070, name = "Тест пополнения", lastServiceDate = "", salesAfterService = 0),
        StationInventory(4, 0, 0, 0, 0, 0, 0, 0, garbageBags = 0),
        StationSettings(cupsMax = 12, lidsMax = 0, coffeeMax = 0, milkMax = 0, chocolateMax = 0, toppingMax = 0,
            sugarMax = 0, stirrersMax = 0, garbageBagsMax = 0)
    )
    private suspend fun seed(db: AppDatabase, cups: Int) {
        val detail = detail()
        db.coffeeStationDao().insert(detail.toStationEntity())
        db.stationInventoryDao().insert(detail.toInventoryEntity())
        db.stationSettingsDao().insert(detail.toSettingsEntity())
        WarehouseItemType.entries.forEach { type ->
            db.warehouseStockDao().insert(WarehouseStockEntity(type.code, if (type == WarehouseItemType.Cups) cups else 100))
        }
    }

    @Test fun shortageDoesNotWriteStationAndWarehouseTogether(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seed(db, 7)
            val result = SupplierRefillRepository(db).saveLocally(detail(), detail().inventory.copy(cups = 11), mapOf("cups" to 8))
            assertTrue(result is SupplierRefillSaveResult.Shortage)
            assertEquals(4, db.stationInventoryDao().getByStationId(908070)!!.cups)
            assertEquals(7, db.warehouseStockDao().getAll().single { it.itemType == "cups" }.quantity)
            assertTrue(db.stationInventoryDao().getByStationId(908070)!!.isSynced)
        } finally { db.close() }
    }

    @Test fun actualElevenAndAddedEightAreSavedIndependently(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seed(db, 20)
            val result = SupplierRefillRepository(db).saveLocally(detail(), detail().inventory.copy(cups = 11), mapOf("cups" to 8))
            assertEquals(SupplierRefillSaveResult.Saved, result)
            val inventory = db.stationInventoryDao().getByStationId(908070)!!
            val stock = db.warehouseStockDao().getAll().single { it.itemType == "cups" }
            assertEquals(11, inventory.cups)
            assertEquals(12, stock.quantity)
            assertFalse(inventory.isSynced)
            assertFalse(stock.isSynced)
            assertEquals(inventory.updatedAt, stock.updatedAt)
            assertTrue(inventory.updatedAt.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")))
            assertTrue(db.coffeeStationDao().getById(908070)!!.isSynced)
        } finally { db.close() }
    }

    @Test fun externalPurchaseDoesNotChangeWarehouse(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seed(db, 0)
            SupplierRefillRepository(db).saveLocally(detail(), detail().inventory.copy(cups = 11), emptyMap())
            val stock = db.warehouseStockDao().getAll().single { it.itemType == "cups" }
            assertEquals(0, stock.quantity)
            assertTrue(stock.isSynced)
            assertEquals(11, db.stationInventoryDao().getByStationId(908070)!!.cups)
        } finally { db.close() }
    }

    @Test fun supplierCanReviewAndCorrectWithoutChangingAddedQuantity(): Unit = runBlocking {
        assumeTrue(BuildConfig.IS_TEST_ENVIRONMENT && context.packageName.endsWith(".ci"))
        assumeTrue(BuildConfig.TEST_SHEETS_WEB_APP_URL.isBlank())
        val db = DatabaseProvider.getDatabase(context)
        check(db.coffeeStationDao().getById(908070) == null) { "Test station already exists" }
        val oldStock = db.warehouseStockDao().getAll()
        try {
            seed(db, 20)
            compose.setContent {
                CoffeePointTheme {
                    ServiceStep1Screen(908070, UserRole.SUPPLY_OPERATOR, onBack = {}, onNext = {},
                        isSupplyServiceFlow = true, syncImmediatelyOnSave = true)
                }
            }
            compose.waitUntil(10000) { compose.onAllNodesWithText("Сколько добавили?").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Склад").assertIsDisplayed()
            compose.onNodeWithText("8 / 20").assertIsDisplayed()
            screenshot("supplier-refill-added.png")
            compose.onNodeWithText("ПРОВЕРИТЬ ОСТАТКИ").performClick()
            compose.waitUntil(10000) { compose.onAllNodesWithText("Остаток после пополнения").fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(hasText("-") and hasAnyAncestor(hasTestTag("stock-cups"))).performClick()
            compose.onNodeWithText("11").assertIsDisplayed()
            compose.onNodeWithText("Добавлено: 8 · Ожидалось: 12 · Корректировка: -1").assertIsDisplayed()
            screenshot("supplier-refill-review.png")
            compose.onNodeWithContentDescription("Назад").performClick()
            compose.onNodeWithText("8 / 20").assertIsDisplayed()
            compose.onNodeWithText("ПРОВЕРИТЬ ОСТАТКИ").performClick()
            compose.onNodeWithText("11").assertIsDisplayed()
            compose.onNodeWithText("ДАЛЕЕ").performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Изменения сохранены на устройстве").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(11, db.stationInventoryDao().getByStationId(908070)!!.cups)
            assertEquals(12, db.warehouseStockDao().getAll().single { it.itemType == "cups" }.quantity)
            compose.onNodeWithText("ПОВТОРИТЬ СЕЙЧАС").performClick()
            compose.waitUntil(15000) {
                compose.onAllNodesWithText("Изменения сохранены на устройстве").fetchSemanticsNodes().isNotEmpty()
            }
            assertEquals(12, db.warehouseStockDao().getAll().single { it.itemType == "cups" }.quantity)
        } finally {
            WorkManager.getInstance(context).cancelAllWork().result.get()
            db.stationInventoryDao().deleteByStationId(908070)
            db.stationSettingsDao().deleteByStationId(908070)
            db.coffeeStationDao().deleteById(908070)
            // This test runs only in the isolated CI package and never launches background sync.
            db.openHelper.writableDatabase.execSQL("DELETE FROM warehouse_stock")
            db.warehouseStockDao().insertAll(oldStock)
        }
    }

    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
