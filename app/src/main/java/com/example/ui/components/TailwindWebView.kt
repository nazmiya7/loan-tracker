package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.LoanViewModel
import com.example.ui.theme.MoneyGreen

data class TailwindFeatureItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val targetTabIndex: Int
)

val TAILWIND_FEATURES = listOf(
    TailwindFeatureItem("all", "All Features", Icons.Default.Assessment, 0),
    TailwindFeatureItem("dashboard", "Dashboard", Icons.Default.Dashboard, 0),
    TailwindFeatureItem("interest", "Monthly Interest", Icons.Default.TrendingDown, 1),
    TailwindFeatureItem("simulator", "Payoff Simulator", Icons.Default.Calculate, 2),
    TailwindFeatureItem("audit", "Audit & Proof", Icons.Default.CheckCircle, 3),
    TailwindFeatureItem("charts", "Trend Charts", Icons.Default.ShowChart, 4),
    TailwindFeatureItem("ledger", "Ledger History", Icons.Default.ListAlt, 5),
    TailwindFeatureItem("ai_advisor", "AI Advisor", Icons.Default.Psychology, 7)
)

// Tailwind Dark Theme color palette constants
private val Slate950 = Color(0xFF020617)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate400 = Color(0xFF94A3B8)
private val Sky500 = Color(0xFF0EA5E9)
private val Sky400 = Color(0xFF38BDF8)
private val Emerald500 = Color(0xFF10B981)

/**
 * Tailwind-inspired executive financial report and summary dashboard.
 * Built 100% in native Jetpack Compose for zero crashes, maximum performance,
 * and reliable execution across all cloud emulator and physical devices.
 */
