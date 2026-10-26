package com.example

import java.io.File
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LoanTransaction
import com.example.ui.LoanViewModel
import com.example.ui.components.AddTransactionDialog
import com.example.ui.components.AuditReconciliationSection
import com.example.ui.components.DashboardSection
import com.example.ui.components.InterestTrendChartSection
import com.example.ui.components.LedgerSection
import com.example.ui.components.MonthlyInterestForecastSection
import com.example.ui.components.SettingsSection
import com.example.ui.components.TailwindWebView
import com.example.ui.components.WhatIfSimulatorSection
import com.example.ui.theme.MoneyGreen
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.material.icons.filled.Psychology
import com.example.ui.components.AiAdvisorSection

data class AppTab(
    val title: String,
    val shortName: String,
    val icon: ImageVector,
    val testTag: String
)

val APP_TABS = listOf(
    AppTab("Dashboard", "Dashboard", Icons.Default.Dashboard, "tab_dashboard"),
    AppTab("Monthly Interest", "Interest", Icons.Default.CalendarMonth, "tab_monthly_interest"),
    AppTab("Payoff Simulator", "Simulator", Icons.Default.Calculate, "tab_simulator"),
    AppTab("Audit & Proof", "Audit", Icons.Default.Verified, "tab_audit"),
    AppTab("Trend Charts", "Charts", Icons.AutoMirrored.Filled.ShowChart, "tab_charts"),
    AppTab("Ledger", "Ledger", Icons.AutoMirrored.Filled.ReceiptLong, "tab_ledger"),
    AppTab("Executive Report", "Report", Icons.Default.Language, "tab_tailwind_web"),
    AppTab("AI Advisor", "AI Chat", Icons.Default.Psychology, "tab_ai_advisor"),
    AppTab("Settings", "Settings", Icons.Default.Settings, "tab_settings")
)


