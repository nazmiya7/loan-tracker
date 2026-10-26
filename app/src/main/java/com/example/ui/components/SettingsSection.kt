package com.example.ui.components

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LoanSettings
import com.example.ui.DashboardSummary
import com.example.ui.LoanViewModel
import com.example.reminder.ReminderSettings
import com.example.reminder.ReminderType
import com.example.ui.ReminderStatusInfo
import com.example.ui.theme.MoneyBlue
import com.example.ui.theme.MoneyGold
import com.example.ui.theme.MoneyGreen
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun SettingsSection(
    currentSettings: LoanSettings,
    dashboardSummary: DashboardSummary,
    reminderSettings: ReminderSettings? = null,
    reminderStatus: ReminderStatusInfo? = null,
    onUpdateEmiReminder: ((enabled: Boolean, day: Int, hour: Int, minute: Int) -> Unit)? = null,
    onUpdateInterestReminder: ((enabled: Boolean, day: Int, hour: Int, minute: Int) -> Unit)? = null,
    onTriggerTestReminder: ((ReminderType) -> Unit)? = null,
    onLogEmiPayment: (() -> Unit)? = null,
    onLogPreviousMonthInterest: (() -> Unit)? = null,
    onSaveSettings: (emi: Double, rate: Double, dueDay: Int, effectiveFromNextMonth: Boolean, previousEmi: Double) -> Unit,
    onResetSettings: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    var emiInput by remember(currentSettings.monthlyEmiAmount) {
        mutableStateOf(currentSettings.monthlyEmiAmount.toLong().toString())
    }
    var rateInput by remember(currentSettings.annualRatePercent) {
        mutableStateOf(currentSettings.annualRatePercent.toString())
    }
    var selectedDueDay by remember(currentSettings.emiDueDayOfMonth) {
        mutableStateOf(currentSettings.emiDueDayOfMonth)
    }
    var effectiveFromNextMonth by remember(currentSettings.effectiveFromNextMonth) {
        mutableStateOf(currentSettings.effectiveFromNextMonth)
    }
    var previousEmiInput by remember(currentSettings.previousEmiAmount) {
        mutableStateOf(currentSettings.previousEmiAmount.toLong().toString())
    }
    var showSavedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(showSavedBanner) {
        if (showSavedBanner) {
            delay(3500)
            showSavedBanner = false
        }
    }

    val parsedEmi = emiInput.toDoubleOrNull() ?: currentSettings.monthlyEmiAmount
    val parsedRate = rateInput.toDoubleOrNull() ?: currentSettings.annualRatePercent
    val parsedPrevEmi = previousEmiInput.toDoubleOrNull() ?: currentSettings.previousEmiAmount
    val currentBalance = dashboardSummary.currentOutstandingPrincipal

    // Real-time calculation of new tenure based on the input values
    val livePayoffPreview by remember(parsedEmi, parsedRate, parsedPrevEmi, currentBalance) {
        derivedStateOf {
            val monthlyRate = (parsedRate / 100.0) / 12.0
            val minMonthlyInterest = currentBalance * monthlyRate
            val baselineMonths = if (parsedPrevEmi > minMonthlyInterest && currentBalance > 0) {
                val numBase = -ln(1.0 - (currentBalance * monthlyRate) / parsedPrevEmi) / ln(1.0 + monthlyRate)
                numBase.roundToInt()
            } else 59

            if (parsedEmi > minMonthlyInterest && currentBalance > 0) {
                val numMonthsDouble = -ln(1.0 - (currentBalance * monthlyRate) / parsedEmi) / ln(1.0 + monthlyRate)
                val months = numMonthsDouble.roundToInt()
                val yrs = months / 12
                val remMos = months % 12
                val yearsMosText = when {
                    yrs == 0 -> "$remMos months"
                    remMos == 0 -> "$yrs years"
                    else -> "$yrs yrs $remMos mos"
                }
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, months)
                val targetDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
                val monthsSavedVsBaseline = max(0, baselineMonths - months)
                LivePreviewResult(
                    isValid = true,
                    months = months,
                    yearsMonthsText = yearsMosText,
                    targetDate = targetDate,
                    monthsSaved = monthsSavedVsBaseline,
                    baselineMonths = baselineMonths,
                    errorMessage = null
                )
            } else if (currentBalance <= 0) {
                LivePreviewResult(
                    isValid = true,
                    months = 0,
                    yearsMonthsText = "0 months",
                    targetDate = "Debt-Free!",
                    monthsSaved = 0,
                    baselineMonths = baselineMonths,
                    errorMessage = null
                )
            } else {
                LivePreviewResult(
                    isValid = false,
                    months = 999,
                    yearsMonthsText = "Indefinite",
                    targetDate = "N/A",
                    monthsSaved = 0,
                    baselineMonths = baselineMonths,
                    errorMessage = "EMI must exceed monthly interest of ${LoanViewModel.formatCurrency(minMonthlyInterest)}"
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_hero"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Valuation & EMI Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Future Repayment & Amortization Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Live Sync",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "If you change your EMI amount or interest rate in the future, updating it here immediately recalculates all valuations across the Live Dashboard, payoff timeline, monthly forecasts, and debt-free target date.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Monthly Loan Reminders Setup (10th Day EMI Payoff Check & 1st Day Previous Month Interest Log)
        if (reminderSettings != null && reminderStatus != null) {
            ReminderSetupCard(
                reminderSettings = reminderSettings,
                reminderStatus = reminderStatus,
                onUpdateEmiReminder = { en, d, h, m -> onUpdateEmiReminder?.invoke(en, d, h, m) },
                onUpdateInterestReminder = { en, d, h, m -> onUpdateInterestReminder?.invoke(en, d, h, m) },
                onTriggerTestReminder = { type -> onTriggerTestReminder?.invoke(type) },
                onLogEmiPayment = { onLogEmiPayment?.invoke() },
                onLogPreviousMonthInterest = { onLogPreviousMonthInterest?.invoke() }
            )
        }

        // Animated Saved Banner
        AnimatedVisibility(
            visible = showSavedBanner,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_settings_saved"),
                shape = RoundedCornerShape(12.dp),
                color = MoneyGreen.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MoneyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Settings Applied Successfully!",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MoneyGreen
                        )
                        val curMonth = reminderStatus?.currentMonthLabel?.split(" ")?.firstOrNull() ?: "October"
                        val prevMonth = reminderStatus?.previousMonthLabel?.split(" ")?.firstOrNull() ?: "September"
                        Text(
                            text = if (effectiveFromNextMonth) {
                                "New EMI of ${LoanViewModel.formatCurrency(parsedEmi)} scheduled starting next month ($curMonth). $prevMonth & prior calculations remain based on ${LoanViewModel.formatCurrency(parsedPrevEmi)}."
                            } else {
                                "All dashboard payoff timelines and monthly valuation models have been updated to ${LoanViewModel.formatCurrency(parsedEmi)}/month."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Valuation Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_active_valuation_summary"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Current Active Valuation Profile",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = if (currentSettings.effectiveFromNextMonth) MoneyGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (currentSettings.effectiveFromNextMonth) "Starts Next Month (Oct)" else "Active Immediately",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSettings.effectiveFromNextMonth) MoneyGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Item 1: Active Future EMI
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Future Scheduled EMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LoanViewModel.formatCurrency(currentSettings.monthlyEmiAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Profile Item 2: Previous EMI Baseline
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "This & Past Month EMI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = LoanViewModel.formatCurrency(currentSettings.previousEmiAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Profile Item 3: Rate
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Interest Rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${currentSettings.annualRatePercent}% p.a.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MoneyBlue,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Profile Item 4: Scheduled Day
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "EMI Due Day",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${currentSettings.emiDueDayOfMonth}th of month",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MoneyGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Current Live Dashboard Result
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Live Dashboard Result:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dashboardSummary.estimatedPayoffMonths} Months (${dashboardSummary.estimatedPayoffYearsMonthsText}) • Target ${dashboardSummary.estimatedPayoffCompletionDateText}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Section 0: EMI Effective Timing & Transition Policy
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_effective_policy"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "EMI Effective Timing Policy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "When changing the EMI, decide if the new EMI should take effect starting from next month or immediately.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = effectiveFromNextMonth,
                        onClick = { effectiveFromNextMonth = true },
                        label = {
                            Text(
                                text = "From Next Month (Oct 2026)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_effective_next_month"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    FilterChip(
                        selected = !effectiveFromNextMonth,
                        onClick = { effectiveFromNextMonth = false },
                        label = {
                            Text(
                                text = "Effective Immediately (Sep 2026)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chip_effective_immediate"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Explanatory breakdown box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (effectiveFromNextMonth) MoneyGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (effectiveFromNextMonth) Icons.Default.CheckCircle else Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (effectiveFromNextMonth) MoneyGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (effectiveFromNextMonth) {
                                    "Effective Next Month Active (Recommended)"
                                } else {
                                    "Immediate Application Mode"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (effectiveFromNextMonth) MoneyGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val explanationCurMonth = reminderStatus?.currentMonthLabel?.split(" ")?.firstOrNull() ?: "October"
                        val explanationPrevMonth = reminderStatus?.previousMonthLabel?.split(" ")?.firstOrNull() ?: "September"
                        Text(
                            text = if (effectiveFromNextMonth) {
                                "• Current Cycle ($explanationPrevMonth) & Prior: Preserved and calculated on your previous EMI of ${LoanViewModel.formatCurrency(parsedPrevEmi)}.\n• Next Cycle ($explanationCurMonth) & Future: Automatically modeled using your new EMI of ${LoanViewModel.formatCurrency(parsedEmi)}."
                            } else {
                                "• All periods (both current $explanationPrevMonth cycle and future) will calculate using your new EMI of ${LoanViewModel.formatCurrency(parsedEmi)}."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // Section 1: Monthly EMI Amount Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_emi_input"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CurrencyRupee,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Future Scheduled Monthly EMI Amount",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your planned regular monthly installment amount. Changing this updates future payoff tenures and month-end closing balances.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emiInput,
                    onValueChange = { emiInput = it },
                    label = { Text("Monthly EMI Amount (₹)") },
                    placeholder = { Text("e.g. 21500") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_settings_emi"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    prefix = {
                        Text(
                            text = "₹ ",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Presets
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val emiPresets = listOf(
                    "21500" to "₹21.5k (Base)",
                    "25000" to "₹25k",
                    "30000" to "₹30k",
                    "35000" to "₹35k",
                    "40000" to "₹40k",
                    "50000" to "₹50k"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emiPresets.take(3).forEach { (presetVal, label) ->
                        val isSelected = emiInput == presetVal
                        FilterChip(
                            selected = isSelected,
                            onClick = { emiInput = presetVal },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_emi_$presetVal"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    emiPresets.drop(3).forEach { (presetVal, label) ->
                        val isSelected = emiInput == presetVal
                        FilterChip(
                            selected = isSelected,
                            onClick = { emiInput = presetVal },
                            label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_emi_$presetVal"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real-Time Valuation Impact Preview Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("surface_live_emi_impact"),
                    color = if (livePayoffPreview.isValid) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = if (livePayoffPreview.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Live Valuation Impact on Balance (${LoanViewModel.formatCurrency(currentBalance)}):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (livePayoffPreview.isValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (livePayoffPreview.isValid) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "New Estimated Tenure:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${livePayoffPreview.yearsMonthsText} (${livePayoffPreview.months} Mos)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Target Debt-Free Date:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = livePayoffPreview.targetDate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (livePayoffPreview.monthsSaved > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = MoneyGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Saves ${livePayoffPreview.monthsSaved} months sooner vs previous ₹21.5k EMI baseline!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MoneyGreen
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = livePayoffPreview.errorMessage ?: "Invalid EMI input",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Annual Interest Rate Configuration
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_rate_input"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Percent,
                        contentDescription = null,
                        tint = MoneyBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Annual Interest Rate (% p.a.)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "State Bank of India (SBI) floating benchmark rate. Update if the repo rate changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Annual Rate (%)") },
                    placeholder = { Text("7.35") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_settings_rate"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    suffix = {
                        Text(
                            text = "% p.a.",
                            fontWeight = FontWeight.Bold,
                            color = MoneyBlue
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                val ratePresets = listOf("7.10", "7.35", "7.65", "8.00", "8.50")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ratePresets.forEach { presetRate ->
                        val isSelected = rateInput == presetRate
                        FilterChip(
                            selected = isSelected,
                            onClick = { rateInput = presetRate },
                            label = { Text("$presetRate%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_rate_$presetRate"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MoneyBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Section 3: Scheduled EMI Repayment Day
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_day_input"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MoneyGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Monthly EMI Auto-Debit Day",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "The calendar day each month your loan EMI is debited. Interest calculations split the month into segments before and after this day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                val dayOptions = listOf(1, 5, 10, 15, 20, 25)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    dayOptions.forEach { day ->
                        val isSelected = selectedDueDay == day
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedDueDay = day },
                            label = {
                                Text(
                                    text = if (day == 10) "10th (SBI)" else "${day}th",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("chip_day_$day"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Section 4: Valuation Impact Breakdown Guide
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("card_settings_guide"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "How Changing EMI Affects Valuations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                ValuationGuideItem(
                    title = "1. Live Loan Dashboard",
                    description = "Recalculates total remaining months, years/months display, and projected debt-free completion date using the exact daily-reducing amortization model."
                )
                ValuationGuideItem(
                    title = "2. Monthly Interest Forecasts",
                    description = "October and upcoming forecast models automatically apply the new EMI amount on the scheduled debit day, giving accurate cycle interest savings."
                )
                ValuationGuideItem(
                    title = "3. Particular Date Custom Calculator",
                    description = "Baseline interest comparison seamlessly adopts the new regular installment."
                )
                ValuationGuideItem(
                    title = "4. Persistent Across App Restarts",
                    description = "Your customized EMI is stored locally so all future valuations reflect your exact financial plan."
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onResetSettings()
                    emiInput = "21500"
                    rateInput = "7.35"
                    selectedDueDay = 10
                    effectiveFromNextMonth = true
                    previousEmiInput = "21500"
                    showSavedBanner = true
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("btn_reset_settings"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Defaults", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    if (livePayoffPreview.isValid) {
                        onSaveSettings(parsedEmi, parsedRate, selectedDueDay, effectiveFromNextMonth, parsedPrevEmi)
                        showSavedBanner = true
                    }
                },
                enabled = livePayoffPreview.isValid,
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("btn_save_settings"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Apply & Save EMI", fontWeight = FontWeight.Bold)
            }
        }

        // Quick Jump to Dashboard
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ready to inspect updated numbers?",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "View live timeline on the dashboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onNavigateToDashboard,
                    modifier = Modifier.testTag("btn_settings_goto_dashboard"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Go to Dashboard", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ValuationGuideItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

private data class LivePreviewResult(
    val isValid: Boolean,
    val months: Int,
    val yearsMonthsText: String,
    val targetDate: String,
    val monthsSaved: Int,
    val baselineMonths: Int = 59,
    val errorMessage: String?
)
