package ru.coffeepoint

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.repository.ServiceIntervalRepository
import ru.coffeepoint.feature.station.model.ServiceIntervals
import ru.coffeepoint.feature.station.model.TechServiceNode
import java.util.UUID

class MaintenanceSettingsDatabaseTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext

    @Test fun migration41KeepsLocalRulesPendingWithoutLosingValues(): Unit = runBlocking {
        val name = "maintenance-settings-41-${UUID.randomUUID()}.db"
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().let {
                it.openHelper.writableDatabase; it.close()
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE maintenance_settings")
                it.execSQL("CREATE TABLE maintenance_settings (nodeType TEXT NOT NULL PRIMARY KEY, intervalDays INTEGER NOT NULL, salesLimit INTEGER NOT NULL)")
                it.execSQL("INSERT INTO maintenance_settings VALUES ('MIXERS',45,500)")
                it.version = 41
            }
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(DatabaseProvider.MIGRATION_41_42).build()
            try {
                val rule = db.maintenanceSettingsDao().getPending().single()
                assertEquals(45, rule.intervalDays)
                assertEquals(500L, rule.salesLimit)
                assertFalse(rule.isSynced)
                java.time.Instant.parse(rule.updatedAt)
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }

    @Test fun migration40AddsIndependentSettingsTableAndKeepsExistingData() = runBlocking {
        val name = "maintenance-settings-${UUID.randomUUID()}.db"
        val prefs = context.getSharedPreferences(name, 0)
        try {
            Room.databaseBuilder(context, AppDatabase::class.java, name).build().let { db ->
                db.openHelper.writableDatabase.execSQL("INSERT INTO employees (id,name,role,workStartDate,isActive,createdAt,updatedAt,isSynced,syncError,syncErrorType,isLocalOnly) VALUES (77,'Saved','OPERATOR','',1,'','',1,0,'',0)")
                db.close()
            }
            SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("DROP TABLE maintenance_settings")
                it.version = 40
            }
            val db = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(DatabaseProvider.MIGRATION_40_41, DatabaseProvider.MIGRATION_41_42).build()
            try {
                val schema = JSONObject(instrumentation.context.assets.open("contracts/maintenance-settings-schema.json").bufferedReader().readText())
                val expected = schema.getJSONArray("columns")
                db.openHelper.readableDatabase.query("PRAGMA table_info(maintenance_settings)").use { cursor ->
                    var i = 0
                    while (cursor.moveToNext()) {
                        val column = expected.getJSONObject(i++)
                        assertEquals(column.getString("name"), cursor.getString(cursor.getColumnIndexOrThrow("name")))
                        assertEquals(column.getString("type"), cursor.getString(cursor.getColumnIndexOrThrow("type")))
                        assertEquals(column.getInt("primaryKey"), cursor.getInt(cursor.getColumnIndexOrThrow("pk")))
                        assertEquals(column.getBoolean("notNull"), cursor.getInt(cursor.getColumnIndexOrThrow("notnull")) == 1)
                    }
                    assertEquals(expected.length(), i)
                }
                db.openHelper.readableDatabase.query("SELECT name FROM employees WHERE id=77").use {
                    assertTrue(it.moveToFirst()); assertEquals("Saved", it.getString(0))
                }
                prefs.edit().putInt("MIXERS", 45).putInt("EXHAUST", -10).commit()
                val repository = ServiceIntervalRepository(db, prefs)
                val first = async { repository.read() }
                val second = async { repository.read() }
                assertEquals(first.await(), second.await())
                assertEquals(45, repository.read()[TechServiceNode.MIXERS])
                assertEquals(720, repository.read()[TechServiceNode.EXHAUST])
                assertTrue(repository.read().salesLimits.values.all { it == 0L })
                val saved = ServiceIntervals(repository.read().values + (TechServiceNode.MIXERS to 60),
                    repository.read().salesLimits + (TechServiceNode.MIXERS to 500L))
                repository.save(saved)
                assertEquals(saved, repository.observe().first())
                prefs.edit().putInt("MIXERS", 10).commit()
                assertEquals(saved, ServiceIntervalRepository(db, prefs).read())
                assertEquals(6, db.maintenanceSettingsDao().getAll().size)
            } finally { db.close() }
            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                val settings = ServiceIntervalRepository(reopened, prefs).read()
                assertEquals(60, settings[TechServiceNode.MIXERS])
                assertEquals(500L, settings.salesLimit(TechServiceNode.MIXERS))
            } finally { reopened.close() }
        } finally {
            context.deleteDatabase(name)
            prefs.edit().clear().commit()
        }
    }
}
