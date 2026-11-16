package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.coffeepoint.core.data.local.entity.MaintenanceSettingsEntity

@Dao
interface MaintenanceSettingsDao {
    @Query("SELECT * FROM maintenance_settings")
    suspend fun getAll(): List<MaintenanceSettingsEntity>

    @Query("SELECT * FROM maintenance_settings WHERE isSynced = 0")
    suspend fun getPending(): List<MaintenanceSettingsEntity>

    @Query("UPDATE maintenance_settings SET isSynced = :synced, syncError = :error, syncErrorType = :errorType WHERE nodeType = :nodeType AND updatedAt = :version AND isSynced = 0")
    suspend fun markResult(nodeType: String, version: String, synced: Boolean, error: Boolean, errorType: String)

    @Query("SELECT * FROM maintenance_settings")
    fun observeAll(): Flow<List<MaintenanceSettingsEntity>>

    @Upsert
    suspend fun upsertAll(items: List<MaintenanceSettingsEntity>)
}
