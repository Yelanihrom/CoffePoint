package ru.coffeepoint.core.data.repository

import androidx.room.withTransaction
import ru.coffeepoint.BuildConfig
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.StationServiceVisitEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SupplierServiceCompletionRepository(
    private val db: AppDatabase,
    private val recordVisits: Boolean = true,
    private val allowLocalSupplier: Boolean = BuildConfig.IS_TEST_ENVIRONMENT
) {
    suspend fun completeLocally(
        completionId: String,
        stationId: Int,
        role: UserRole,
        completedAt: Instant = Instant.now()
    ): StationServiceVisitEntity? = db.withTransaction {
        require(completionId.isNotBlank())
        require(role == UserRole.SUPPLY_OPERATOR || role == UserRole.MANAGER)
        val shouldRecord = recordVisits && role == UserRole.SUPPLY_OPERATOR
        if (shouldRecord) {
            db.stationServiceVisitDao().getById(completionId)?.let { existing ->
                require(existing.stationId == stationId) { "Visit belongs to another station" }
                return@withTransaction existing
            }
        }
        val station = checkNotNull(db.coffeeStationDao().getById(stationId)) { "Station not found" }
        val supplier = if (shouldRecord) {
            db.employeeDao().getActiveByRole("SUPPLY_OPERATOR")
                .singleOrNull()?.takeIf { allowLocalSupplier || !it.isLocalOnly }
                ?: throw LocalSupplierUnavailableException()
        } else null
        val serviceDate = completedAt.atZone(ZoneId.of("Europe/Moscow"))
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        db.coffeeStationDao().insert(station.copy(
            mixersServiceDate = serviceDate,
            isSynced = false,
            syncError = false,
            syncErrorType = "",
            pendingFullStationSync = station.pendingFullStationSync ||
                (!station.isSynced && !station.pendingServiceOnly && !station.pendingSupplyServiceOnly),
            pendingSupplyServiceOnly = true,
            pendingMixersServiceDate = true
        ))
        supplier?.let {
            val timestamp = completedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS).toString()
            val visit = StationServiceVisitEntity(
                id = completionId, employeeId = it.id, stationId = stationId,
                completedAt = timestamp, updatedAt = timestamp
            )
            db.stationServiceVisitDao().insertIfAbsent(visit)
            db.employeeTransactionDao().insertLocalEarning(EmployeeTransactionEntity(
                id = java.util.UUID.randomUUID().toString(),
                employeeId = it.id,
                type = "EARNING",
                operationDate = timestamp,
                periodFrom = null, periodTo = null, salesCount = null,
                amount = EARNINGS_PER_VISIT,
                comment = "",
                createdAt = timestamp, updatedAt = timestamp,
                isSynced = false, isLocalOnly = true,
                sourceType = "SUPPLY_SERVICE", sourceId = completionId
            ))
            visit
        }
    }

    companion object {
        const val EARNINGS_PER_VISIT = 2000
    }
}

class LocalSupplierUnavailableException : IllegalStateException("No unique active local supplier")
