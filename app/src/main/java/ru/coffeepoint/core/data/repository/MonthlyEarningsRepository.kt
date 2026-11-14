package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.AppDatabase
import ru.coffeepoint.core.auth.UserRole
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.preferences.MonthlyEarningsPreferences
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.time.OffsetDateTime
import java.time.Instant
import java.util.TimeZone

class MonthlyEarningsRepository(
    context: Context,
    private val employeeRole: UserRole = UserRole.OPERATOR,
    private val db: AppDatabase = DatabaseProvider.getDatabase(context),
    private val preferences: MonthlyEarningsPreferences = MonthlyEarningsPreferences(context, employeeRole.name),
    private val enqueueSync: () -> Unit = {
        ru.coffeepoint.core.sync.google.GoogleSheetsSyncManager().enqueueBackgroundSync(context.applicationContext)
    }
) {

    private val employeeSyncRepository = EmployeeSyncRepository(context, db)
    private val employeeDao = db.employeeDao()
    private val employeeTransactionDao = db.employeeTransactionDao()

    init { require(employeeRole == UserRole.OPERATOR || employeeRole == UserRole.SUPPLY_OPERATOR) }

    fun getState(): MonthlyEarningsState {

        val settlementAmount = preferences.getSettlementAmount()

        return MonthlyEarningsState(
            workStartDateText = preferences.getWorkStartDateText()
                ?: if (employeeRole == UserRole.OPERATOR) toDisplayDate(DEFAULT_EMPLOYEE_WORK_START_DATE) else "—",
            settlementText = formatEarnings(settlementAmount),
            settlementAmount = settlementAmount,
            employeeName = preferences.getEmployeeName()
        )

    }

    suspend fun addPayment(amount: Int) {
        require(amount > 0)

        val employee = currentEmployee()
        val now = Calendar.getInstance(MOSCOW).time
        val storageDate = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).apply { timeZone = MOSCOW }.format(now)

        employeeTransactionDao.insert(
            EmployeeTransactionEntity(
                id = UUID.randomUUID().toString(),
                employeeId = employee.id,
                type = TRANSACTION_TYPE_PAYMENT,
                operationDate = storageDate,
                periodFrom = null,
                periodTo = null,
                salesCount = null,
                amount = amount,
                comment = "",
                createdAt = storageDate,
                updatedAt = storageDate,
                isSynced = false,
                isLocalOnly = employee.isLocalOnly
            )
        )
        refreshLocalState()
        enqueueSync()

    }

    suspend fun refreshEmployeeTransactions(): Boolean {

        val refreshed = employeeSyncRepository.refreshEmployeeDataFromGoogleSheets()
        refreshLocalState()
        return refreshed
    }

    suspend fun refreshLocalState() {
        val current = currentEmployee()

        preferences.updateWorkStartDateText(
            toDisplayDate(current.workStartDate)
        )
        preferences.updateEmployeeName(current.name)
        preferences.updateSettlementAmount(
            getSettlementAmount(
                employee = current
            )
        )

    }

    suspend fun syncPendingData() {
        try {
            employeeSyncRepository.syncEmployeeData()
        } catch (cancellation: kotlinx.coroutines.CancellationException) { throw cancellation }
        catch (_: Exception) { /* The persisted queue is retried by WorkManager. */ }
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

        val candidates = employeeDao.getActiveByRole(employeeRole.name)
        if (employeeRole == UserRole.SUPPLY_OPERATOR) {
            return candidates.singleOrNull()
                ?: throw LocalSupplierUnavailableException()
        }
        val employee = candidates.firstOrNull()

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
        ).apply { timeZone = MOSCOW }.format(millis)

    }

    private fun toDisplayDate(
        storageDate: String
    ): String {

        if (storageDate.isBlank()) return "—"
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
        } ?: return "—"

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
            timeZone = MOSCOW
        }.parse(startDate)?.let { date ->
            Calendar.getInstance(MOSCOW).apply {
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
            timeZone = MOSCOW
        }.parse(endDate)?.let { date ->
            Calendar.getInstance(MOSCOW).apply {
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

        runCatching { OffsetDateTime.parse(paymentDate).toInstant().toEpochMilli() }.getOrNull()?.let { return it }
        runCatching { Instant.parse(paymentDate).toEpochMilli() }.getOrNull()?.let { return it }
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
                    timeZone = MOSCOW
                }.parse(paymentDate)?.time
            }.getOrNull()
        }

    }

    private companion object {

        val MOSCOW: TimeZone = TimeZone.getTimeZone("Europe/Moscow")

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
    val settlementAmount: Int,
    val employeeName: String = ""
)

data class MonthlyEarningsOperation(
    val id: String,
    val operationDateMillis: Long,
    val dateText: String,
    val amountText: String,
    val isEarning: Boolean
)

