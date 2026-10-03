package ru.coffeepoint.core.data.preferences

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonthlyEarningsPreferences(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        "monthly_earnings",
        Context.MODE_PRIVATE
    )

    fun save(
        salesCount: Int,
        paymentsAmount: Int,
        settlementAmount: Int,
        workStartDateText: String,
        periodFrom: String,
        periodTo: String,
        timeMillis: Long
    ) {

        preferences.edit()
            .putInt(KEY_SALES_COUNT, salesCount)
            .putInt(KEY_PAYMENTS_AMOUNT, paymentsAmount)
            .putInt(KEY_SETTLEMENT_AMOUNT, settlementAmount)
            .putString(KEY_WORK_START_DATE_TEXT, workStartDateText)
            .putString(KEY_PERIOD_FROM, periodFrom)
            .putString(KEY_PERIOD_TO, periodTo)
            .putLong(KEY_LAST_REFRESH_TIME, timeMillis)
            .apply()

    }

    fun getPaymentsAmount(): Int {

        return preferences.getInt(
            KEY_PAYMENTS_AMOUNT,
            0
        )

    }

    fun updatePaymentsAmount(
        paymentsAmount: Int
    ) {

        preferences.edit()
            .putInt(KEY_PAYMENTS_AMOUNT, paymentsAmount)
            .apply()

    }

    fun updateSettlementAmount(
        settlementAmount: Int
    ) {

        preferences.edit()
            .putInt(KEY_SETTLEMENT_AMOUNT, settlementAmount)
            .apply()

    }

    fun getSettlementAmount(): Int {

        return preferences.getInt(
            KEY_SETTLEMENT_AMOUNT,
            0
        )

    }

    fun getWorkStartDateText(): String? {

        return preferences.getString(
            KEY_WORK_START_DATE_TEXT,
            null
        )

    }

    fun getPeriodFrom(): String? {

        return preferences.getString(
            KEY_PERIOD_FROM,
            null
        )

    }

    fun getPeriodTo(): String? {

        return preferences.getString(
            KEY_PERIOD_TO,
            null
        )

    }

    fun getSalesCount(): Int {

        return preferences.getInt(
            KEY_SALES_COUNT,
            0
        )

    }

    fun getLastRefreshText(): String? {

        val timeMillis = preferences.getLong(
            KEY_LAST_REFRESH_TIME,
            0L
        )

        if (timeMillis == 0L) return null

        val formatter = SimpleDateFormat(
            "dd.MM.yyyy HH:mm",
            Locale.getDefault()
        )

        return formatter.format(Date(timeMillis))

    }

    private companion object {

        const val KEY_SALES_COUNT = "sales_count"
        const val KEY_PAYMENTS_AMOUNT = "payments_amount"
        const val KEY_SETTLEMENT_AMOUNT = "settlement_amount"
        const val KEY_WORK_START_DATE_TEXT = "work_start_date_text"
        const val KEY_PERIOD_FROM = "period_from"
        const val KEY_PERIOD_TO = "period_to"
        const val KEY_LAST_REFRESH_TIME = "last_refresh_time"

    }

}
