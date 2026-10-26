package com.example.data

import android.content.Context
import android.content.SharedPreferences

data class LoanSettings(
    val monthlyEmiAmount: Double = 21500.0,
    val annualRatePercent: Double = 7.35,
    val emiDueDayOfMonth: Int = 10,
    val previousEmiAmount: Double = 21500.0,
    val effectiveFromNextMonth: Boolean = true
)

class LoanPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("loan_valuation_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MONTHLY_EMI = "key_monthly_emi"
        private const val KEY_ANNUAL_RATE = "key_annual_rate"
        private const val KEY_EMI_DUE_DAY = "key_emi_due_day"
        private const val KEY_PREVIOUS_EMI = "key_previous_emi"
        private const val KEY_EFFECTIVE_FROM_NEXT_MONTH = "key_effective_from_next_month"

        const val DEFAULT_EMI = 21500.0
        const val DEFAULT_RATE = 7.35
        const val DEFAULT_DUE_DAY = 10
        const val DEFAULT_PREVIOUS_EMI = 21500.0
        const val DEFAULT_EFFECTIVE_NEXT_MONTH = true
    }

    fun loadSettings(): LoanSettings {
        val emi = prefs.getFloat(KEY_MONTHLY_EMI, DEFAULT_EMI.toFloat()).toDouble()
        val rate = prefs.getFloat(KEY_ANNUAL_RATE, DEFAULT_RATE.toFloat()).toDouble()
        val day = prefs.getInt(KEY_EMI_DUE_DAY, DEFAULT_DUE_DAY)
        val prevEmi = prefs.getFloat(KEY_PREVIOUS_EMI, DEFAULT_PREVIOUS_EMI.toFloat()).toDouble()
        val effectiveNext = prefs.getBoolean(KEY_EFFECTIVE_FROM_NEXT_MONTH, DEFAULT_EFFECTIVE_NEXT_MONTH)

        return LoanSettings(
            monthlyEmiAmount = if (emi > 0.0) emi else DEFAULT_EMI,
            annualRatePercent = if (rate > 0.0) rate else DEFAULT_RATE,
            emiDueDayOfMonth = if (day in 1..28) day else DEFAULT_DUE_DAY,
            previousEmiAmount = if (prevEmi > 0.0) prevEmi else DEFAULT_PREVIOUS_EMI,
            effectiveFromNextMonth = effectiveNext
        )
    }

    fun saveSettings(settings: LoanSettings) {
        prefs.edit()
            .putFloat(KEY_MONTHLY_EMI, settings.monthlyEmiAmount.toFloat())
            .putFloat(KEY_ANNUAL_RATE, settings.annualRatePercent.toFloat())
            .putInt(KEY_EMI_DUE_DAY, settings.emiDueDayOfMonth)
            .putFloat(KEY_PREVIOUS_EMI, settings.previousEmiAmount.toFloat())
            .putBoolean(KEY_EFFECTIVE_FROM_NEXT_MONTH, settings.effectiveFromNextMonth)
            .apply()
    }

    fun resetToDefaults(): LoanSettings {
        prefs.edit().clear().apply()
        return LoanSettings()
    }
}

