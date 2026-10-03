package ru.coffeepoint.core.data.repository

import android.content.Context
import ru.coffeepoint.core.data.local.DatabaseProvider
import ru.coffeepoint.core.data.local.entity.EmployeeEntity
import ru.coffeepoint.core.data.local.entity.EmployeeTransactionEntity
import ru.coffeepoint.core.data.preferences.MonthlyEarningsPreferences
import ru.coffeepoint.core.data.remote.repository.SalesRepository
import ru.coffeepoint.core.network.NetworkChecker
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class MonthlyEarningsRepository(
    context: Context
) {

    private val stationRepository = StationRepository(context)
    private val salesRepository = SalesRepository()
    private val networkChecker = NetworkChecker(context)
    private val preferences = MonthlyEarningsPreferences(context)
    private val employeeSyncRepository = EmployeeSyncRepository(context)
    private val db = DatabaseProvider.getDatabase(context)
    private val employeeDao = db.employeeDao()
    private val employeeTransactionDao = db.employeeTransactionDao()

    suspend fun refresh(
        startDate: String,
        endDate: String? = null
    ): MonthlyEarningsResult {

        if (!networkChecker.hasInternetConnection()) {
            return MonthlyEarningsResult.ConnectionError
        }

        return runCatching {

            employeeSyncRepository.refreshEmployeeDataFromGoogleSheets()

            val employee = currentEmployee()
            val workStartDate = toDisplayDate(employee.workStartDate)
            val terminalIds = stationRepository.getStations()
                .filter { detail ->
                    !detail.station.excludeFromOperatorEarnings
                }
                .mapNotNull { detail ->
                    detail.station.terminalId
                }
                .toSet()

            val salesByTerminal = salesRepository.loadSalesByTerminal(
                dateFrom = toApiStartDate(startDate),
                dateTo = endDate?.let { value ->
                    toApiEndDate(value)
                }
            )

            val salesCount = terminalIds.sumOf { terminalId ->
                salesByTerminal[terminalId] ?: 0
            }
            val paymentsAmount = getPaymentsAmountForPeriod(
                startDate = startDate,
                endDate = endDate
            )
            val earningsAmount = salesCount * EARNINGS_PER_SALE
            val periodTo = endDate ?: currentDisplayDate()
            val settlementAmount = getSettlementAmount(
                employee = employee,
                endDate = periodTo
            )

            preferences.save(
                salesCount = salesCount,
                paymentsAmount = paymentsAmount,
                settlementAmount = settlementAmount,
                workStartDateText = workStartDate,
                periodFrom = startDate,
                periodTo = periodTo,
                timeMillis = System.currentTimeMillis()
            )

            MonthlyEarningsResult.Success(
                salesCount = salesCount
            )

        }.getOrElse {

            MonthlyEarningsResult.ConnectionError

        }

    }

    suspend fun accrueEarnings(
        startDate: String,
        endDate: String
    ): MonthlyEarningsAccrualResult {

        val state = getState()

        if (state.periodFrom != startDate || state.periodTo != endDate) {
            return MonthlyEarningsAccrualResult.NeedRefresh
        }

        val employee = currentEmployee()
        val storageStartDate = toStorageDate(startDate)
        val storageEndDate = toStorageDate(endDate)
        val hasOverlappingEarning = employeeTransactionDao.getOverlappingPeriodCount(
            employeeId = employee.id,
            type = TRANSACTION_TYPE_EARNING,
            startDate = storageStartDate,
            endDate = storageEndDate
        ) > 0

        if (hasOverlappingEarning) {
            return MonthlyEarningsAccrualResult.AlreadyExists
        }

        val salesCount = preferences.getSalesCount()
        val amount = salesCount * EARNINGS_PER_SALE

        saveEarning(
            employeeId = employee.id,
            startDate = startDate,
            endDate = endDate,
            salesCount = salesCount,
            amount = amount
        )
        employeeSyncRepository.syncEmployeeTransactions()
        preferences.updateSettlementAmount(
            getSettlementAmount(
                employee = employee,
                endDate = endDate
            )
        )

        return MonthlyEarningsAccrualResult.Success

    }

    fun getState(): MonthlyEarningsState {

        val salesCount = preferences.getSalesCount()
        val earningsAmount = salesCount * EARNINGS_PER_SALE
        val settlementAmount = preferences.getSettlementAmount()

        return MonthlyEarningsState(
            workStartDateText = preferences.getWorkStartDateText()
                ?: toDisplayDate(DEFAULT_EMPLOYEE_WORK_START_DATE),
            earningsText = formatEarnings(earningsAmount),
            settlementText = formatEarnings(settlementAmount),
            settlementAmount = settlementAmount,
            periodFrom = preferences.getPeriodFrom(),
            periodTo = preferences.getPeriodTo()
        )

    }

    suspend fun addPayment(
        amount: Int,
        startDate: String,
        endDate: String? = null
    ) {

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
                updatedAt = storageDate
            )
        )
        employeeSyncRepository.syncEmployeeTransactions()

        preferences.updatePaymentsAmount(
            getPaymentsAmountForPeriod(
                startDate = startDate,
                endDate = endDate
            )
        )
        preferences.updateSettlementAmount(
            getSettlementAmount(
                employee = currentEmployee(),
                endDate = endDate ?: currentDisplayDate()
            )
        )

    }

    suspend fun refreshPaymentsForPeriod(
        startDate: String,
        endDate: String? = null
    ) {

        employeeSyncRepository.refreshEmployeeDataFromGoogleSheets()

        preferences.updatePaymentsAmount(
            getPaymentsAmountForPeriod(
                startDate = startDate,
                endDate = endDate
            )
        )
        preferences.updateSettlementAmount(
            getSettlementAmount(
                employee = currentEmployee(),
                endDate = endDate ?: currentDisplayDate()
            )
        )

    }

    suspend fun getPaymentsForPeriod(
        startDate: String,
        endDate: String? = null
    ): List<MonthlyEarningsPayment> {

        val startMillis = parsePeriodStartMillis(startDate)
            ?: return emptyList()
        val endMillis = endDate?.let { value ->
            parsePeriodEndMillis(value)
        } ?: System.currentTimeMillis()

        return employeeTransactionDao.getByEmployeeAndType(
            employeeId = currentEmployeeId(),
            type = TRANSACTION_TYPE_PAYMENT
        )
            .mapNotNull { transaction ->
                val paymentMillis = parsePaymentDateMillis(transaction.operationDate)
                    ?: return@mapNotNull null

                if (paymentMillis !in startMillis..endMillis) {
                    return@mapNotNull null
                }

                MonthlyEarningsPayment(
                    id = transaction.id,
                    operationDateMillis = paymentMillis,
                    dateText = formatPaymentDisplayDate(paymentMillis),
                    amountText = formatEarnings(transaction.amount),
                    comment = transaction.comment
                )
            }
            .sortedByDescending { payment ->
                payment.operationDateMillis
            }

    }

    private suspend fun getPaymentsAmountForPeriod(
        startDate: String,
        endDate: String? = null
    ): Int {

        val startMillis = parsePeriodStartMillis(startDate)
            ?: return 0
        val endMillis = endDate?.let { value ->
            parsePeriodEndMillis(value)
        } ?: System.currentTimeMillis()

        return employeeTransactionDao.getByEmployeeAndType(
            employeeId = currentEmployeeId(),
            type = TRANSACTION_TYPE_PAYMENT
        )
            .filter { transaction ->
                val paymentMillis = parsePaymentDateMillis(transaction.operationDate)
                    ?: return@filter false

                paymentMillis in startMillis..endMillis
            }
            .sumOf { transaction ->
                transaction.amount
            }

    }

    private suspend fun saveEarning(
        employeeId: Int,
        startDate: String,
        endDate: String,
        salesCount: Int,
        amount: Int
    ) {

        val now = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

        employeeTransactionDao.insert(
            EmployeeTransactionEntity(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                type = TRANSACTION_TYPE_EARNING,
                operationDate = now,
                periodFrom = toStorageDate(startDate),
                periodTo = toStorageDate(endDate),
                salesCount = salesCount,
                amount = amount,
                comment = "",
                createdAt = now,
                updatedAt = now
            )
        )

    }

    private suspend fun getSettlementAmount(
        employee: EmployeeEntity,
        endDate: String
    ): Int {

        val startDate = toDisplayDate(employee.workStartDate)
        val storageStartDate = toStorageDate(startDate)
        val storageEndDate = toStorageDate(endDate)
        val earningsAmount = employeeTransactionDao.getPeriodTotalAmount(
            employeeId = employee.id,
            type = TRANSACTION_TYPE_EARNING,
            startDate = storageStartDate,
            endDate = storageEndDate
        )
        val paymentsAmount = getPaymentsAmountForPeriod(
            startDate = startDate,
            endDate = endDate
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
            updatedAt = DEFAULT_EMPLOYEE_CREATED_AT
        )

    }

    private fun formatEarnings(value: Int): String {

        return "${NumberFormat.getIntegerInstance(Locale.forLanguageTag("ru-RU")).format(value)} \u20bd"

    }

    private fun formatPaymentDisplayDate(
        millis: Long
    ): String {

        return SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
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

    private fun currentDisplayDate(): String {

        return SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

    }

    private fun toStorageDate(date: String): String {

        val inputFormatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        val parsedDate = inputFormatter.parse(date)

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(parsedDate ?: Calendar.getInstance().time)

    }

    private fun toStorageStartDate(startDate: String): String {

        val inputFormatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        val date = inputFormatter.parse(startDate)

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(date ?: Calendar.getInstance().time)

    }

    private fun toStorageEndDate(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(Calendar.getInstance().time)

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

    private fun toApiStartDate(startDate: String): String {

        val inputFormatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        val date = inputFormatter.parse(startDate)

        val calendar = Calendar.getInstance().apply {
            time = date ?: time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(calendar.time)

    }

    private fun toApiEndDate(endDate: String): String {

        val inputFormatter = SimpleDateFormat(
            "dd.MM.yyyy",
            Locale.getDefault()
        ).apply {
            isLenient = false
        }

        val date = inputFormatter.parse(endDate)

        val calendar = Calendar.getInstance().apply {
            time = date ?: time
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(calendar.time)

    }

    private companion object {

        const val EARNINGS_PER_SALE = 15
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
    val earningsText: String,
    val settlementText: String,
    val settlementAmount: Int,
    val periodFrom: String?,
    val periodTo: String?
)

data class MonthlyEarningsPayment(
    val id: String,
    val operationDateMillis: Long,
    val dateText: String,
    val amountText: String,
    val comment: String
)

sealed interface MonthlyEarningsResult {

    data class Success(
        val salesCount: Int
    ) : MonthlyEarningsResult

    data object ConnectionError : MonthlyEarningsResult

}

sealed interface MonthlyEarningsAccrualResult {

    data object Success : MonthlyEarningsAccrualResult

    data object AlreadyExists : MonthlyEarningsAccrualResult

    data object NeedRefresh : MonthlyEarningsAccrualResult

}
