package com.example.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.LoanDatabase
import com.example.data.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

enum class ReminderType {
    EMI_PAYMENT_DUE,
    MONTH_END_INTEREST_LOG
}

class LoanReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED) {
            LoanReminderScheduler.scheduleAllReminders(context)
            return
        }

        val typeName = intent.getStringExtra(EXTRA_REMINDER_TYPE) ?: return
        val reminderType = try {
            ReminderType.valueOf(typeName)
        } catch (_: Exception) {
            return
        }

        val isTest = intent.getBooleanExtra(EXTRA_IS_TEST, false)

        if (!isTest) {
            // Reschedule future reminders since setAndAllowWhileIdle is a single-shot alarm
            LoanReminderScheduler.scheduleAllReminders(context)
        }

        CoroutineScope(Dispatchers.IO).launch {
            handleReminder(context, reminderType, isTest)
        }
    }

    private suspend fun handleReminder(context: Context, type: ReminderType, isTest: Boolean) {
        ensureNotificationChannel(context)

        val db = LoanDatabase.getDatabase(context, CoroutineScope(Dispatchers.IO))
        val txList = db.transactionDao().getAllTransactionsAsc().firstOrNull() ?: emptyList()

        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) // 0-indexed (8=Sep, 9=Oct, 10=Nov)
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val fullMonthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        val currentMonthName = fullMonthNames[currentMonth]
        val currentMonthShort = monthNames[currentMonth]
        val prevMonthIndex = if (currentMonth == 0) 11 else currentMonth - 1
        val prevMonthName = fullMonthNames[prevMonthIndex]
        val prevMonthShort = monthNames[prevMonthIndex]

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("TARGET_TAB", if (type == ReminderType.MONTH_END_INTEREST_LOG) 1 else 0)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            type.ordinal,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        when (type) {
            ReminderType.EMI_PAYMENT_DUE -> {
                // 10th Day of Month: Check if EMI payment (e.g. around 21,500) has been recorded for this month
                val hasEmiPaid = txList.any { tx ->
                    tx.type == TransactionType.PAYMENT &&
                    (tx.date.contains(currentMonthShort, ignoreCase = true) || tx.note.contains(currentMonthShort, ignoreCase = true)) &&
                    (tx.amount >= 20000.0)
                }

                val title = if (hasEmiPaid) {
                    "✓ Monthly EMI Already Paid ($currentMonthName)"
                } else {
                    "⚠️ Monthly EMI Due Today (10th $currentMonthName)"
                }

                val message = if (hasEmiPaid) {
                    "Your scheduled EMI for $currentMonthName is already logged in the ledger."
                } else {
                    "Scheduled ₹21,500 EMI is due today. Tap to check balance or log payment."
                }

                showNotification(context, NOTIFICATION_ID_EMI, title, message, pendingIntent)
            }

            ReminderType.MONTH_END_INTEREST_LOG -> {
                // 1st Day of Month: Check if previous month's interest has been recorded in the ledger
                val hasInterestLogged = txList.any { tx ->
                    tx.type == TransactionType.INTEREST_CHARGED &&
                    (tx.note.contains(prevMonthShort, ignoreCase = true) ||
                     tx.date.contains(prevMonthShort, ignoreCase = true) ||
                     (tx.date.contains("01 $currentMonthShort", ignoreCase = true) && !tx.note.contains("$currentMonthShort interest", ignoreCase = true)))
                }

                val title = if (hasInterestLogged) {
                    "✓ $prevMonthName Interest Recorded"
                } else {
                    "📌 1st of Month: Log $prevMonthName Interest"
                }

                val message = if (hasInterestLogged) {
                    "Interest debit for $prevMonthName is already recorded in your loan ledger."
                } else {
                    "Bank interest for $prevMonthName has been debited. Tap to record the debit and advance your cycle."
                }

                showNotification(context, NOTIFICATION_ID_INTEREST, title, message, pendingIntent)
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        pendingIntent: PendingIntent
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Handled when notification permission is not granted yet
        }
    }

    companion object {
        const val CHANNEL_ID = "loan_reminders_channel"
        const val EXTRA_REMINDER_TYPE = "EXTRA_REMINDER_TYPE"
        const val EXTRA_IS_TEST = "EXTRA_IS_TEST"

        const val NOTIFICATION_ID_EMI = 1010
        const val NOTIFICATION_ID_INTEREST = 1001

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Loan & Interest Reminders"
                val descriptionText = "Reminders for 10th-day EMI payments and 1st-day interest logging"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
}
