package ru.coffeepoint.core.sync.google

import androidx.room.withTransaction
import ru.coffeepoint.BuildConfig
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.local.entity.StationServiceVisitEntity
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EmployeeDataImporter(
    private val db: AppDatabase,
    private val allowSupplierAdoption: Boolean = BuildConfig.IS_TEST_ENVIRONMENT
) {
    suspend fun import(bundle: GoogleSheetsFullSyncBundle, reconcile: Boolean = false): Boolean = db.withTransaction {
        val employees = db.employeeDao()
        val transactions = db.employeeTransactionDao()
        val visits = db.stationServiceVisitDao()
        check(bundle.employees.map { it.id }.distinct().size == bundle.employees.size)
        check(bundle.employeeTransactions.map { it.id }.distinct().size == bundle.employeeTransactions.size)
        check(bundle.stationServiceVisits.map { it.id }.distinct().size == bundle.stationServiceVisits.size)
        bundle.employees.forEach { remote ->
            val local = employees.getById(remote.id)
            if (local?.isLocalOnly == true) {
                check(allowSupplierAdoption && local.name == remote.name &&
                    local.role == "SUPPLY_OPERATOR" && remote.role == local.role) { "Supplier identity conflict" }
                employees.insert(remote.copy(isLocalOnly = false))
                transactions.enableSupplierPayments(remote.id)
            } else if (local == null || local.isSynced) {
                employees.insert(remote)
            }
        }
        val canReconcile = reconcile && transactions.getUnsynced().isEmpty() &&
            (bundle.employeeTransactions.isNotEmpty() || transactions.getAll().none {
                !it.isLocalOnly && it.sourceType != "SUPPLY_SERVICE"
            })
        if (canReconcile) transactions.deleteRemoteRecords()
        val localAssignments = db.employeeStationAssignmentDao().getAll().associateBy { it.id }
        bundle.employeeStationAssignments.forEach { remote ->
            // Assignments are edited independently; do not replace queued local changes.
            val local = localAssignments[remote.id]
            if (local == null || local.isSynced) db.employeeStationAssignmentDao().insertAll(listOf(remote))
        }
        bundle.stationServiceVisits.forEach { remote ->
            val local = visits.getById(remote.id)
            check(local == null || sameVisit(local, remote)) { "Visit identity conflict" }
            // full_sync reads sheets sequentially, so it may straddle a server batch commit.
            val hasEarning = bundle.employeeTransactions.any {
                it.sourceType == "SUPPLY_SERVICE" && it.sourceId == remote.id
            } || transactions.getBySource("SUPPLY_SERVICE", remote.id)?.let { it.isSynced && !it.isLocalOnly } == true
            visits.insert(remote.copy(isSynced = hasEarning, syncError = false, syncErrorType = ""))
        }
        bundle.employeeTransactions.forEach { remote ->
            val local = transactions.getById(remote.id)
            if (remote.sourceType == "SUPPLY_SERVICE") {
                val visit = visits.getById(checkNotNull(remote.sourceId))
                check(visit != null && visit.isSynced && remote.id.isNotBlank() &&
                    remote.employeeId == visit.employeeId && remote.type == "EARNING" && remote.amount > 0 &&
                    instant(remote.operationDate) == instant(visit.completedAt)) { "Invalid visit earning" }
                check(local == null || (local.sourceType == remote.sourceType &&
                    local.sourceId == remote.sourceId && local.employeeId == remote.employeeId &&
                    local.type == "EARNING" && instant(local.operationDate) == instant(remote.operationDate))) {
                    "Earning identity conflict"
                }
                val linked = transactions.getBySource("SUPPLY_SERVICE", checkNotNull(remote.sourceId))
                check(linked == null || (linked.employeeId == remote.employeeId && linked.type == "EARNING" &&
                    instant(linked.operationDate) == instant(remote.operationDate))) { "Earning source conflict" }
                if (linked != null && linked.id != remote.id) transactions.deleteById(linked.id)
            } else if (local != null && !local.isSynced) {
                // A downloaded stale snapshot must not erase a pending edit or provisional earning.
                if (!samePayment(local, remote)) return@forEach
            }
            check(local?.isLocalOnly != true || remote.sourceType == "SUPPLY_SERVICE") {
                "Remote transaction conflicts with local-only data"
            }
            check(employees.getById(remote.employeeId)?.isLocalOnly != true) { "Employee remains local-only" }
            transactions.insert(remote.copy(isLocalOnly = false, isSynced = true, syncError = false, syncErrorType = ""))
        }
        canReconcile
    }

    private fun sameVisit(a: StationServiceVisitEntity, b: StationServiceVisitEntity) =
        a.employeeId == b.employeeId && a.stationId == b.stationId && instant(a.completedAt) == instant(b.completedAt)

    private fun samePayment(a: EmployeeTransactionEntity, b: EmployeeTransactionEntity) =
        a.type == "PAYMENT" && b.type == a.type && a.employeeId == b.employeeId &&
            a.amount == b.amount && instant(a.operationDate) == instant(b.operationDate)

    private fun instant(value: String): Instant = runCatching { Instant.parse(value) }.getOrElse {
        LocalDateTime.parse(value).atZone(ZoneId.of("Europe/Moscow")).toInstant()
    }.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
}
