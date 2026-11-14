package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.coffeepoint.core.data.local.entity.EmployeeEntity

@Dao
interface EmployeeDao {

    @Query("SELECT * FROM employees ORDER BY isActive DESC, id")
    suspend fun getAll(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE isSynced = 0 AND isLocalOnly = 0")
    suspend fun getUnsynced(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE isLocalOnly = 1")
    suspend fun getLocalOnly(): List<EmployeeEntity>

    @Transaction
    suspend fun insertRemoteAll(entities: List<EmployeeEntity>) {
        assertRemoteEmployeesAllowed(entities)
        insertAll(entities)
    }

    suspend fun assertRemoteEmployeesAllowed(entities: List<EmployeeEntity>) {
        val localIds = getLocalOnly().map { it.id }.toSet()
        check(entities.none { it.id in localIds || it.isLocalOnly }) {
            "Remote employee ID conflicts with a local-only employee"
        }
    }

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Int): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE isActive = 1 AND role = :role ORDER BY id")
    suspend fun getActiveByRole(role: String): List<EmployeeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmployeeEntity>)

    @Query("DELETE FROM employees")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE employees
        SET
            isSynced = :isSynced,
            syncError = :syncError,
            syncErrorType = :syncErrorType
        WHERE id = :id
        """
    )
    suspend fun updateSyncStatus(
        id: Int,
        isSynced: Boolean,
        syncError: Boolean,
        syncErrorType: String
    )

}
