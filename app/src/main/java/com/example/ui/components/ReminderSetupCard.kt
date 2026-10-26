package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderSettings
import com.example.reminder.ReminderType
import com.example.ui.LoanViewModel
import com.example.ui.ReminderStatusInfo
import com.example.ui.theme.MoneyBlue
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed
import kotlinx.coroutines.delay

@Composable
fun ReminderSetupCard(
    reminderSettings: ReminderSettings,
    reminderStatus: ReminderStatusInfo,
    onUpdateEmiReminder: (enabled: Boolean, day: Int, hour: Int, minute: Int) -> Unit,
    onUpdateInterestReminder: (enabled: Boolean, day: Int, hour: Int, minute: Int) -> Unit,
    onTriggerTestReminder: (ReminderType) -> Unit,
    onLogEmiPayment: () -> Unit,
    onLogPreviousMonthInterest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(3500)
            feedbackMessage = null
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_reminder_setup"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Reminders",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Reminders & Automation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "10th Day (EMI Paid Check) • 1st Day (Previous Month Interest)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notification permission banner if needed
            if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MoneyGold.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Enable notifications",
                                tint = MoneyGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enable notifications to receive monthly alerts on the 10th and 1st.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MoneyGold,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("btn_grant_notification_permission")
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Feedback banner
            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                feedbackMessage?.let { msg ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MoneyGreen.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = MoneyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // SECTION 1: 10th Day EMI Payment Check Reminder
            // -------------------------------------------------------------
            EmiReminderCard(
                reminderSettings = reminderSettings,
                reminderStatus = reminderStatus,
                onUpdate = onUpdateEmiReminder,
                onTest = {
                    onTriggerTestReminder(ReminderType.EMI_PAYMENT_DUE)
                    feedbackMessage = "Sent 10th-Day EMI test notification!"
                },
                onLogEmi = {
                    onLogEmiPayment()
                    feedbackMessage = "Logged scheduled EMI of ${LoanViewModel.formatCurrency(reminderStatus.emiExpectedAmount)} in ledger!"
                }
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp
            )

            // -------------------------------------------------------------
            // SECTION 2: 1st Day Previous Month Interest Log Reminder
            // -------------------------------------------------------------
            InterestReminderCard(
                reminderSettings = reminderSettings,
                reminderStatus = reminderStatus,
                onUpdate = onUpdateInterestReminder,
                onTest = {
                    onTriggerTestReminder(ReminderType.MONTH_END_INTEREST_LOG)
                    feedbackMessage = "Sent 1st-Day Interest Log test notification!"
                },
                onLogInterest = {
                    onLogPreviousMonthInterest()
                    feedbackMessage = "Logged ${reminderStatus.previousMonthLabel} interest (${LoanViewModel.formatCurrency(reminderStatus.pendingInterestAmount)}) in ledger!"
                }
            )
        }
    }
}

