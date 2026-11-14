package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity

@Dao
interface EmployeeTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLocalEarning(entity: EmployeeTransactionEntity)

    @Query("SELECT * FROM employees_transactions WHERE id = :id")
    suspend fun getById(id: String): EmployeeTransactionEntity?

    @Query("SELECT * FROM employees_transactions WHERE sourceType = :sourceType AND sourceId = :sourceId")
    suspend fun getBySource(sourceType: String, sourceId: String): EmployeeTransactionEntity?

    @Query("DELETE FROM employees_transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM employees_transactions WHERE isLocalOnly = 1")
    suspend fun getLocalOnly(): List<EmployeeTransactionEntity>

    @Query("SELECT id FROM employees WHERE isLocalOnly = 1")
    suspend fun localEmployeeIds(): List<Int>

    suspend fun assertRemoteTransactionsAllowed(entities: List<EmployeeTransactionEntity>) {
        val localIds = getLocalOnly().map { it.id }.toSet()
        val localEmployees = localEmployeeIds().toSet()
        check(entities.none { it.isLocalOnly || it.id in localIds || it.employeeId in localEmployees }) {
            "Remote transaction conflicts with local-only employee data"
        }
    }

    @Transaction
    suspend fun insertRemoteAll(entities: List<EmployeeTransactionEntity>) {
        assertRemoteTransactionsAllowed(entities)
        insertAll(entities)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: EmployeeTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EmployeeTransactionEntity>)

    @Query("SELECT * FROM employees_transactions ORDER BY operationDate DESC")
    suspend fun getAll(): List<EmployeeTransactionEntity>

    @Query("SELECT * FROM employees_transactions WHERE isSynced = 0 AND isLocalOnly = 0 AND COALESCE(sourceType, '') != 'SUPPLY_SERVICE' AND employeeId NOT IN (SELECT id FROM employees WHERE isLocalOnly = 1)")
    suspend fun getUnsynced(): List<EmployeeTransactionEntity>

    @Query("UPDATE employees_transactions SET isLocalOnly = 0 WHERE employeeId = :employeeId AND type = 'PAYMENT'")
    suspend fun enableSupplierPayments(employeeId: Int)

    @Query("UPDATE employees_transactions SET syncError = 1, syncErrorType = :errorType WHERE sourceType = 'SUPPLY_SERVICE' AND sourceId = :visitId AND isSynced = 0")
    suspend fun markVisitEarningError(visitId: String, errorType: String)

    @Query("DELETE FROM employees_transactions")
    suspend fun deleteAll()

    @Query("DELETE FROM employees_transactions WHERE isSynced = 1 AND isLocalOnly = 0 AND employeeId IN (SELECT id FROM employees WHERE role = 'OPERATOR')")
    suspend fun deleteRemoteRecords()

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