class MainActivity : ComponentActivity() {
    private val viewModel: LoanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val targetTab = intent.getIntExtra("TARGET_TAB", -1)
        if (targetTab >= 0) {
            viewModel.setTab(targetTab)
        }
        setContent {
            MyApplicationTheme {
                LoanAppScreen(viewModel = viewModel, initialTargetTab = targetTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val targetTab = intent.getIntExtra("TARGET_TAB", -1)
        if (targetTab >= 0) {
            viewModel.setTab(targetTab)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanAppScreen(viewModel: LoanViewModel, initialTargetTab: Int = -1) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val dashboardSummary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val simulatorState by viewModel.simulatorState.collectAsStateWithLifecycle()
    val simulatorResult by viewModel.simulatorResult.collectAsStateWithLifecycle()
    val thisMonthForecast by viewModel.thisMonthForecast.collectAsStateWithLifecycle()
    val nextMonthForecast by viewModel.nextMonthForecast.collectAsStateWithLifecycle()
    val particularDateState by viewModel.particularDateState.collectAsStateWithLifecycle()
    val particularDateResult by viewModel.particularDateResult.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val selectedTabFromVm by viewModel.selectedTab.collectAsStateWithLifecycle()
    val loanSettings by viewModel.loanSettings.collectAsStateWithLifecycle()
    val activeCycleInfo by viewModel.activeCycleInfo.collectAsStateWithLifecycle()
    val reminderSettings by viewModel.reminderSettings.collectAsStateWithLifecycle()
    val reminderStatusInfo by viewModel.reminderStatusInfo.collectAsStateWithLifecycle()

    // Local synchronous state for 100% reliable tab switching without animation cancel loops
    var activeTabIndex by rememberSaveable { mutableIntStateOf(if (initialTargetTab >= 0) initialTargetTab.coerceIn(0, APP_TABS.lastIndex) else 0) }

    LaunchedEffect(initialTargetTab) {
        if (initialTargetTab >= 0) {
            activeTabIndex = initialTargetTab.coerceIn(0, APP_TABS.lastIndex)
        }
    }

    // Runtime notification permission check for Android 13+
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission response handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun navigateToTab(targetIndex: Int) {
        val bounded = targetIndex.coerceIn(0, APP_TABS.lastIndex)
        activeTabIndex = bounded
        viewModel.setTab(bounded)
    }

    // Android back handler: returns to previous tab
    BackHandler(enabled = activeTabIndex > 0) {
        navigateToTab(activeTabIndex - 1)
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<LoanTransaction?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (activeTabIndex > 0) {
                        IconButton(
                            onClick = { navigateToTab(activeTabIndex - 1) },
                            modifier = Modifier.testTag("btn_top_prev")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Section"
                            )
                        }
                    }
                },
                title = {
                    Text(
                        text = APP_TABS[activeTabIndex].title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navigateToTab(7) },
                        modifier = Modifier.testTag("btn_top_ai_advisor")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Ask AI Loan Advisor"
                        )
                    }
                    IconButton(
                        onClick = { navigateToTab(8) },
                        modifier = Modifier.testTag("btn_top_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Loan & Valuation Settings"
                        )
                    }
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("btn_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Formula Details"
                        )
                    }
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.testTag("btn_reset_data")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to Original Ledger"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bar_bottom_navigation"),
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp
            ) {
                APP_TABS.forEachIndexed { index, tab ->
                    val isSelected = activeTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { navigateToTab(index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.shortName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.shortName.lowercase()}")
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTabIndex != 6 && activeTabIndex != 7 && activeTabIndex != 8) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Log Transaction", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_add_transaction"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Modern Scrollable Tab Bar
            ScrollableTabRow(
                selectedTabIndex = activeTabIndex,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTabIndex]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                }
            ) {
                APP_TABS.forEachIndexed { index, tab ->
                    val isSelected = activeTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { navigateToTab(index) },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        text = {
                            Text(
                                text = if (index == 5) "${tab.title} (${transactions.size})" else tab.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }

            // Tab Content with Direct Rendering (Zero animation lockups, instantaneous responsiveness)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (activeTabIndex) {
                    0 -> {
                        // Feature 1: Live Loan Dashboard
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                DashboardSection(
                                    summary = dashboardSummary,
                                    reminderStatus = reminderStatusInfo,
                                    onUseInSimulator = { amount ->
                                        viewModel.applyCurrentOutstandingToSimulator(amount)
                                        navigateToTab(2)
                                    },
                                    onLogTransaction = { showAddDialog = true },
                                    onLogEmiPaid = { viewModel.logCurrentEmiPayment() },
                                    onLogPreviousMonthInterest = { viewModel.logPreviousMonthInterest() },
                                    onConfigureSettings = { navigateToTab(8) },
                                    onAskAiAdvisor = { navigateToTab(7) }
                                )
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Dashboard",
                                    nextTabName = "Monthly Interest",
                                    nextTabDesc = "Detailed interest breakdown for Sep & Oct with 10th & 27th payoffs",
                                    onNextClick = { navigateToTab(1) }
                                )
                            }
                        }
                    }
                    1 -> {
                        // Feature 2: Dedicated Monthly Interest Breakdown (This & Next Month)
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                MonthlyInterestForecastSection(
                                    thisMonthForecast = thisMonthForecast,
                                    nextMonthForecast = nextMonthForecast,
                                    activeCycleInfo = activeCycleInfo,
                                    onSelectCycle = { viewModel.selectBillingCycle(it) },
                                    customState = particularDateState,
                                    customResult = particularDateResult,
                                    onSelectCustomMonth = { viewModel.updateParticularDateMonth(it) },
                                    onCustomDayChange = { viewModel.updateParticularDateDay(it) },
                                    onCustomAmountChange = { viewModel.updateParticularDateAmount(it) },
                                    onToggleIncludeTenthDay = { viewModel.toggleParticularDateTenthDay(it) },
                                    onLogCustomPayment = { note -> viewModel.logCustomParticularDatePayment(note) },
                                    onNavigateToSimulator = { navigateToTab(2) },
                                    onLogSepInterest = { viewModel.logPreviousMonthInterest() }
                                )
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Monthly Interest",
                                    nextTabName = "Payoff Simulator",
                                    nextTabDesc = "Calculate daily reducing interest & part-payment scenarios",
                                    onNextClick = { navigateToTab(2) }
                                )
                            }
                        }
                    }
                    2 -> {
                        // Feature 3: Interactive Payoff & Prepayment Simulator
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                WhatIfSimulatorSection(
                                    state = simulatorState,
                                    result = simulatorResult,
                                    onPrincipalChange = { viewModel.updatePrincipalInput(it) },
                                    onRateChange = { viewModel.updateAnnualRate(it) },
                                    onExtraPaymentChange = { viewModel.updateExtraPaymentInput(it) },
                                    onDaysChange = { viewModel.updateDaysInMonth(it) },
                                    onRegularEmiChange = { viewModel.updateRegularMonthlyPayment(it) },
                                    onPlannedDaysOffsetChange = { viewModel.updatePlannedDaysOffset(it) },
                                    onCustomPlannedDateChange = { date, days -> viewModel.updatePlannedDate(date, days) },
                                    onSyncCurrentBalance = { viewModel.syncSimulatorWithLatestBalance() },
                                    latestLedgerBalance = dashboardSummary.currentOutstandingPrincipal
                                )
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Payoff Simulator",
                                    nextTabName = "Audit & Proof",
                                    nextTabDesc = "Examine cycle-by-cycle exact reconciliation against SBI statements",
                                    onNextClick = { navigateToTab(3) }
                                )
                            }
                        }
                    }
                    3 -> {
                        // Feature 4: Mathematical Audit & Bank Reconciliation
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                AuditReconciliationSection()
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Audit & Proof",
                                    nextTabName = "Trend Charts",
                                    nextTabDesc = "Visualize monthly interest drops and balance compression curves",
                                    onNextClick = { navigateToTab(4) }
                                )
                            }
                        }
                    }
                    4 -> {
                        // Feature 5: Interactive Trend Charts & Analytics
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                InterestTrendChartSection(transactions = transactions)
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Trend Charts",
                                    nextTabName = "Transaction Ledger",
                                    nextTabDesc = "Review and log repayment records and interest charges",
                                    onNextClick = { navigateToTab(5) }
                                )
                            }
                        }
                    }
                    5 -> {
                        // Feature 6: Chronological Transaction Ledger
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                LedgerSection(
                                    transactions = transactions,
                                    currentFilter = filter,
                                    onFilterChange = { viewModel.setFilter(it) },
                                    onAddClick = { showAddDialog = true },
                                    onDeleteClick = { tx -> transactionToDelete = tx }
                                )
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Transaction Ledger",
                                    nextTabName = "Executive Report",
                                    nextTabDesc = "View the responsive executive summary & feature breakdown",
                                    onNextClick = { navigateToTab(6) }
                                )
                            }
                        }
                    }
                    6 -> {
                        // Feature 7: Executive Financial Report & Overview
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                TailwindWebView(
                                    onNavigateToNativeTab = { targetTab -> navigateToTab(targetTab) },
                                    loanViewModel = viewModel
                                )
                            }
                            NextTabNavigationCard(
                                currentTab = "Executive Report",
                                nextTabName = "AI Loan Advisor",
                                nextTabDesc = "Ask doubts with your live balance & daily reducing math",
                                onNextClick = { navigateToTab(7) }
                            )
                        }
                    }
                    7 -> {
                        // Feature 8: AI Loan Advisor & Financial Chat
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                AiAdvisorSection(
                                    viewModel = viewModel,
                                    dashboardSummary = dashboardSummary,
                                    loanSettings = loanSettings,
                                    thisMonthForecast = thisMonthForecast
                                )
                            }
                            NextTabNavigationCard(
                                currentTab = "AI Loan Advisor",
                                nextTabName = "Valuation & EMI Settings",
                                nextTabDesc = "Configure regular monthly EMI, annual rate, and due dates",
                                onNextClick = { navigateToTab(8) }
                            )
                        }
                    }
                    8 -> {
                        // Feature 9: Loan Valuation & EMI Settings
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                        ) {
                            item {
                                SettingsSection(
                                    currentSettings = loanSettings,
                                    dashboardSummary = dashboardSummary,
                                    reminderSettings = reminderSettings,
                                    reminderStatus = reminderStatusInfo,
                                    onUpdateEmiReminder = { en, d, h, m -> viewModel.updateEmiReminder(en, d, h, m) },
                                    onUpdateInterestReminder = { en, d, h, m -> viewModel.updateInterestReminder(en, d, h, m) },
                                    onTriggerTestReminder = { type -> viewModel.triggerTestReminder(type) },
                                    onLogEmiPayment = { viewModel.logCurrentEmiPayment() },
                                    onLogPreviousMonthInterest = { viewModel.logPreviousMonthInterest() },
                                    onSaveSettings = { emi, rate, dueDay, effectiveFromNextMonth, previousEmi ->
                                        viewModel.updateLoanSettings(emi, rate, dueDay, effectiveFromNextMonth, previousEmi)
                                    },
                                    onResetSettings = {
                                        viewModel.resetSettingsToDefault()
                                    },
                                    onNavigateToDashboard = {
                                        navigateToTab(0)
                                    }
                                )
                            }
                            item {
                                NextTabNavigationCard(
                                    currentTab = "Valuation & EMI Settings",
                                    nextTabName = "Live Loan Dashboard",
                                    nextTabDesc = "Inspect updated payoff timeline, months, and debt-free target",
                                    onNextClick = { navigateToTab(0) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            currentBalance = dashboardSummary.currentOutstandingPrincipal,
            defaultEmiAmount = loanSettings.monthlyEmiAmount,
            onDismiss = { showAddDialog = false },
            onConfirm = { date, type, amount, note ->
                viewModel.addTransaction(date, type, amount, note)
                showAddDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction") },
            text = {
                Text("Are you sure you want to remove the record of ${LoanViewModel.formatCurrency(tx.amount)} on ${tx.date}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Initial Ledger") },
            text = {
                Text("This will restore the original pre-loaded transactions from June 30 to September 30. Any custom transactions will be removed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetToDefault()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset Ledger", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Formula & Logic Info Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("SBI Daily Reducing Interest Logic") },
            text = {
                Column {
                    Text(
                        text = "Interest is calculated on the daily closing principal balance rather than the opening month balance:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "\nFormula:\nDaily Interest = Principal × (Annual Rate / 365)\nMonthly Interest = Principal × (Rate / 365) × Days",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "\nBenefits of Mid-Month Prepayments:\nAny part-payment immediately lowers the daily closing balance, reducing subsequent daily interest charges for all remaining days in that month.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
fun NextTabNavigationCard(
    currentTab: String,
    nextTabName: String,
    nextTabDesc: String,
    onNextClick: () -> Unit
) {
    Card(
        onClick = onNextClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("card_next_tab"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NEXT SECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = nextTabName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = nextTabDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Go to $nextTabName",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
