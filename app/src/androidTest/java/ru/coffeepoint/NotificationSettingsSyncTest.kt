package ru.coffeepoint

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.repository.NotificationSettingsRepository
import ru.coffeepoint.core.notification.*
import ru.coffeepoint.core.sync.google.*

class NotificationSettingsSyncTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("notification_sync_test", 0)
    private val supplier = UserRole.SUPPLY_OPERATOR
    private val warehouse = NotificationOption.WAREHOUSE
    private lateinit var store: NotificationSettings

    @Before fun before() { prefs.edit().clear().commit(); store = NotificationSettings(prefs) }
    @After fun after() { prefs.edit().clear().commit() }

    private fun remote(stamp: String = "2026-01-01T00:00:00Z") = UserRole.entries.map { role ->
        NotificationSettingsRow(role, NotificationOption.entries.associateWith { it.defaultEnabled(role) }, stamp)
    }

    @Test fun defaultRepositorySynchronizesWithoutATestEnvironmentSwitch() = runBlocking {
        store.setEnabled(supplier, warehouse, true)
        var reads = 0
        var writes = 0
        val repository = NotificationSettingsRepository(store, { reads++; remote() }, {
            writes++
            GoogleSheetsEntitySyncResult.success()
        })
        repository.refresh()
        assertTrue(repository.syncPending().synced)
        assertEquals(1, reads)
        assertEquals(1, writes)
        assertTrue(store.pending().isEmpty())
    }

    @Test fun offlineChangeSurvivesRecreationAndIsAcknowledgedOnce() = runBlocking {
        store.setEnabled(supplier, warehouse, true)
        val offline = NotificationSettingsRepository(store, { remote() }, {
            GoogleSheetsEntitySyncResult.error(GoogleSheetsSyncErrorType.CONNECTION)
        }, enabled = true)
        assertFalse(offline.syncPending().synced)
        val reopened = NotificationSettings(prefs)
        assertTrue(reopened.isEnabled(supplier, warehouse))
        var sends = 0
        val retry = NotificationSettingsRepository(reopened, { remote() }, {
            sends++
            assertEquals(mapOf(warehouse to true), it.values)
            GoogleSheetsEntitySyncResult.success()
        }, enabled = true)
        assertTrue(retry.syncPending().synced)
        assertTrue(retry.syncPending().synced)
        assertEquals(1, sends)
        assertTrue(reopened.pending().isEmpty())
    }

    @Test fun importPreservesDirtyFlagsAndAcknowledgementCannotClearNewEdits() {
        store.setEnabled(supplier, warehouse, true)
        val oldRequest = store.pending().single()
        val rows = remote().map { if (it.role == supplier)
            it.copy(values = it.values + (NotificationOption.CRITICAL_STOCK to false)) else it }
        store.importRows(rows)
        assertTrue(store.isEnabled(supplier, warehouse))
        assertFalse(store.isEnabled(supplier, NotificationOption.CRITICAL_STOCK))
        store.setEnabled(supplier, NotificationOption.MIXERS, true)
        store.acknowledge(oldRequest)
        assertEquals(2, store.pending().single().values.size)
        assertTrue(store.isEnabled(UserRole.MANAGER, warehouse))
    }

    @Test fun conflictReloadsCleanFlagsThenRetriesOnlyLocallyEditedFields() = runBlocking {
        store.setEnabled(supplier, warehouse, true)
        var sends = 0
        val rows = remote("2099-01-01T00:00:00Z").map { if (it.role == supplier)
            it.copy(values = it.values + (NotificationOption.CRITICAL_STOCK to false)) else it }
        val repository = NotificationSettingsRepository(store, { rows }, { row ->
            sends++
            assertEquals(mapOf(warehouse to true), row.values)
            if (sends == 1) GoogleSheetsEntitySyncResult.error(GoogleSheetsSyncErrorType.CONFLICT)
            else {
                assertTrue(java.time.Instant.parse(row.updatedAt) > java.time.Instant.parse(rows[0].updatedAt))
                GoogleSheetsEntitySyncResult.success()
            }
        }, enabled = true)
        assertTrue(repository.syncPending().synced)
        assertEquals(2, sends)
        assertFalse(store.isEnabled(supplier, NotificationOption.CRITICAL_STOCK))
        assertTrue(store.pending().isEmpty())
    }

    @Test fun cancellationKeepsPendingAndDisabledEnvironmentNeverUsesNetwork() = runBlocking {
        store.setEnabled(supplier, warehouse, true)
        val disabled = NotificationSettingsRepository(store, { error("Unexpected read") },
            { error("Unexpected write") }, enabled = false)
        disabled.refresh()
        assertTrue(disabled.syncPending().synced)
        val cancelled = NotificationSettingsRepository(store, { remote() },
            { throw CancellationException("cancelled") }, enabled = true)
        try { cancelled.syncPending(); fail("Expected cancellation") } catch (_: CancellationException) { }
        assertEquals(1, store.pending().size)
    }
}
