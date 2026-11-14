package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.*
import ru.coffeepoint.core.data.preferences.MonthlyEarningsPreferences
import ru.coffeepoint.core.data.repository.EmployeeSyncRepository
import ru.coffeepoint.core.data.repository.MonthlyEarningsRepository
import ru.coffeepoint.core.data.repository.SupplierServiceCompletionRepository
import ru.coffeepoint.core.data.mapper.toStationEntity
import ru.coffeepoint.feature.station.model.*
import ru.coffeepoint.core.sync.google.*
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi
import ru.coffeepoint.core.sync.google.dto.*
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.util.UUID

class SupplierFinanceSyncTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val visit = StationServiceVisitEntity("visit-1", 2, 7, "2026-11-10T21:30:00Z",
        updatedAt = "2026-11-10T21:30:00Z")
    private fun supplier(local: Boolean) = EmployeeEntity(2, "Денис", "SUPPLY_OPERATOR", "", true,
        "2026-11-10T00:00:00Z", "2026-11-10T00:00:00Z", isSynced = !local, isLocalOnly = local)
    private suspend fun seed(db: AppDatabase) {
        db.employeeDao().insert(supplier(true))
        db.stationServiceVisitDao().insertIfAbsent(visit)
        db.employeeTransactionDao().insert(EmployeeTransactionEntity(UUID.randomUUID().toString(), 2,
            "EARNING", visit.completedAt, null, null, null, 2000, "", visit.completedAt, visit.completedAt,
            isSynced = false, sourceType = "SUPPLY_SERVICE", sourceId = visit.id, isLocalOnly = true))
    }
    private class Server : GoogleSheetsSyncApi {
        var offline = false
        var loseResponse = false
        var posts = 0
        var onCommit: (suspend () -> Unit)? = null
        val visits = linkedMapOf<String, Map<String, Any?>>()
        val operations = linkedMapOf<String, Map<String, Any?>>()
        override suspend fun salesAnalytics(url: String, action: String, token: String, startDate: String,
            endDate: String?): SalesAnalyticsResponseDto = error("Not used")
        override suspend fun fullSync(url: String, action: String, token: String, entities: String?, stationId: Int?): GoogleSheetsFullSyncResponseDto {
            if (offline) throw UnknownHostException()
            return GoogleSheetsFullSyncResponseDto(true, GoogleSheetsFullSyncDataDto(emptyList(), emptyList(),
                employees = listOf(mapOf("id" to 2, "name" to "Денис", "role" to "SUPPLY_OPERATOR",
                    "isActive" to true, "createdAt" to "2026-11-10T00:00:00Z", "updatedAt" to "2026-11-10T00:00:00Z")),
                employeeTransactions = operations.values.toList(), stationServiceVisits = visits.values.toList()), null)
        }
        override suspend fun upsert(url: String, request: GoogleSheetsSyncRequestDto): GoogleSheetsSyncResponseDto {
            if (offline) throw UnknownHostException()
            posts++
            if (request.action == "complete_supply_visit") {
                val id = request.data["id"].toString()
                val visit = visits.getOrPut(id) { request.data + mapOf("createdAt" to "2026-11-11T00:00:00Z", "updatedAt" to "2026-11-11T00:00:00Z") }
                val existing = operations.values.singleOrNull { it["sourceType"] == "SUPPLY_SERVICE" && it["sourceId"] == id }
                val earningId = existing?.get("id")?.toString() ?: request.data["earningId"]?.toString() ?: UUID.randomUUID().toString()
                val earning = operations.getOrPut(earningId) { mapOf("id" to earningId,
                    "employeeId" to 2, "type" to "EARNING", "amount" to 2000,
                    "operationDate" to visit["completedAt"], "sourceType" to "SUPPLY_SERVICE", "sourceId" to id,
                    "createdAt" to "2026-11-11T00:00:00Z", "updatedAt" to "2026-11-11T00:00:00Z") }
                onCommit?.invoke()
                if (loseResponse) { loseResponse = false; throw SocketTimeoutException() }
                return GoogleSheetsSyncResponseDto(true, null, GoogleSheetsSyncDataDto(visit, earning))
            }
            check(request.entity == "employees_transactions" && request.data["type"] == "PAYMENT")
            operations[request.data["id"].toString()] = request.data
            if (loseResponse) { loseResponse = false; throw SocketTimeoutException() }
            return GoogleSheetsSyncResponseDto(true, null)
        }
        fun manager() = GoogleSheetsSyncManager(GoogleSheetsRemoteDataSource(this,
            "https://test.invalid/exec", "test-token", reportFailure = { _, _ -> }))
    }

    @Test fun freshDatabaseImportsRealSupplierAndCompletesWithProductionRules(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val server = Server()
        try {
            assertTrue(db.employeeDao().getAll().isEmpty())
            val employees = EmployeeSyncRepository(context, db, server.manager(), enqueueSync = {})
            assertTrue(employees.refreshEmployeeDataFromGoogleSheets())
            val supplier = db.employeeDao().getActiveByRole("SUPPLY_OPERATOR").single()
            assertFalse(supplier.isLocalOnly)
            val detail = StationDetail(CoffeeStation(7, "Production rules test", lastServiceDate = "", salesAfterService = 0),
                StationInventory(1, 1, 1, 1, 1, 1, 1, 1),
                StationSettings(cupsMax = 12, lidsMax = 6, coffeeMax = 6, milkMax = 10,
                    chocolateMax = 10, toppingMax = 10))
            db.coffeeStationDao().insert(detail.toStationEntity())
            val completion = SupplierServiceCompletionRepository(db, allowLocalSupplier = false)
            completion.completeLocally(visit.id, 7, UserRole.SUPPLY_OPERATOR,
                java.time.Instant.parse(visit.completedAt))
            assertTrue(SupplierVisitSyncService(db, server.manager()).syncPending().synced)
            assertTrue(SupplierVisitSyncService(db, server.manager()).syncPending().synced)
            assertEquals(1, server.posts)
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(supplier.id, "EARNING"))
            assertTrue(db.stationServiceVisitDao().getById(visit.id)!!.isSynced)
        } finally { db.close() }
    }

    @Test fun offlineLostResponseRestartAndSecondDeviceDoNotDuplicateMoney(): Unit = runBlocking {
        val name = "finance-${UUID.randomUUID()}.db"
        val server = Server()
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val second = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val scope = "finance-${UUID.randomUUID()}"
        try {
            seed(db)
            val originalId = db.employeeTransactionDao().getBySource("SUPPLY_SERVICE", visit.id)!!.id
            server.offline = true
            assertEquals(GoogleSheetsSyncErrorType.CONNECTION, SupplierVisitSyncService(db, server.manager()).syncPending().errorType)
            assertEquals(1, db.stationServiceVisitDao().getUnsynced().size)
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
            server.offline = false
            server.loseResponse = true
            assertEquals(GoogleSheetsSyncErrorType.CONNECTION, SupplierVisitSyncService(db, server.manager()).syncPending().errorType)
            assertFalse(db.employeeDao().getById(2)!!.isLocalOnly)
            assertEquals(1, server.operations.size)
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            assertTrue(SupplierVisitSyncService(db, server.manager()).syncPending().synced)
            assertTrue(db.stationServiceVisitDao().getUnsynced().isEmpty())
            assertEquals(1, server.operations.size)
            assertEquals(2, server.posts)
            assertFalse(db.employeeTransactionDao().getAll().single().isLocalOnly)
            assertTrue(db.employeeTransactionDao().getAll().single().isSynced)
            assertEquals(originalId, db.employeeTransactionDao().getAll().single().id)

            EmployeeDataImporter(second).import(server.manager().loadFullSyncBundle())
            val earnings = MonthlyEarningsRepository(context, UserRole.SUPPLY_OPERATOR, second,
                MonthlyEarningsPreferences(context, scope), enqueueSync = {})
            earnings.refreshLocalState()
            assertEquals(2000, earnings.getState().settlementAmount)
            assertEquals(1, earnings.getOperationsForPeriod("11.11.2026", "11.11.2026").size)
            earnings.addPayment(500)
            server.offline = true
            assertFalse(EmployeeSyncRepository(context, second, server.manager(), enqueueSync = {}).syncEmployeeData().synced)
            assertEquals(1, second.employeeTransactionDao().getUnsynced().size)
            server.offline = false
            server.loseResponse = true
            assertFalse(EmployeeSyncRepository(context, second, server.manager(), enqueueSync = {}).syncEmployeeData().synced)
            assertTrue(EmployeeSyncRepository(context, second, server.manager(), enqueueSync = {}).syncEmployeeData().synced)
            assertEquals(2, server.operations.size)
            EmployeeDataImporter(db).import(server.manager().loadFullSyncBundle())
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
            assertEquals(500, db.employeeTransactionDao().getTotalAmount(2, "PAYMENT"))
        } finally {
            db.close(); second.close(); context.deleteDatabase(name)
            context.getSharedPreferences("monthly_earnings_$scope", 0).edit().clear().commit()
        }
    }

    @Test fun conflictingEmployeeCannotErasePendingVisitOrMoney(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seed(db)
            var rejected = false
            try { EmployeeDataImporter(db).import(GoogleSheetsFullSyncBundle(employees = listOf(supplier(false).copy(name = "Other")))) }
            catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
            assertTrue(db.employeeDao().getById(2)!!.isLocalOnly)
            assertEquals(visit, db.stationServiceVisitDao().getById(visit.id))
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
        } finally { db.close() }
    }

    @Test fun incompleteSnapshotKeepsVisitPendingAndImportNeverDeletesQueuedPayment(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            seed(db)
            val payment = EmployeeTransactionEntity("payment-1", 2, "PAYMENT", "2026-11-11T00:00:00Z",
                null, null, null, 500, "", "2026-11-11T00:00:00Z", "2026-11-11T00:00:00Z",
                isSynced = false, isLocalOnly = true)
            db.employeeTransactionDao().insert(payment)
            val snapshot = GoogleSheetsFullSyncBundle(employees = listOf(supplier(false)),
                stationServiceVisits = listOf(visit.copy(createdAt = "2026-11-11T00:00:00Z", isSynced = true)))
            EmployeeDataImporter(db).import(snapshot, reconcile = true)
            assertFalse(db.stationServiceVisitDao().getById(visit.id)!!.isSynced)
            assertTrue(db.employeeTransactionDao().getBySource("SUPPLY_SERVICE", visit.id)!!.isLocalOnly)
            assertEquals(listOf(payment.copy(isLocalOnly = false)), db.employeeTransactionDao().getUnsynced())
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
        } finally { db.close() }
    }

    @Test fun lateTimeoutCannotUndoReceiptImportedByAnotherRequest(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val server = Server()
        try {
            seed(db)
            server.loseResponse = true
            server.onCommit = { EmployeeDataImporter(db).import(server.manager().loadFullSyncBundle()) }
            assertEquals(GoogleSheetsSyncErrorType.CONNECTION,
                SupplierVisitSyncService(db, server.manager()).syncPending().errorType)
            assertTrue(db.stationServiceVisitDao().getById(visit.id)!!.isSynced)
            assertTrue(db.employeeTransactionDao().getAll().single().isSynced)
            assertFalse(db.employeeTransactionDao().getAll().single().syncError)
            assertTrue(SupplierVisitSyncService(db, server.manager()).syncPending().synced)
            assertEquals(1, server.posts)
        } finally { db.close() }
    }

    @Test fun existingServerUuidReplacesLocalUuidBySourceWithoutDoubleCounting(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val server = Server()
        try {
            seed(db)
            val localId = db.employeeTransactionDao().getBySource("SUPPLY_SERVICE", visit.id)!!.id
            val remoteId = UUID.randomUUID().toString()
            server.upsert("", GoogleSheetsSyncRequestDto("complete_supply_visit", "test", "station_service_visits",
                mapOf("id" to visit.id, "employeeId" to 2, "stationId" to 7,
                    "completedAt" to visit.completedAt, "earningId" to remoteId)))
            assertTrue(SupplierVisitSyncService(db, server.manager()).syncPending().synced)
            assertNull(db.employeeTransactionDao().getById(localId))
            assertEquals(remoteId, db.employeeTransactionDao().getAll().single().id)
            assertEquals(2000, db.employeeTransactionDao().getTotalAmount(2, "EARNING"))
            assertEquals(1, server.posts)
        } finally { db.close() }
    }

    @Test fun legacyLocalComposedIdBecomesUuidOnConfirmation(): Unit = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val server = Server()
        try {
            seed(db)
            val original = db.employeeTransactionDao().getBySource("SUPPLY_SERVICE", visit.id)!!
            val legacyId = "SUPPLY-EARNING-${visit.id}"
            db.employeeTransactionDao().deleteById(original.id)
            db.employeeTransactionDao().insert(original.copy(id = legacyId))
            assertTrue(SupplierVisitSyncService(db, server.manager()).syncPending().synced)
            val saved = db.employeeTransactionDao().getAll().single()
            assertEquals(saved.id, UUID.fromString(saved.id).toString())
            assertNull(db.employeeTransactionDao().getById(legacyId))
            assertEquals(2000, saved.amount)
        } finally { db.close() }
    }
}
