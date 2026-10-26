package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ActiveCycleInfo
import com.example.ui.InterestSegment
import com.example.ui.LoanViewModel
import com.example.ui.MonthInterestBreakdown
import com.example.ui.ParticularDateCalculation
import com.example.ui.ParticularDateCustomState
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MoneyRed
import kotlin.math.roundToInt

@Composable
fun MonthlyInterestForecastSection(
    thisMonthForecast: MonthInterestBreakdown,
    nextMonthForecast: MonthInterestBreakdown,
    activeCycleInfo: ActiveCycleInfo,
    onSelectCycle: (String) -> Unit,
    customState: ParticularDateCustomState,
    customResult: ParticularDateCalculation,
    onSelectCustomMonth: (Int) -> Unit,
    onCustomDayChange: (Int) -> Unit,
    onCustomAmountChange: (String) -> Unit,
    onToggleIncludeTenthDay: (Boolean) -> Unit,
    onLogCustomPayment: (String) -> Unit,
    onNavigateToSimulator: () -> Unit,
    onLogSepInterest: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Cycle Banner & Manual Overrides
        CycleSelectionCard(
            activeCycleInfo = activeCycleInfo,
            onSelectCycle = onSelectCycle,
            onLogSepInterest = onLogSepInterest
        )

        // Hero Header Card
        MonthlyInterestHeroCard(
            thisMonthForecast = thisMonthForecast,
            nextMonthForecast = nextMonthForecast
        )

        // Card 1: THIS MONTH
        MonthBreakdownCard(
            forecast = thisMonthForecast,
            badgeTitle = "CURRENT CYCLE • ${thisMonthForecast.monthName.uppercase()}",
            badgeColor = MaterialTheme.colorScheme.primary,
            isThisMonth = true,
            testTagPrefix = "this_month"
        )

        // Card 2: NEXT MONTH
        MonthBreakdownCard(
            forecast = nextMonthForecast,
            badgeTitle = "NEXT CYCLE • ${nextMonthForecast.monthName.uppercase()}",
            badgeColor = MoneyGreen,
            isThisMonth = false,
            testTagPrefix = "next_month"
        )

        // Card 3: Interactive Particular Date Payment Calculator
        ParticularDatePaymentCalculatorCard(
            customState = customState,
            customResult = customResult,
            thisMonthForecast = thisMonthForecast,
            nextMonthForecast = nextMonthForecast,
            onSelectMonth = onSelectCustomMonth,
            onDayChange = onCustomDayChange,
            onAmountChange = onCustomAmountChange,
            onToggleTenth = onToggleIncludeTenthDay,
            onLogPayment = onLogCustomPayment,
            onNavigateToSimulator = onNavigateToSimulator
        )

        // Card 4: Executive Savings & Reduction Summary
        SavingsComparisonSummaryCard(
            thisMonth = thisMonthForecast,
            nextMonth = nextMonthForecast
        )
    }
}