@Composable
private fun EmiReminderCard(
    reminderSettings: ReminderSettings,
    reminderStatus: ReminderStatusInfo,
    onUpdate: (enabled: Boolean, day: Int, hour: Int, minute: Int) -> Unit,
    onTest: () -> Unit,
    onLogEmi: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("section_emi_reminder"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MoneyBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Payment,
                        contentDescription = "EMI reminder",
                        tint = MoneyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "10th Day: Monthly EMI Reminder",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Checks if EMI (₹${LoanViewModel.formatCurrency(reminderStatus.emiExpectedAmount)}) was paid or pending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = reminderSettings.emiReminderEnabled,
                onCheckedChange = { isChecked ->
                    onUpdate(
                        isChecked,
                        reminderSettings.emiReminderDay,
                        reminderSettings.emiReminderHour,
                        reminderSettings.emiReminderMinute
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("switch_emi_reminder")
            )
        }

        // Live Status Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_emi_payment"),
            shape = RoundedCornerShape(12.dp),
            color = if (reminderStatus.isEmiPaidThisMonth) {
                MoneyGreen.copy(alpha = 0.12f)
            } else {
                MoneyGold.copy(alpha = 0.12f)
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (reminderStatus.isEmiPaidThisMonth) MoneyGreen.copy(alpha = 0.4f) else MoneyGold.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (reminderStatus.isEmiPaidThisMonth) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (reminderStatus.isEmiPaidThisMonth) MoneyGreen else MoneyGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (reminderStatus.isEmiPaidThisMonth) {
                                "EMI PAID (${reminderStatus.currentMonthLabel})"
                            } else {
                                "EMI PENDING (${reminderStatus.currentMonthLabel})"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (reminderStatus.isEmiPaidThisMonth) MoneyGreen else MoneyGold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Due Day: ${reminderSettings.emiReminderDay}th",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (reminderStatus.isEmiPaidThisMonth) {
                        "✓ ${LoanViewModel.formatCurrency(reminderStatus.emiPaidAmount ?: reminderStatus.emiExpectedAmount)} was paid on ${reminderStatus.emiPaidDate ?: "the 10th"}. Ledger is up to date."
                    } else {
                        "Scheduled payoff of ${LoanViewModel.formatCurrency(reminderStatus.emiExpectedAmount)} is due on the ${reminderSettings.emiReminderDay}th of ${reminderStatus.currentMonthLabel}. Has this been debited?"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Quick Action: If pending, offer 1-click log
                if (!reminderStatus.isEmiPaidThisMonth) {
                    Button(
                        onClick = onLogEmi,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_log_emi_paid_quick"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mark / Log ₹${LoanViewModel.formatCurrency(reminderStatus.emiExpectedAmount)} Paid",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Time and Day Options (if enabled)
        if (reminderSettings.emiReminderEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Reminder Schedule & Time",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(5, 7, 10, 15).forEach { day ->
                        FilterChip(
                            selected = reminderSettings.emiReminderDay == day,
                            onClick = {
                                onUpdate(
                                    true,
                                    day,
                                    reminderSettings.emiReminderHour,
                                    reminderSettings.emiReminderMinute
                                )
                            },
                            label = { Text("${day}th Day", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MoneyBlue.copy(alpha = 0.2f),
                                selectedLabelColor = MoneyBlue
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(9, "09:00 AM"),
                        Pair(12, "12:00 PM"),
                        Pair(18, "06:00 PM"),
                        Pair(20, "08:00 PM")
                    ).forEach { (hr, label) ->
                        FilterChip(
                            selected = reminderSettings.emiReminderHour == hr,
                            onClick = {
                                onUpdate(
                                    true,
                                    reminderSettings.emiReminderDay,
                                    hr,
                                    0
                                )
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // Test Notification Action
        OutlinedButton(
            onClick = onTest,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_test_emi_reminder"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Send Test 10th Day EMI Notification",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun InterestReminderCard(
    reminderSettings: ReminderSettings,
    reminderStatus: ReminderStatusInfo,
    onUpdate: (enabled: Boolean, day: Int, hour: Int, minute: Int) -> Unit,
    onTest: () -> Unit,
    onLogInterest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("section_interest_reminder"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MoneyGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = "Interest reminder",
                        tint = MoneyGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "1st Day: Log Previous Month Interest",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Prompts you on the 1st to record the bank interest debit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = reminderSettings.interestReminderEnabled,
                onCheckedChange = { isChecked ->
                    onUpdate(
                        isChecked,
                        reminderSettings.interestReminderDay,
                        reminderSettings.interestReminderHour,
                        reminderSettings.interestReminderMinute
                    )
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("switch_interest_reminder")
            )
        }

        // Live Status Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_interest_logged"),
            shape = RoundedCornerShape(12.dp),
            color = if (reminderStatus.isInterestLoggedForPreviousMonth) {
                MoneyGreen.copy(alpha = 0.12f)
            } else {
                MoneyRed.copy(alpha = 0.12f)
            },
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (reminderStatus.isInterestLoggedForPreviousMonth) MoneyGreen.copy(alpha = 0.4f) else MoneyRed.copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (reminderStatus.isInterestLoggedForPreviousMonth) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (reminderStatus.isInterestLoggedForPreviousMonth) MoneyGreen else MoneyRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (reminderStatus.isInterestLoggedForPreviousMonth) {
                                "${reminderStatus.previousMonthLabel.uppercase()} INTEREST LOGGED"
                            } else {
                                "LOG ${reminderStatus.previousMonthLabel.uppercase()} INTEREST"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (reminderStatus.isInterestLoggedForPreviousMonth) MoneyGreen else MoneyRed
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Log Day: ${reminderSettings.interestReminderDay}st",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = if (reminderStatus.isInterestLoggedForPreviousMonth) {
                        "✓ ${reminderStatus.previousMonthLabel} interest of ${LoanViewModel.formatCurrency(reminderStatus.interestLoggedAmount ?: reminderStatus.pendingInterestAmount)} is already logged in the ledger."
                    } else {
                        "Bank debits interest on the 1st for ${reminderStatus.previousMonthLabel}. Expected amount: ~${LoanViewModel.formatCurrency(reminderStatus.pendingInterestAmount)}. Record this to advance your cycle."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Quick Action: If pending, offer 1-click log
                if (!reminderStatus.isInterestLoggedForPreviousMonth) {
                    Button(
                        onClick = onLogInterest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_log_interest_quick"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MoneyRed
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Log ${reminderStatus.previousMonthLabel} Interest (${LoanViewModel.formatCurrency(reminderStatus.pendingInterestAmount)})",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Time and Day Options (if enabled)
        if (reminderSettings.interestReminderEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Reminder Schedule & Time",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { day ->
                        FilterChip(
                            selected = reminderSettings.interestReminderDay == day,
                            onClick = {
                                onUpdate(
                                    true,
                                    day,
                                    reminderSettings.interestReminderHour,
                                    reminderSettings.interestReminderMinute
                                )
                            },
                            label = { Text("${day}st/th Day", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MoneyGold.copy(alpha = 0.2f),
                                selectedLabelColor = MoneyGold
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair(9, "09:00 AM"),
                        Pair(12, "12:00 PM"),
                        Pair(18, "06:00 PM"),
                        Pair(20, "08:00 PM")
                    ).forEach { (hr, label) ->
                        FilterChip(
                            selected = reminderSettings.interestReminderHour == hr,
                            onClick = {
                                onUpdate(
                                    true,
                                    reminderSettings.interestReminderDay,
                                    hr,
                                    0
                                )
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // Test Notification Action
        OutlinedButton(
            onClick = onTest,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_test_interest_reminder"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Send Test 1st Day Interest Notification",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