@Composable
fun TailwindWebView(
    modifier: Modifier = Modifier,
    initialFeature: String = "all",
    onNavigateToNativeTab: ((Int) -> Unit)? = null,
    loanViewModel: LoanViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedFeature by remember { mutableStateOf(initialFeature) }

    val dashboardSummary by loanViewModel.dashboardSummary.collectAsStateWithLifecycle()
    val thisMonthForecast by loanViewModel.thisMonthForecast.collectAsStateWithLifecycle()
    val transactions by loanViewModel.transactions.collectAsStateWithLifecycle()
    val loanSettings by loanViewModel.loanSettings.collectAsStateWithLifecycle()

    val currentPrincipal = dashboardSummary.currentOutstandingPrincipal
    val annualRate = loanSettings.annualRatePercent
    val dailyRatePercent = annualRate / 365.0
    val dailyInterestToday = (currentPrincipal * (annualRate / 100.0)) / 365.0
    val monthlyEmi = loanSettings.monthlyEmiAmount
    val estimatedThisMonthInterest = thisMonthForecast.totalInterest
    val daysInCurrentMonth = thisMonthForecast.totalDays

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        // Feature Filter Header Chips
        Surface(
            color = Slate900,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Feature View:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    modifier = Modifier.padding(end = 8.dp)
                )

                TAILWIND_FEATURES.forEach { feature ->
                    val isSelected = selectedFeature == feature.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFeature = feature.id },
                        label = {
                            Text(
                                text = feature.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = feature.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("chip_feature_${feature.id}"),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Slate800,
                            labelColor = Slate400,
                            iconColor = Slate400,
                            selectedContainerColor = Sky500,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) Sky400 else Slate700
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Top Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                    shape = RoundedCornerShape(10.dp),
                                    color = Sky500,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "SBI",
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "SBI Daily-Reducing Loan",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$annualRate% p.a. • Daily Balance Accounting",
                                        fontSize = 11.sp,
                                        color = Sky400
                                    )
                                }
                            }

                            // Share Action
                            OutlinedButton(
                                onClick = {
                                    val summaryText = buildString {
                                        appendLine("=== SBI Loan Tracker Summary ===")
                                        appendLine("Principal Balance: ${LoanViewModel.formatCurrency(currentPrincipal)}")
                                        appendLine("Interest Rate: $annualRate% p.a.")
                                        appendLine("Estimated Monthly Interest: ${LoanViewModel.formatCurrency(estimatedThisMonthInterest)}")
                                        appendLine("Regular Monthly EMI: ${LoanViewModel.formatCurrency(monthlyEmi)}")
                                        appendLine("Total Transactions: ${transactions.size}")
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, summaryText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Loan Summary")
                                    context.startActivity(shareIntent)
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Sky400),
                                border = BorderStroke(1.dp, Slate700)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Report",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share", fontSize = 12.sp)
                            }
                        }

                        Text(
                            text = "Daily Balance Accounting • Scheduled 10th EMI • What-If Payoff Planner",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }
            }

            // Key Financial Metrics Grid
            item {
                Text(
                    text = "EXECUTIVE SUMMARY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "CURRENT PRINCIPAL",
                        amount = LoanViewModel.formatCurrency(currentPrincipal),
                        subtext = "Live Reducing Balance",
                        accentColor = Sky400,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "EST. THIS MONTH",
                        amount = LoanViewModel.formatCurrency(estimatedThisMonthInterest),
                        subtext = "$daysInCurrentMonth Days In Month",
                        accentColor = Emerald500,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        title = "DAILY INTEREST",
                        amount = LoanViewModel.formatCurrency(dailyInterestToday),
                        subtext = "${String.format("%.4f", dailyRatePercent)}% per day",
                        accentColor = MoneyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "MONTHLY EMI",
                        amount = LoanViewModel.formatCurrency(monthlyEmi),
                        subtext = "Due 10th of Month",
                        accentColor = Sky400,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Daily-Reducing Formula Explanation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Sky400,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "How SBI Daily-Reducing Interest Works",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "1. Interest is calculated every single day on the exact end-of-day balance:\n" +
                                    "   Daily Interest = (Principal × $annualRate%) ÷ 365 days\n" +
                                    "2. When your ${LoanViewModel.formatCurrency(monthlyEmi)} EMI is debited on the 10th, the balance drops immediately on the 10th.\n" +
                                    "3. Any lump-sum prepayment reduces your daily interest calculation starting the very next morning.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = Slate400,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Interactive Navigation Quick Cards
            if (selectedFeature == "all" || selectedFeature == "simulator") {
                item {
                    FeatureNavigationCard(
                        title = "Payoff & What-If Simulator",
                        description = "Test lump sum prepayments (e.g. ₹1,00,000 on Oct 15) and calculate exact interest savings.",
                        icon = Icons.Default.Calculate,
                        accentColor = Emerald500,
                        actionLabel = "Open Simulator Tab",
                        onClick = { onNavigateToNativeTab?.invoke(2) }
                    )
                }
            }

            if (selectedFeature == "all" || selectedFeature == "audit") {
                item {
                    FeatureNavigationCard(
                        title = "Bank vs App Audit Proof",
                        description = "Daily segmented audit breakdown comparing bank statement records with day-by-day interest.",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Sky400,
                        actionLabel = "Open Audit Tab",
                        onClick = { onNavigateToNativeTab?.invoke(3) }
                    )
                }
            }

            if (selectedFeature == "all" || selectedFeature == "charts") {
                item {
                    FeatureNavigationCard(
                        title = "Trend Charts & Visualizer",
                        description = "Visual representation of principal balance trajectory and cumulative interest savings over time.",
                        icon = Icons.Default.ShowChart,
                        accentColor = Color(0xFFA855F7),
                        actionLabel = "Open Trend Charts",
                        onClick = { onNavigateToNativeTab?.invoke(4) }
                    )
                }
            }

            if (selectedFeature == "all" || selectedFeature == "ledger") {
                item {
                    FeatureNavigationCard(
                        title = "Chronological Transaction Ledger",
                        description = "View, filter, add, and audit all EMI repayments, lump-sum credits, and interest debits.",
                        icon = Icons.Default.ReceiptLong,
                        accentColor = Color(0xFFF59E0B),
                        actionLabel = "Open Ledger",
                        onClick = { onNavigateToNativeTab?.invoke(5) }
                    )
                }
            }

            if (selectedFeature == "all" || selectedFeature == "dashboard") {
                item {
                    FeatureNavigationCard(
                        title = "Interactive KPI Dashboard",
                        description = "Live overview with quick-action transaction modals, next due date alerts, and valuation metrics.",
                        icon = Icons.Default.Dashboard,
                        accentColor = Sky400,
                        actionLabel = "Open Dashboard",
                        onClick = { onNavigateToNativeTab?.invoke(0) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    amount: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Slate400,
                letterSpacing = 0.5.sp
            )
            Text(
                text = amount,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = Slate400
            )
        }
    }
}

@Composable
private fun FeatureNavigationCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Slate400,
                        lineHeight = 16.sp
                    )
                }
            }

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Slate800,
                    contentColor = accentColor
                )
            ) {
                Text(
                    text = actionLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
