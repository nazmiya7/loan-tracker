package com.example.reminder

import android.content.Context
import android.content.SharedPreferences

data class ReminderSettings(
    val emiReminderEnabled: Boolean = true,
    val emiReminderDay: Int = 10,
    val emiReminderHour: Int = 9,
    val emiReminderMinute: Int = 0,
    val interestReminderEnabled: Boolean = true,
    val interestReminderDay: Int = 1,
    val interestReminderHour: Int = 9,
    val interestReminderMinute: Int = 0
)

class ReminderPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("loan_reminders_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): ReminderSettings {
        return ReminderSettings(
            emiReminderEnabled = prefs.getBoolean(KEY_EMI_ENABLED, true),
            emiReminderDay = prefs.getInt(KEY_EMI_DAY, 10),
            emiReminderHour = prefs.getInt(KEY_EMI_HOUR, 9),
            emiReminderMinute = prefs.getInt(KEY_EMI_MINUTE, 0),
            interestReminderEnabled = prefs.getBoolean(KEY_INTEREST_ENABLED, true),
            interestReminderDay = prefs.getInt(KEY_INTEREST_DAY, 1),
            interestReminderHour = prefs.getInt(KEY_INTEREST_HOUR, 9),
            interestReminderMinute = prefs.getInt(KEY_INTEREST_MINUTE, 0)
        )
    }

    fun saveSettings(settings: ReminderSettings) {
        prefs.edit()
            .putBoolean(KEY_EMI_ENABLED, settings.emiReminderEnabled)
            .putInt(KEY_EMI_DAY, settings.emiReminderDay)
            .putInt(KEY_EMI_HOUR, settings.emiReminderHour)
            .putInt(KEY_EMI_MINUTE, settings.emiReminderMinute)
            .putBoolean(KEY_INTEREST_ENABLED, settings.interestReminderEnabled)
            .putInt(KEY_INTEREST_DAY, settings.interestReminderDay)
            .putInt(KEY_INTEREST_HOUR, settings.interestReminderHour)
            .putInt(KEY_INTEREST_MINUTE, settings.interestReminderMinute)
            .apply()
    }

    companion object {
        private const val KEY_EMI_ENABLED = "emi_reminder_enabled"
        private const val KEY_EMI_DAY = "emi_reminder_day"
        private const val KEY_EMI_HOUR = "emi_reminder_hour"
        private const val KEY_EMI_MINUTE = "emi_reminder_minute"
        private const val KEY_INTEREST_ENABLED = "interest_reminder_enabled"
        private const val KEY_INTEREST_DAY = "interest_reminder_day"
        private const val KEY_INTEREST_HOUR = "interest_reminder_hour"
        private const val KEY_INTEREST_MINUTE = "interest_reminder_minute"
    }
}
