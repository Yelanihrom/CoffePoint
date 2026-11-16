package ru.coffeepoint

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.entity.MaintenanceSettingsEntity
import ru.coffeepoint.core.data.repository.ServiceIntervalRepository
import ru.coffeepoint.core.sync.google.*
import ru.coffeepoint.feature.station.model.*
import java.io.IOException

class MaintenanceSettingsSyncTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun remote(days: Int = 30) = TechServiceNode.entries.map {
        MaintenanceSettingsEntity(it.name, if (it == TechServiceNode.MIXERS) days else it.defaultIntervalDays,
            500L, "2026-11-01T00:00:00Z")
    }

    @Test fun importProtectsPendingAndRetrySendsTheSameVersion() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            var fail = true
            var enqueues = 0
            val requests = mutableListOf<List<MaintenanceSettingsEntity>>()
            val store = ServiceIntervalRepository(db, load = { remote() }, send = {
                requests.add(it)
                if (fail) throw IOException("offline")
                GoogleSheetsEntitySyncResult.success()
            }, enqueue = { enqueues++ })
            store.refresh()
            val old = store.read()
            store.save(old.copy(values = old.values + (TechServiceNode.MIXERS to 45)))
            assertEquals(1, enqueues)
            assertEquals(1, db.maintenanceSettingsDao().getPending().size)
            assertEquals(GoogleSheetsSyncErrorType.CONNECTION, store.syncPending().errorType)
            val pending = db.maintenanceSettingsDao().getPending().single()
            assertTrue(pending.syncError)
            assertEquals("CONNECTION", pending.syncErrorType)
            store.refresh()
            assertEquals(45, store.read()[TechServiceNode.MIXERS])
            fail = false
            assertTrue(store.syncPending().synced)
            assertEquals(requests[0].single().updatedAt, requests[1].single().updatedAt)
            assertTrue(db.maintenanceSettingsDao().getPending().isEmpty())
            assertFalse(db.maintenanceSettingsDao().getAll().single { it.nodeType == "MIXERS" }.syncError)
            // An older server response cannot rewind a confirmed local edit.
            store.refresh()
            assertEquals(45, store.read()[TechServiceNode.MIXERS])
            assertEquals(6, db.maintenanceSettingsDao().getAll().size)
        } finally { db.close() }
    }

    @Test fun oldAcknowledgementCannotConfirmConcurrentEdit() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val started = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val store = ServiceIntervalRepository(db, send = {
                started.complete(Unit); finish.await(); GoogleSheetsEntitySyncResult.success()
            })
            store.read()
            val change = ServiceIntervals(values = ServiceIntervals().values + (TechServiceNode.MIXERS to 45))
            store.save(change)
            val sending = async { store.syncPending() }
            started.await()
            store.save(change.copy(values = change.values + (TechServiceNode.MIXERS to 60)))
            finish.complete(Unit)
            assertTrue(sending.await().synced)
            assertEquals(60, db.maintenanceSettingsDao().getPending().single().intervalDays)
            assertTrue(store.syncPending().synced)
            assertTrue(db.maintenanceSettingsDao().getPending().isEmpty())
        } finally { db.close() }
    }

    @Test fun conflictAndCancellationKeepLocalChanges() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            var cancel = false
            val store = ServiceIntervalRepository(db, send = {
                if (cancel) throw CancellationException("cancel")
                GoogleSheetsEntitySyncResult.error(GoogleSheetsSyncErrorType.CONFLICT)
            })
            store.read()
            store.save(ServiceIntervals(salesLimits = ServiceIntervals().salesLimits + (TechServiceNode.MIXERS to 500L)))
            assertEquals(GoogleSheetsSyncErrorType.CONFLICT, store.syncPending().errorType)
            val before = db.maintenanceSettingsDao().getPending().single()
            assertEquals("CONFLICT", before.syncErrorType)
            cancel = true
            try { store.syncPending(); fail("Cancellation swallowed") } catch (_: CancellationException) { }
            assertEquals(before, db.maintenanceSettingsDao().getPending().single())
        } finally { db.close() }
    }
}
