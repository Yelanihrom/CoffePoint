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

    @Query(
        """
        SELECT *
        FROM employees_transactions
        WHERE employeeId = :employeeId
            AND type = :type
        """
    )
    suspend fun getByEmployeeAndType(
        employeeId: Int,
        type: String
    ): List<EmployeeTransactionEntity>

    @Query(
        """
        SELECT COUNT(*)
        FROM employees_transactions
        WHERE employeeId = :employeeId
            AND type = :type
            AND periodFrom <= :endDate
            AND periodTo >= :startDate
        """
    )
    suspend fun getOverlappingPeriodCount(
        employeeId: Int,
        type: String,
        startDate: String,
        endDate: String
    ): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM employees_transactions
        WHERE employeeId = :employeeId
            AND type = :type
            AND periodFrom >= :startDate
            AND periodTo <= :endDate
        """
    )
    suspend fun getPeriodTotalAmount(
        employeeId: Int,
        type: String,
        startDate: String,
        endDate: String
    ): Int

}
