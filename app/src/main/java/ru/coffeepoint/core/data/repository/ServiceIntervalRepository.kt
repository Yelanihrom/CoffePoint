package ru.coffeepoint.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager
import ru.coffeepoint.core.sync.google.GoogleSheetsEntitySyncResult
import ru.coffeepoint.core.sync.google.GoogleSheetsSyncErrorType
import ru.coffeepoint.core.sync.google.MAX_MAINTENANCE_SALES_LIMIT
import ru.coffeepoint.core.sync.google.worker.GoogleSheetsSyncScheduler
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.MaintenanceSettingsEntity
import ru.coffeepoint.feature.station.model.ServiceIntervals
import ru.coffeepoint.feature.station.model.TechServiceNode

class ServiceIntervalRepository(
    private val db: AppDatabase,
    private val legacyPreferences: SharedPreferences? = null,
    private val load: (suspend () -> List<MaintenanceSettingsEntity>)? = null,
    private val send: (suspend (List<MaintenanceSettingsEntity>) -> GoogleSheetsEntitySyncResult)? = null,
    private val enqueue: () -> Unit = {}
) {
    constructor(context: Context, manager: GoogleSheetsSyncManager = GoogleSheetsSyncManager()) : this(
        DatabaseProvider.getDatabase(context),
        context.applicationContext.getSharedPreferences("service_intervals", Context.MODE_PRIVATE),
        manager::loadMaintenanceSettings, manager::syncMaintenanceSettings,
        { GoogleSheetsSyncScheduler.enqueueSync(context.applicationContext) })

    private val dao = db.maintenanceSettingsDao()

    private suspend fun initialize() = db.withTransaction {
        val existing = dao.getAll().map { it.nodeType }.toSet()
        // Only missing rows import the previous device settings; Room becomes authoritative.
        val missing = TechServiceNode.entries.filter { it.name !in existing }.map { node ->
            val legacy = (legacyPreferences?.all?.get(node.name) as? Int)?.takeIf { it > 0 }
            MaintenanceSettingsEntity(node.name, legacy ?: node.defaultIntervalDays, 0,
                updatedAt = if (legacy != null) Instant.now().toString() else "",
                isSynced = legacy == null)
        }
        if (missing.isNotEmpty()) dao.upsertAll(missing)
    }

    suspend fun read(): ServiceIntervals {
        initialize()
        return model(dao.getAll())
    }

    fun observe(): Flow<ServiceIntervals> = flow {
        initialize()
        emitAll(dao.observeAll().map(::model))
    }.flowOn(Dispatchers.IO)

    suspend fun save(intervals: ServiceIntervals) {
        require(intervals.salesLimits.values.all { it in 0..MAX_MAINTENANCE_SALES_LIMIT })
        db.withTransaction {
            val current = dao.getAll().associateBy { it.nodeType }
            dao.upsertAll(TechServiceNode.entries.mapNotNull {
                val old = current[it.name]
                if (old?.intervalDays == intervals[it] && old.salesLimit == intervals.salesLimit(it)) null
                else {
                    val previous = old?.updatedAt?.takeIf(String::isNotBlank)?.let(Instant::parse) ?: Instant.EPOCH
                    val version = maxOf(Instant.now(), previous.plusMillis(1)).truncatedTo(ChronoUnit.MILLIS)
                    MaintenanceSettingsEntity(it.name, intervals[it], intervals.salesLimit(it),
                        updatedAt = version.toString(), isSynced = false)
                }
            })
        }
        if (dao.getPending().isNotEmpty()) enqueue()
    }

    suspend fun refresh() {
        val loader = load ?: return
        withContext(Dispatchers.IO) { syncMutex.withLock {
            initialize()
            val rows = loader()
            db.withTransaction {
                val current = dao.getAll().associateBy { it.nodeType }
                dao.upsertAll(rows.filter { row ->
                    val old = current[row.nodeType]
                    old == null || (old.isSynced && (old.updatedAt.isBlank() ||
                        Instant.parse(row.updatedAt) >= Instant.parse(old.updatedAt)))
                }.map { it.copy(isSynced = true, syncError = false, syncErrorType = "") })
            }
        } }
    }

    suspend fun syncPending(): GoogleSheetsEntitySyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val rows = dao.getPending()
            if (rows.isEmpty()) return@withLock GoogleSheetsEntitySyncResult.success()
            val result = try {
                send?.invoke(rows) ?: GoogleSheetsEntitySyncResult.error(GoogleSheetsSyncErrorType.CONNECTION)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                GoogleSheetsEntitySyncResult.error(if (error is java.io.IOException)
                    GoogleSheetsSyncErrorType.CONNECTION else GoogleSheetsSyncErrorType.UNKNOWN)
            }
            db.withTransaction {
                rows.forEach { dao.markResult(it.nodeType, it.updatedAt, result.synced,
                    !result.synced, if (result.synced) "" else result.errorType.name) }
            }
            result
        }
    }

    private fun model(rows: List<MaintenanceSettingsEntity>): ServiceIntervals {
        val nodes = rows.associateBy { it.nodeType }
        return ServiceIntervals(
            TechServiceNode.entries.associateWith { nodes[it.name]?.intervalDays ?: it.defaultIntervalDays },
            TechServiceNode.entries.associateWith { nodes[it.name]?.salesLimit ?: 0L }
        )
    }

    private companion object { val syncMutex = Mutex() }
}
