package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.preferences.MonthlyEarningsPreferences
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MonthlyEarningsRepository(
    context: Context
) {

    private val preferences = MonthlyEarningsPreferences(context)
    private val employeeSyncRepository = EmployeeSyncRepository(context)
    private val db = DatabaseProvider.getDatabase(context)
    private val employeeDao = db.employeeDao()
    private val employeeTransactionDao = db.employeeTransactionDao()

    fun getState(): MonthlyEarningsState {

        val settlementAmount = preferences.getSettlementAmount()

        return MonthlyEarningsState(
            workStartDateText = preferences.getWorkStartDateText()
                ?: toDisplayDate(DEFAULT_EMPLOYEE_WORK_START_DATE),
            settlementText = formatEarnings(settlementAmount),
            settlementAmount = settlementAmount
        )

    }

    suspend fun addPayment(amount: Int) {

        val now = Calendar.getInstance().time
        val storageDate = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(now)

        employeeTransactionDao.insert(
            EmployeeTransactionEntity(
                id = UUID.randomUUID().toString(),
                employeeId = currentEmployeeId(),
                type = TRANSACTION_TYPE_PAYMENT,
                operationDate = storageDate,
                periodFrom = null,
                periodTo = null,
                salesCount = null,
                amount = amount,
                comment = "",
                createdAt = storageDate,
                updatedAt = storageDate,
                isSynced = false
            )
        )
        preferences.updateSettlementAmount(
            getSettlementAmount(
                employee = currentEmployee()
            )
        )

    }

    suspend fun refreshEmployeeTransactions(): Boolean {

        if (!employeeSyncRepository.refreshEmployeeDataFromGoogleSheets()) return false

        preferences.updateWorkStartDateText(
            toDisplayDate(currentEmployee().workStartDate)
        )
        preferences.updateSettlementAmount(
            getSettlementAmount(
                employee = currentEmployee()
            )
        )

        return true

    }

    suspend fun syncPendingData() {
        runCatching {
            employeeSyncRepository.syncEmployeeData()
        }
    }

    suspend fun getOperationsForPeriod(
        startDate: String,
        endDate: String? = null
    ): List<MonthlyEarningsOperation> {

        val startMillis = parsePeriodStartMillis(startDate)
            ?: return emptyList()
        val endMillis = endDate?.let { value ->
            parsePeriodEndMillis(value)
        } ?: System.currentTimeMillis()

        return employeeTransactionDao.getByEmployee(
            employeeId = currentEmployeeId()
        )
            .mapNotNull { transaction ->
                val paymentMillis = parsePaymentDateMillis(transaction.operationDate)
                    ?: return@mapNotNull null

                if (paymentMillis !in startMillis..endMillis) {
                    return@mapNotNull null
                }

                MonthlyEarningsOperation(
                    id = transaction.id,
                    operationDateMillis = paymentMillis,
                    dateText = formatPaymentDisplayDate(paymentMillis),
                    amountText = formatOperationAmount(
                        amount = transaction.amount,
                        type = transaction.type
                    ),
                    isEarning = transaction.type == TRANSACTION_TYPE_EARNING
                )
            }
            .sortedByDescending { payment ->
                payment.operationDateMillis
            }

    }

    private suspend fun getSettlementAmount(
        employee: EmployeeEntity
    ): Int {

        val earningsAmount = employeeTransactionDao.getTotalAmount(
            employeeId = employee.id,
            type = TRANSACTION_TYPE_EARNING
        )
        val paymentsAmount = employeeTransactionDao.getTotalAmount(
            employeeId = employee.id,
            type = TRANSACTION_TYPE_PAYMENT
        )

        return earningsAmount - paymentsAmount

    }

    private suspend fun currentEmployeeId(): Int {

        return currentEmployee().id

    }

    private suspend fun currentEmployee(): EmployeeEntity {

        val employee = employeeDao.getFirstActive()

        if (employee != null) {
            return employee
        }

        val defaultEmployee = defaultEmployee()

        employeeDao.insert(defaultEmployee)

        return defaultEmployee

    }

    private fun defaultEmployee(): EmployeeEntity {

        return EmployeeEntity(
            id = DEFAULT_EMPLOYEE_ID,
            name = DEFAULT_EMPLOYEE_NAME,
            role = DEFAULT_EMPLOYEE_ROLE,
            workStartDate = DEFAULT_EMPLOYEE_WORK_START_DATE,
            isActive = true,
            createdAt = DEFAULT_EMPLOYEE_CREATED_AT,
            updatedAt = DEFAULT_EMPLOYEE_CREATED_AT,
            isSynced = false
        )

    }

    private fun formatEarnings(value: Int): String {

        return "${NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(value)} \u20bd"

    }

    private fun formatOperationAmount(
        amount: Int,
        type: String
    ): String {
        val sign = if (type == TRANSACTION_TYPE_EARNING) "+" else "-"

        return "$sign${formatEarnings(amount)}"
    }

    private fun formatPaymentDisplayDate(
        millis: Long
    ): String {

        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).format(millis)

    }

    private fun toDisplayDate(
        storageDate: String
    ): String {

        val patterns = listOf(
            "yyyy-MM-dd",
            "yyyy-M-d",
            "dd.MM.yyyy"
        )

        val date = patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(
                    pattern,
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }.parse(storageDate)
            }.getOrNull()
        } ?: Calendar.getInstance().time

        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).format(date)

    }

    private fun parsePeriodStartMillis(
        startDate: String
    ): Long? {

        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }.parse(startDate)?.let { date ->
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

    }

    private fun parsePeriodEndMillis(
        endDate: String
    ): Long? {

        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }.parse(endDate)?.let { date ->
            Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

    }

    private fun parsePaymentDateMillis(
        paymentDate: String
    ): Long? {

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-M-d'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-M-d HH:mm:ss",
            "yyyy-MM-dd",
            "yyyy-M-d",
            "dd.MM.yyyy"
        )

        return patterns.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(
                    pattern,
                    Locale.getDefault()
                ).apply {
                    isLenient = false
                }.parse(paymentDate)?.time
            }.getOrNull()
        }

    }

    private companion object {

        const val DEFAULT_EMPLOYEE_ID = 1
        const val DEFAULT_EMPLOYEE_NAME = "Максим"
        const val DEFAULT_EMPLOYEE_ROLE = "OPERATOR"
        const val DEFAULT_EMPLOYEE_WORK_START_DATE = "2026-09-13"
        const val DEFAULT_EMPLOYEE_CREATED_AT = "2026-09-13T00:00:00"
        const val TRANSACTION_TYPE_EARNING = "EARNING"
        const val TRANSACTION_TYPE_PAYMENT = "PAYMENT"

    }

}

data class MonthlyEarningsState(
    val workStartDateText: String,
    val settlementText: String,
    val settlementAmount: Int
)

data class MonthlyEarningsOperation(
    val id: String,
    val operationDateMillis: Long,
    val dateText: String,
    val amountText: String,
    val isEarning: Boolean
)