@Composable
private fun CycleSelectionCard(
    activeCycleInfo: ActiveCycleInfo,
    onSelectCycle: (String) -> Unit,
    onLogSepInterest: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_cycle_selector"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "BILLING CYCLE VIEW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                }
                Surface(
                    color = if (activeCycleInfo.isPreviousMonthInterestLogged) MoneyGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (activeCycleInfo.isPreviousMonthInterestLogged) "${activeCycleInfo.thisMonthName.take(3).uppercase()} ACTIVE" else "${activeCycleInfo.previousMonthName.take(3).uppercase()} ACCRUING",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activeCycleInfo.isPreviousMonthInterestLogged) MoneyGreen else MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val currentCycle = activeCycleInfo.selectedCycleKey
                FilterChip(
                    selected = currentCycle == "AUTO",
                    onClick = { onSelectCycle("AUTO") },
                    label = { Text("Auto (${activeCycleInfo.thisMonthName.take(3)} & ${activeCycleInfo.nextMonthName.take(3)})", fontSize = 11.sp) },
                    modifier = Modifier.testTag("chip_cycle_auto"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = currentCycle == "SEP_OCT",
                    onClick = { onSelectCycle("SEP_OCT") },
                    label = { Text("Sep & Oct", fontSize = 11.sp) },
                    modifier = Modifier.testTag("chip_cycle_sep_oct"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = currentCycle == "OCT_NOV",
                    onClick = { onSelectCycle("OCT_NOV") },
                    label = { Text("Oct & Nov", fontSize = 11.sp) },
                    modifier = Modifier.testTag("chip_cycle_oct_nov"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = currentCycle == "NOV_DEC",
                    onClick = { onSelectCycle("NOV_DEC") },
                    label = { Text("Nov & Dec", fontSize = 11.sp) },
                    modifier = Modifier.testTag("chip_cycle_nov_dec"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Informative Context Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (activeCycleInfo.hasSepInterestLogged) MoneyGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (activeCycleInfo.hasSepInterestLogged) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (activeCycleInfo.hasSepInterestLogged) MoneyGreen else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeCycleInfo.bannerMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }

            if (!activeCycleInfo.hasSepInterestLogged && onLogSepInterest != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onLogSepInterest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_log_sep_interest_banner"),
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
                        text = "Log ${activeCycleInfo.previousMonthName} Month-End Interest (${LoanViewModel.formatCurrency(activeCycleInfo.previousMonthInterestAmount)})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyInterestHeroCard(
    thisMonthForecast: MonthInterestBreakdown,
    nextMonthForecast: MonthInterestBreakdown
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_monthly_interest_hero"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Monthly Interest Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily Reducing Balance • SBI 7.35% p.a.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = MoneyGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${thisMonthForecast.monthName.take(3)} & ${nextMonthForecast.monthName.take(3)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MoneyGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Highlight chips row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HighlightPill(
                    icon = Icons.Default.EventNote,
                    label = "10th Payoff",
                    value = LoanViewModel.formatCurrency(thisMonthForecast.tenthDayPayoffAmount),
                    modifier = Modifier.weight(1f)
                )
                HighlightPill(
                    icon = Icons.Default.ElectricBolt,
                    label = if (thisMonthForecast.hasSep27OneLakhPayoff) "27 Sep Payoff" else "Next EMI",
                    value = if (thisMonthForecast.hasSep27OneLakhPayoff) "₹1,00,000 Paid" else LoanViewModel.formatCurrency(nextMonthForecast.tenthDayPayoffAmount),
                    modifier = Modifier.weight(1f)
                )
                HighlightPill(
                    icon = Icons.Default.TrendingDown,
                    label = "Active Principal",
                    value = LoanViewModel.formatCurrency(thisMonthForecast.endingPrincipal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HighlightPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MonthBreakdownCard(
    forecast: MonthInterestBreakdown,
    badgeTitle: String,
    badgeColor: Color,
    isThisMonth: Boolean,
    testTagPrefix: String
) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_$testTagPrefix"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = badgeTitle,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                        Text(
                            text = "${forecast.totalDays} Days",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = forecast.monthName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("btn_expand_$testTagPrefix")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle segment table"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Total Highlight Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Month-End Total Interest",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = LoanViewModel.formatCurrencyDecimals(forecast.totalInterest),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (forecast.interestSaved > 0) {
                        Surface(
                            color = MoneyGreen.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = MoneyGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Saved This Cycle",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MoneyGreen
                                    )
                                }
                                Text(
                                    text = LoanViewModel.formatCurrencyDecimals(forecast.interestSaved),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MoneyGreen
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Balances Summary Ribbon with Month-End Capitalization
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Opening: ${LoanViewModel.formatCurrency(forecast.openingPrincipal)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "10th Payoff: -${LoanViewModel.formatCurrency(forecast.tenthDayPayoffAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Post-Payoffs: ${LoanViewModel.formatCurrency(forecast.endingPrincipal)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Month-End Interest (Debit): ${LoanViewModel.formatCurrencyDecimals(forecast.totalInterest)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MoneyRed
                        )
                        Text(
                            text = "Capitalized Closing: ${LoanViewModel.formatCurrencyDecimals(forecast.closingWithInterest)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        text = "Day-by-Day Segment Calculation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    forecast.segments.forEachIndexed { index, seg ->
                        SegmentItemRow(
                            segment = seg,
                            isLast = index == forecast.segments.lastIndex
                        )
                        if (index < forecast.segments.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = DividerDefaults.color.copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                val formulaText = if (forecast.segments.isNotEmpty()) {
                                    "Formula: " + forecast.segments.joinToString(" + ") {
                                        "(${it.days}d × ${LoanViewModel.formatCurrencyDecimals(it.dailyInterestAmount)})"
                                    } + " = ${LoanViewModel.formatCurrencyDecimals(forecast.totalInterest)}"
                                } else {
                                    "Total Interest: ${LoanViewModel.formatCurrencyDecimals(forecast.totalInterest)}"
                                }
                                Text(
                                    text = formulaText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Bank Capitalization: On month-end, the calculated interest of ${LoanViewModel.formatCurrencyDecimals(forecast.totalInterest)} is incurred and debited to the principal (${LoanViewModel.formatCurrency(forecast.endingPrincipal)} + ${LoanViewModel.formatCurrencyDecimals(forecast.totalInterest)} = ${LoanViewModel.formatCurrencyDecimals(forecast.closingWithInterest)}). This becomes next month's opening principal.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentItemRow(
    segment: InterestSegment,
    isLast: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = segment.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${segment.datesDesc} • Balance: ${LoanViewModel.formatCurrency(segment.principal)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (segment.eventDescription.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "• ${segment.eventDescription}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = LoanViewModel.formatCurrencyDecimals(segment.totalSegmentInterest),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${LoanViewModel.formatCurrencyDecimals(segment.dailyInterestAmount)}/day",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ParticularDatePaymentCalculatorCard(
    customState: ParticularDateCustomState,
    customResult: ParticularDateCalculation,
    thisMonthForecast: MonthInterestBreakdown,
    nextMonthForecast: MonthInterestBreakdown,
    onSelectMonth: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
    onAmountChange: (String) -> Unit,
    onToggleTenth: (Boolean) -> Unit,
    onLogPayment: (String) -> Unit,
    onNavigateToSimulator: () -> Unit
) {
    var expandedBreakdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_particular_date_calculator"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MoneyGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MoneyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Pay on Particular Date Calculator",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Include 10th-day ${LoanViewModel.formatCurrency(customResult.tenthDayAmount)} + custom payment on any day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Month Selector Tabs
            Text(
                text = "1. Select Target Month",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = customState.selectedMonthOption == 0,
                    onClick = { onSelectMonth(0) },
                    label = { Text("${thisMonthForecast.monthName.substringBefore(" ")} (This Month)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_month_this"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                FilterChip(
                    selected = customState.selectedMonthOption == 1,
                    onClick = { onSelectMonth(1) },
                    label = { Text("${nextMonthForecast.monthName.substringBefore(" ")} (Next Month)") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chip_month_next"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Day of Month Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "2. Day of Payment in Month",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Day ${customState.dayOfMonth}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val targetDays = if (customState.selectedMonthOption == 0) thisMonthForecast.totalDays else nextMonthForecast.totalDays
            Slider(
                value = customState.dayOfMonth.coerceIn(1, targetDays).toFloat(),
                onValueChange = { onDayChange(it.roundToInt().coerceIn(1, targetDays)) },
                valueRange = 1f..targetDays.toFloat(),
                steps = kotlin.math.max(0, targetDays - 2),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("slider_custom_day"),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            // Quick Day Preset Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 10, 15, 18, 25, 27).forEach { day ->
                    val isSelected = customState.dayOfMonth == day
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                        onClick = { onDayChange(day) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${day}th",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Payment Amount Input
            Text(
                text = "3. Payment Amount (₹)",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = customState.amountInput,
                onValueChange = onAmountChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_custom_pay_amount"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
            // Quick Amount Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("21500", "50000", "100000", "200000").forEach { amt ->
                    val label = when (amt) {
                        "21500" -> "₹21.5k"
                        "50000" -> "₹50k"
                        "100000" -> "₹1 Lakh"
                        else -> "₹2 Lakh"
                    }
                    val isSelected = customState.amountInput == amt
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MoneyGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .background(
                                if (isSelected) MoneyGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                        onClick = { onAmountChange(amt) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MoneyGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Always Include 10th Day Payoff Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Always Include 10th-Day Payoff",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Regular ₹21,500 automatically paid on 10th",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = customState.includeTenthDayPayoff,
                    onCheckedChange = onToggleTenth,
                    modifier = Modifier.testTag("switch_include_tenth_day"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Result Display Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MoneyGreen.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MoneyGreen.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Forecasted Month Interest",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = LoanViewModel.formatCurrencyDecimals(customResult.newTotalInterest),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MoneyGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Month Interest Saved",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = LoanViewModel.formatCurrencyDecimals(customResult.interestSavedInMonth),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MoneyGreen
                            )
                            Text(
                                text = "Saves ₹${(customResult.permanentDailySaving * 100).roundToInt() / 100.0}/day permanently",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Resulting Ending Principal: ${LoanViewModel.formatCurrency(customResult.endingPrincipal)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { expandedBreakdown = !expandedBreakdown },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (expandedBreakdown) "Hide Segment Proof" else "Show Segment Proof (${customResult.segments.size} parts)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = {
                                val monthName = if (customState.selectedMonthOption == 0) thisMonthForecast.monthName else nextMonthForecast.monthName
                                val dayStr = if (customState.dayOfMonth < 10) "0${customState.dayOfMonth}" else "${customState.dayOfMonth}"
                                onLogPayment("Payoff on $dayStr $monthName")
                            },
                            modifier = Modifier.testTag("btn_log_custom_payment"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log to Ledger", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(visible = expandedBreakdown) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            customResult.segments.forEachIndexed { i, seg ->
                                SegmentItemRow(segment = seg, isLast = i == customResult.segments.lastIndex)
                                if (i < customResult.segments.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        color = DividerDefaults.color.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsComparisonSummaryCard(
    thisMonth: MonthInterestBreakdown,
    nextMonth: MonthInterestBreakdown
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_savings_comparison"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MoneyGreen,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Executive Payoff & Interest Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            SummaryComparisonRow(
                label = "Before 27 Sep Prepayment",
                monthlyInterest = "₹7,109 / mo",
                principal = "₹1,154,003",
                isMuted = true
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            SummaryComparisonRow(
                label = "This Month (Sep with 1L Payoff)",
                monthlyInterest = "₹6,934",
                principal = "₹1,054,003",
                isMuted = false
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            SummaryComparisonRow(
                label = "Next Month (Oct with 10th Payoff)",
                monthlyInterest = "₹6,484 / mo",
                principal = "₹1,032,503",
                isMuted = false,
                isHighlight = true
            )

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                color = MoneyGreen.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Annual Interest Saved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MoneyGreen
                    )
                    Text(
                        text = "₹7,490 / year permanently",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MoneyGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryComparisonRow(
    label: String,
    monthlyInterest: String,
    principal: String,
    isMuted: Boolean,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Ending Principal: $principal",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = monthlyInterest,
            fontSize = 13.sp,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isHighlight) MoneyGreen else if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace
        )
    }
}
