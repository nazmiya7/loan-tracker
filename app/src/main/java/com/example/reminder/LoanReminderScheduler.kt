package com.example.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object LoanReminderScheduler {

    fun scheduleAllReminders(context: Context) {
        val prefs = ReminderPreferences(context)
        val settings = prefs.loadSettings()
        LoanReminderReceiver.ensureNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 1. Schedule 10th Day EMI Reminder
        if (settings.emiReminderEnabled) {
            scheduleMonthlyAlarm(
                context = context,
                alarmManager = alarmManager,
                dayOfMonth = settings.emiReminderDay,
                hour = settings.emiReminderHour,
                minute = settings.emiReminderMinute,
                type = ReminderType.EMI_PAYMENT_DUE,
                requestCode = 1010
            )
        } else {
            cancelAlarm(context, alarmManager, ReminderType.EMI_PAYMENT_DUE, 1010)
        }

        // 2. Schedule 1st Day Interest Log Reminder
        if (settings.interestReminderEnabled) {
            scheduleMonthlyAlarm(
                context = context,
                alarmManager = alarmManager,
                dayOfMonth = settings.interestReminderDay,
                hour = settings.interestReminderHour,
                minute = settings.interestReminderMinute,
                type = ReminderType.MONTH_END_INTEREST_LOG,
                requestCode = 1001
            )
        } else {
            cancelAlarm(context, alarmManager, ReminderType.MONTH_END_INTEREST_LOG, 1001)
        }
    }

    private fun scheduleMonthlyAlarm(
        context: Context,
        alarmManager: AlarmManager,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        type: ReminderType,
        requestCode: Int
    ) {
        val intent = Intent(context, LoanReminderReceiver::class.java).apply {
            putExtra(LoanReminderReceiver.EXTRA_REMINDER_TYPE, type.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the time has already passed this month, schedule for next month
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // In case exact alarm permission is restricted
        }
    }

    private fun cancelAlarm(
        context: Context,
        alarmManager: AlarmManager,
        type: ReminderType,
        requestCode: Int
    ) {
        val intent = Intent(context, LoanReminderReceiver::class.java).apply {
            putExtra(LoanReminderReceiver.EXTRA_REMINDER_TYPE, type.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )
        alarmManager.cancel(pendingIntent)
    }

    fun triggerImmediateTest(context: Context, type: ReminderType) {
        val intent = Intent(context, LoanReminderReceiver::class.java).apply {
            putExtra(LoanReminderReceiver.EXTRA_REMINDER_TYPE, type.name)
            putExtra(LoanReminderReceiver.EXTRA_IS_TEST, true)
        }
        context.sendBroadcast(intent)
    }
}
