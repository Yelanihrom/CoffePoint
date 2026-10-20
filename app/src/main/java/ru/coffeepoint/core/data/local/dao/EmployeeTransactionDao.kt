package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity

@Dao
interface EmployeeTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmployeeTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmployeeTransactionEntity>)

    @Query("SELECT * FROM employees_transactions ORDER BY operationDate DESC")
    suspend fun getAll(): List<EmployeeTransactionEntity>

    @Query("SELECT * FROM employees_transactions WHERE isSynced = 0")
    suspend fun getUnsynced(): List<EmployeeTransactionEntity>

    @Query("DELETE FROM employees_transactions")
    suspend fun deleteAll()

    @Query(
        """
        UPDATE employees_transactions
        SET
            isSynced = :isSynced,
            syncError = :syncError,
            syncErrorType = :syncErrorType
        WHERE id = :id
        """
    )
    suspend fun updateSyncStatus(
        id: String,
        isSynced: Boolean,
        syncError: Boolean,
        syncErrorType: String
    )

    @Query(
        """
        SELECT *
        FROM employees_transactions
        WHERE employeeId = :employeeId
        """
    )
    suspend fun getByEmployee(
        employeeId: Int
    ): List<EmployeeTransactionEntity>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM employees_transactions
        WHERE employeeId = :employeeId
            AND type = :type
        """
    )
    suspend fun getTotalAmount(
        employeeId: Int,
        type: String
    ): Int

}
