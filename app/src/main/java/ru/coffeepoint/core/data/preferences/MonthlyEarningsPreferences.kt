package ru.coffeepoint.core.data.preferences

import android.content.Context

class MonthlyEarningsPreferences(
    context: Context
) {

    private val preferences = context.getSharedPreferences(
        "monthly_earnings",
        Context.MODE_PRIVATE
    )

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

    fun updateWorkStartDateText(value: String) {
        preferences.edit()
            .putString(KEY_WORK_START_DATE_TEXT, value)
            .apply()
    }

    private companion object {

        const val KEY_SETTLEMENT_AMOUNT = "settlement_amount"
        const val KEY_WORK_START_DATE_TEXT = "work_start_date_text"

    }

}
