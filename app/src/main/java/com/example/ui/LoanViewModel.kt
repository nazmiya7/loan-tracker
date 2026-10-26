package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LoanDatabase
import com.example.data.LoanPreferences
import com.example.data.LoanRepository
import com.example.data.LoanSettings
import com.example.data.LoanTransaction
import com.example.data.TransactionType
import com.example.data.ChatMessage
import com.example.data.ChatSender
import com.example.data.LoanLiveContext
import com.example.data.AiAdvisorService
import com.example.data.LocalLoanExpertEngine
import com.example.reminder.LoanReminderScheduler
import com.example.reminder.ReminderPreferences
import com.example.reminder.ReminderSettings
import com.example.reminder.ReminderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

data class SimulatorState(
    val principalInput: String = "1017937",
    val annualRatePercent: Double = 7.35,
    val extraPaymentInput: String = "100000",
    val daysInMonth: Int = 31,
    val regularMonthlyPaymentInput: String = "21500",
    val plannedDateText: String = "15 Oct 2026",
    val plannedDaysOffset: Int = 20
)

data class InterestSegment(
    val title: String,
    val datesDesc: String,
    val days: Int,
    val principal: Double,
    val dailyRatePercent: Double,
    val dailyInterestAmount: Double,
    val totalSegmentInterest: Double,
    val eventDescription: String = ""
)

data class MonthInterestBreakdown(
    val monthName: String,
    val totalDays: Int,
    val openingPrincipal: Double,
    val endingPrincipal: Double,
    val totalInterest: Double,
    val baselineInterestWithoutPrepayment: Double,
    val interestSaved: Double,
    val segments: List<InterestSegment>,
    val tenthDayPayoffIncluded: Boolean = true,
    val tenthDayPayoffAmount: Double = 21500.0,
    val additionalPayoffsSummary: String = "",
    val hasSep27OneLakhPayoff: Boolean = false,
    val closingWithInterest: Double = endingPrincipal + totalInterest
)

data class ActiveCycleInfo(
    val activeCycleKey: String = "SEP_OCT",
    val selectedCycleKey: String = "AUTO",
    val thisMonthName: String = "September 2026",
    val nextMonthName: String = "October 2026",
    val previousMonthName: String = "September",
    val isAutoAdvanced: Boolean = false,
    val hasSepInterestLogged: Boolean = false,
    val sepInterestAmount: Double = 0.0,
    val previousMonthInterestAmount: Double = 6928.90,
    val isPreviousMonthInterestLogged: Boolean = false,
    val bannerMessage: String = ""
)

data class ReminderStatusInfo(
    val isEmiPaidThisMonth: Boolean = false,
    val emiPaidDate: String? = null,
    val emiPaidAmount: Double? = null,
    val emiDueDay: Int = 10,
    val emiExpectedAmount: Double = 21500.0,
    val isInterestLoggedForPreviousMonth: Boolean = false,
    val interestLoggedDate: String? = null,
    val interestLoggedAmount: Double? = null,
    val pendingInterestAmount: Double = 6928.90,
    val currentMonthLabel: String = "October 2026",
    val previousMonthLabel: String = "September 2026",
    val nextEmiDueDateLabel: String = "10th of every month",
    val nextInterestLogDateLabel: String = "1st of every month"
)

data class ParticularDateCustomState(
    val selectedMonthOption: Int = 1, // 0: This Month (Sep), 1: Next Month (Oct)
    val dayOfMonth: Int = 18,
    val amountInput: String = "100000",
    val includeTenthDayPayoff: Boolean = true,
    val tenthDayPayoffAmount: Double = 21500.0
)

data class ParticularDateCalculation(
    val targetMonthName: String,
    val totalDays: Int,
    val basePrincipal: Double,
    val payDateDay: Int,
    val payAmount: Double,
    val includeTenthDay: Boolean,
    val tenthDayAmount: Double,
    val newTotalInterest: Double,
    val baselineInterest: Double,
    val interestSavedInMonth: Double,
    val permanentDailySaving: Double,
    val endingPrincipal: Double,
    val segments: List<InterestSegment>
)

data class SimulatorResult(
    val currentPrincipal: Double,
    val extraPayment: Double,
    val newPrincipal: Double,
    val dailyInterestCurrent: Double,
    val dailyInterestAfterPrepayment: Double,
    val dailyInterestSaved: Double,
    val plannedDateText: String,
    val daysUntilPlannedDate: Int,
    val accruedInterestTillPlannedDate: Double,
    val settlementPayoffAmountOnPlannedDate: Double,
    // Month Names (dynamically adapt based on active month / cycle)
    val currentMonthName: String = "September",
    val nextMonthName: String = "October",
    val currentMonthShort: String = "Sep",
    val nextMonthShort: String = "Oct",
    val previousMonthName: String = "August",
    // Output 1: Current Month Interest
    val currentMonthInterestBaseline: Double,
    val currentMonthInterestWithPrepayment: Double,
    val currentMonthInterestSaved: Double,
    val currentMonthWithLastPayoff: Double = 6928.90,
    val currentMonthBeforeLastPayoff: Double = 7014.75,
    val hasSep27PayoffIncluded: Boolean = true,
    val sep27PayoffAmount: Double = 100000.0,
    val sep27SavedInMonth: Double = 85.85,
    // Output 2: Next Month Interest (with 10th-day EMI of ₹21,500 added from Day 10 onwards)
    val nextMonthInterestBaseline: Double,
    val nextMonthInterestWithPrepayment: Double,
    val nextMonthInterestSaved: Double,
    val nextMonthPart1Days: Int,
    val nextMonthPart1Interest: Double,
    val nextMonthPart2Days: Int,
    val nextMonthPart2InterestBaseline: Double,
    val nextMonthPart2InterestPrepaid: Double,
    val nextMonthBalanceAfterEmi: Double,
    val nextMonthBalanceAfterEmiAndPrepay: Double,
    val oldNextMonthBaselineWithoutSep27: Double = 7108.56,
    val recurringMonthlySavingFromLastPayoff: Double = 624.14,
    // Timeline & Amortization
    val regularMonthsToPayoff: Int,
    val regularYearsMonthsText: String,
    val regularPayoffDateText: String,
    val prepaidMonthsToPayoff: Int,
    val prepaidYearsMonthsText: String,
    val prepaidPayoffDateText: String,
    val monthsSaved: Int,
    val totalLifetimeInterestSaved: Double,
    val isRegularPayoffPossible: Boolean,
    val requiredMonthlyToPayoffByPlannedDate: Double,
    val nextMonthStartingPrincipal: Double = 0.0
)

data class DashboardSummary(
    val currentOutstandingPrincipal: Double,
    val lastRecordedInterestCharged: Double,
    val estimatedAnnualRate: Double,
    val totalPaymentsMade: Double,
    val totalInterestCharged: Double,
    val initialPrincipal: Double,
    val principalReductionPercent: Double,
    val normalMonthlyPayoff: Double = 21500.0,
    val estimatedPayoffMonths: Int = 59,
    val estimatedPayoffYearsMonthsText: String = "4 yrs 11 mos",
    val estimatedPayoffCompletionDateText: String = "Aug 2029",
    val initialTenureMonths: Int = 100,
    val tenureMonthsSavedSoFar: Int = 41,
    val previousMonthlyPayoff: Double = 21500.0,
    val effectiveFromNextMonth: Boolean = true,
    val baselinePayoffMonths: Int = 59,
    val monthsSavedByNewEmi: Int = 0
)

enum class LedgerFilter {
    ALL,
    PAYMENTS,
    INTEREST
}

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LoanRepository
    val transactions: StateFlow<List<LoanTransaction>>

    private val _filter = MutableStateFlow(LedgerFilter.ALL)
    val filter: StateFlow<LedgerFilter> = _filter.asStateFlow()

    private val _simulatorState = MutableStateFlow(SimulatorState())
    val simulatorState: StateFlow<SimulatorState> = _simulatorState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Native Dashboard/Ledger/Simulator, 1: Tailwind Web App
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Persistent Loan Settings (Monthly EMI, Annual Rate, Scheduled Due Day)
    private val loanPreferences = LoanPreferences(application)
    private val _loanSettings = MutableStateFlow(loanPreferences.loadSettings())
    val loanSettings: StateFlow<LoanSettings> = _loanSettings.asStateFlow()

    fun updateLoanSettings(
        emi: Double,
        rate: Double,
        dueDay: Int,
        effectiveFromNextMonth: Boolean = true,
        previousEmi: Double = 21500.0
    ) {
        val updated = LoanSettings(
            monthlyEmiAmount = emi,
            annualRatePercent = rate,
            emiDueDayOfMonth = dueDay.coerceIn(1, 28),
            previousEmiAmount = previousEmi,
            effectiveFromNextMonth = effectiveFromNextMonth
        )
        _loanSettings.value = updated
        loanPreferences.saveSettings(updated)
        _simulatorState.value = _simulatorState.value.copy(annualRatePercent = rate)
    }

    fun updateEmiAmount(amount: Double) {
        val updated = _loanSettings.value.copy(monthlyEmiAmount = amount)
        _loanSettings.value = updated
        loanPreferences.saveSettings(updated)
        _simulatorState.value = _simulatorState.value.copy(
            regularMonthlyPaymentInput = amount.toLong().toString()
        )
    }

    fun updateEffectiveSchedule(effectiveFromNextMonth: Boolean) {
        val updated = _loanSettings.value.copy(effectiveFromNextMonth = effectiveFromNextMonth)
        _loanSettings.value = updated
        loanPreferences.saveSettings(updated)
    }

    fun resetSettingsToDefault() {
        val defaults = loanPreferences.resetToDefaults()
        _loanSettings.value = defaults
        _simulatorState.value = _simulatorState.value.copy(
            annualRatePercent = defaults.annualRatePercent,
            regularMonthlyPaymentInput = defaults.monthlyEmiAmount.toLong().toString()
        )
    }

    // Reminder Preferences & Settings (10th day EMI & 1st day Interest Log)
    private val reminderPrefs = ReminderPreferences(application)
    private val _reminderSettings = MutableStateFlow(reminderPrefs.loadSettings())
    val reminderSettings: StateFlow<ReminderSettings> = _reminderSettings.asStateFlow()

    fun updateEmiReminder(enabled: Boolean, day: Int = 10, hour: Int = 9, minute: Int = 0) {
        val updated = _reminderSettings.value.copy(
            emiReminderEnabled = enabled,
            emiReminderDay = day.coerceIn(1, 28),
            emiReminderHour = hour.coerceIn(0, 23),
            emiReminderMinute = minute.coerceIn(0, 59)
        )
        _reminderSettings.value = updated
        reminderPrefs.saveSettings(updated)
        LoanReminderScheduler.scheduleAllReminders(getApplication())
    }

    fun updateInterestReminder(enabled: Boolean, day: Int = 1, hour: Int = 9, minute: Int = 0) {
        val updated = _reminderSettings.value.copy(
            interestReminderEnabled = enabled,
            interestReminderDay = day.coerceIn(1, 28),
            interestReminderHour = hour.coerceIn(0, 23),
            interestReminderMinute = minute.coerceIn(0, 59)
        )
        _reminderSettings.value = updated
        reminderPrefs.saveSettings(updated)
        LoanReminderScheduler.scheduleAllReminders(getApplication())
    }

    fun triggerTestReminder(type: ReminderType) {
        LoanReminderScheduler.triggerImmediateTest(getApplication(), type)
    }

    fun logPreviousMonthInterest(amount: Double = 0.0, dateStr: String = "", monthLabel: String = "") {
        val cal = Calendar.getInstance()
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val fullMonthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        // Target the previous month and determine its exact last day (e.g. 30 Sep, 31 Oct)
        val prevCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -1)
        }
        val prevMonthIndex = prevCal.get(Calendar.MONTH)
        val prevMonthName = fullMonthNames[prevMonthIndex]
        val prevMonthShort = monthNames[prevMonthIndex]
        val prevYear = prevCal.get(Calendar.YEAR).toString()
        val lastDayOfPrevMonth = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val effectiveMonthLabel = monthLabel.ifBlank { prevMonthName }

        val txDate = dateStr.ifBlank {
            if (monthLabel.isNotBlank()) {
                val matchedMonthIdx = fullMonthNames.indexOfFirst { it.equals(monthLabel, ignoreCase = true) }
                    .takeIf { it >= 0 } ?: monthNames.indexOfFirst { it.equals(monthLabel, ignoreCase = true) }
                if (matchedMonthIdx >= 0) {
                    val labelCal = (cal.clone() as Calendar).apply {
                        set(Calendar.MONTH, matchedMonthIdx)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                    val labelLastDay = labelCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val labelShort = monthNames[matchedMonthIdx]
                    val labelYear = labelCal.get(Calendar.YEAR).toString()
                    "$labelLastDay $labelShort $labelYear"
                } else {
                    "$lastDayOfPrevMonth $prevMonthShort $prevYear"
                }
            } else {
                "$lastDayOfPrevMonth $prevMonthShort $prevYear"
            }
        }
        val exactAmount = if (amount > 0.0) {
            amount
        } else {
            reminderStatusInfo.value.pendingInterestAmount
        }

        addTransaction(
            date = txDate,
            type = TransactionType.INTEREST_CHARGED,
            amount = exactAmount,
            note = "$effectiveMonthLabel Month-End Interest charged"
        )
    }

    fun logSeptemberInterest(amount: Double = 0.0, dateStr: String = "") {
        val exactAmt = if (amount > 0.0) amount else reminderStatusInfo.value.pendingInterestAmount
        logPreviousMonthInterest(amount = exactAmt, dateStr = dateStr, monthLabel = "September")
    }

    fun logCurrentEmiPayment(amount: Double = 0.0, dateStr: String = "") {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val fullMonthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val currentMonthName = fullMonthNames[currentMonth]
        val currentMonthShort = monthNames[currentMonth]

        val detectedYear = cal.get(Calendar.YEAR).toString()
        val dueDay = loanSettings.value.emiDueDayOfMonth
        val exactAmount = if (amount > 0.0) amount else loanSettings.value.monthlyEmiAmount
        val dayStr = if (dueDay < 10) "0$dueDay" else "$dueDay"
        val txDate = dateStr.ifBlank {
            "$dayStr $currentMonthShort $detectedYear"
        }
        addTransaction(
            date = txDate,
            type = TransactionType.PAYMENT,
            amount = exactAmount,
            note = "Scheduled Monthly EMI Payment ($currentMonthName $detectedYear)"
        )
    }

    init {
        LoanReminderScheduler.scheduleAllReminders(application)
        val db = LoanDatabase.getDatabase(application, viewModelScope)
        repository = LoanRepository(db.transactionDao())

        transactions = repository.allTransactionsAsc.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.ensurePopulated()
        }

        viewModelScope.launch {
            transactions.collect { list ->
                if (list.isNotEmpty()) {
                    val latest = list.last()
                    _simulatorState.value = _simulatorState.value.copy(
                        principalInput = if (latest.resultingBalance % 1.0 == 0.0) {
                            latest.resultingBalance.toLong().toString()
                        } else {
                            String.format(Locale.US, "%.2f", latest.resultingBalance)
                        }
                    )
                }
            }
        }
    }

    fun syncSimulatorWithLatestBalance() {
        val latest = transactions.value.lastOrNull()?.resultingBalance ?: 1017937.0
        _simulatorState.value = _simulatorState.value.copy(
            principalInput = if (latest % 1.0 == 0.0) {
                latest.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", latest)
            }
        )
    }

    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        transactions,
        _simulatorState,
        _loanSettings
    ) { list, _, settings ->
        computeDashboardSummary(list, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = computeDashboardSummary(emptyList(), loanPreferences.loadSettings())
    )

    val reminderStatusInfo: StateFlow<ReminderStatusInfo> = transactions.map { list ->
        computeReminderStatus(list)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = computeReminderStatus(emptyList())
    )

    val simulatorResult: StateFlow<SimulatorResult> = _simulatorState.combine(transactions) { sim, txList ->
        calculateSimulator(sim, txList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateSimulator(_simulatorState.value, emptyList())
    )

    // Monthly Interest Forecast & Particular Date Payoff Engine
    private val _particularDateState = MutableStateFlow(ParticularDateCustomState())
    val particularDateState: StateFlow<ParticularDateCustomState> = _particularDateState.asStateFlow()

    fun updateParticularDateMonth(monthIndex: Int) {
        _particularDateState.value = _particularDateState.value.copy(selectedMonthOption = monthIndex)
    }

    fun updateParticularDateDay(day: Int) {
        _particularDateState.value = _particularDateState.value.copy(dayOfMonth = day.coerceIn(1, 31))
    }

    fun updateParticularDateAmount(amount: String) {
        _particularDateState.value = _particularDateState.value.copy(amountInput = amount)
    }

    fun toggleParticularDateTenthDay(include: Boolean) {
        _particularDateState.value = _particularDateState.value.copy(includeTenthDayPayoff = include)
    }

    private val _selectedCycle = MutableStateFlow("AUTO")
    val selectedCycle: StateFlow<String> = _selectedCycle.asStateFlow()

    fun selectBillingCycle(cycle: String) {
        _selectedCycle.value = cycle
    }

    fun logCustomParticularDatePayment(note: String = "") {
        val state = _particularDateState.value
        val amount = state.amountInput.toDoubleOrNull() ?: 100000.0
        val targetMonth = if (state.selectedMonthOption == 0) thisMonthForecast.value.monthName else nextMonthForecast.value.monthName
        val parts = targetMonth.split(" ")
        val monthShort = if (parts.isNotEmpty()) parts[0].take(3) else "Oct"
        val yearStr = if (parts.size > 1) parts[1] else Calendar.getInstance().get(Calendar.YEAR).toString()
        val dayStr = if (state.dayOfMonth < 10) "0${state.dayOfMonth}" else "${state.dayOfMonth}"
        val dateStr = "$dayStr $monthShort $yearStr"
        val cleanNote = note.ifBlank { "Part Payment / Payoff on $dateStr" }
        addTransaction(dateStr, TransactionType.PAYMENT, amount, cleanNote)
    }

    val activeCycleInfo: StateFlow<ActiveCycleInfo> = combine(
        transactions,
        _simulatorState,
        _loanSettings,
        _selectedCycle
    ) { txList, sim, settings, cycle ->
        calculateCyclePair(txList, sim.annualRatePercent, settings, cycle).third
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateCyclePair(emptyList(), 7.35, loanPreferences.loadSettings(), "AUTO").third
    )

    val thisMonthForecast: StateFlow<MonthInterestBreakdown> = combine(
        transactions,
        _simulatorState,
        _loanSettings,
        _selectedCycle
    ) { txList, sim, settings, cycle ->
        calculateCyclePair(txList, sim.annualRatePercent, settings, cycle).first
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateCyclePair(emptyList(), 7.35, loanPreferences.loadSettings(), "AUTO").first
    )

    val nextMonthForecast: StateFlow<MonthInterestBreakdown> = combine(
        transactions,
        _simulatorState,
        _loanSettings,
        _selectedCycle
    ) { txList, sim, settings, cycle ->
        calculateCyclePair(txList, sim.annualRatePercent, settings, cycle).second
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateCyclePair(emptyList(), 7.35, loanPreferences.loadSettings(), "AUTO").second
    )

    val particularDateResult: StateFlow<ParticularDateCalculation> = combine(
        _particularDateState,
        thisMonthForecast,
        nextMonthForecast,
        _simulatorState,
        _loanSettings
    ) { custom, thisM, nextM, sim, settings ->
        calculateParticularDateCustom(custom, thisM, nextM, sim.annualRatePercent, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculateParticularDateCustom(
            ParticularDateCustomState(),
            calculateCyclePair(emptyList(), 7.35, loanPreferences.loadSettings(), "AUTO").first,
            calculateCyclePair(emptyList(), 7.35, loanPreferences.loadSettings(), "AUTO").second,
            7.35,
            loanPreferences.loadSettings()
        )
    )

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun setFilter(filter: LedgerFilter) {
        _filter.value = filter
    }

    // AI Loan Advisor State & Communication
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "welcome",
                sender = ChatSender.AI,
                text = """
👋 **Hello! I'm your AI Loan Expert & Advisor.**

I have real-time access to your live loan account:
• **Current Balance:** ₹10,54,003 at **7.35% p.a.** (₹212.24/day)
• **Scheduled Monthly EMI:** ₹21,500 due on the 10th
• **Debt-Free Target:** ~4 yrs 11 mos remaining

Ask me any doubt, for example:
1. 💡 **Prepayment Impact:** What happens if you pay ₹50,000 next month?
2. 📐 **Daily Reducing Proof:** How is daily interest calculated on your balance?
3. 📅 **Payment Timing:** Why paying on the 5th saves more than paying on the 25th?
4. 🚀 **Tenure Accelerators:** Strategies to become debt-free 1 year earlier.
                """.trimIndent(),
                timestamp = System.currentTimeMillis(),
                isContextAttached = true,
                isGeminiLive = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _customApiKey = MutableStateFlow<String?>(null)
    val customApiKey: StateFlow<String?> = _customApiKey.asStateFlow()

    fun buildLiveLoanContext(): LoanLiveContext {
        val summary = dashboardSummary.value
        val settings = loanSettings.value
        val forecast = thisMonthForecast.value
        val txs = transactions.value

        val annualRate = settings.annualRatePercent
        val dailyRatePercent = annualRate / 365.0
        val currentPrincipal = summary.currentOutstandingPrincipal
        val dailyInterestToday = (currentPrincipal * (annualRate / 100.0)) / 365.0

        val recentTxsSummary = txs.takeLast(6).joinToString("\n") { tx ->
            "- ${tx.date} | ${tx.type.displayName} | ${formatCurrency(tx.amount)} | Resulting: ${formatCurrency(tx.resultingBalance)} | ${tx.note}"
        }.ifBlank { "No recent transactions" }

        return LoanLiveContext(
            currentOutstandingPrincipal = currentPrincipal,
            annualRatePercent = annualRate,
            dailyRatePercent = dailyRatePercent,
            dailyInterestToday = dailyInterestToday,
            monthlyEmi = settings.monthlyEmiAmount,
            scheduledDueDay = settings.emiDueDayOfMonth,
            totalPaymentsMade = summary.totalPaymentsMade,
            totalInterestCharged = summary.totalInterestCharged,
            principalReductionPercent = summary.principalReductionPercent,
            estimatedPayoffMonths = summary.estimatedPayoffMonths,
            estimatedPayoffCompletionDateText = summary.estimatedPayoffCompletionDateText,
            estimatedPayoffYearsMonthsText = summary.estimatedPayoffYearsMonthsText,
            thisMonthEstimatedInterest = forecast.totalInterest,
            recentTransactionsSummary = recentTxsSummary
        )
    }

    fun sendAiMessage(question: String) {
        if (question.isBlank() || _isAiThinking.value) return
        val trimmed = question.trim()
        val userMsg = ChatMessage(
            id = "usr_${System.currentTimeMillis()}",
            sender = ChatSender.USER,
            text = trimmed,
            timestamp = System.currentTimeMillis()
        )
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch {
            try {
                val context = buildLiveLoanContext()
                val history = _chatMessages.value
                val result = AiAdvisorService.askAdvisor(
                    userQuestion = trimmed,
                    loanContext = context,
                    conversationHistory = history,
                    customApiKey = _customApiKey.value
                )
                val aiMsg = ChatMessage(
                    id = "ai_${System.currentTimeMillis()}",
                    sender = ChatSender.AI,
                    text = result.text,
                    timestamp = System.currentTimeMillis(),
                    isGeminiLive = result.isFromGemini
                )
                _chatMessages.value = _chatMessages.value + aiMsg
            } catch (e: Exception) {
                val fallback = LocalLoanExpertEngine.generateExpertResponse(trimmed, buildLiveLoanContext())
                val aiMsg = ChatMessage(
                    id = "ai_${System.currentTimeMillis()}",
                    sender = ChatSender.AI,
                    text = fallback,
                    timestamp = System.currentTimeMillis(),
                    isGeminiLive = false
                )
                _chatMessages.value = _chatMessages.value + aiMsg
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun clearAiChat() {
        val context = buildLiveLoanContext()
        _chatMessages.value = listOf(
            ChatMessage(
                id = "welcome_${System.currentTimeMillis()}",
                sender = ChatSender.AI,
                text = """
👋 **Chat Reset.** I'm your AI Loan Expert.

Current Live Account Status:
• **Outstanding Principal:** ${formatCurrency(context.currentOutstandingPrincipal)}
• **Annual Rate:** ${context.annualRatePercent}% p.a. (₹${String.format(Locale.US, "%.2f", context.dailyInterestToday)}/day)
• **Monthly EMI:** ${formatCurrency(context.monthlyEmi)} on the ${context.scheduledDueDay}th

How can I assist your loan strategy today?
                """.trimIndent(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun setCustomApiKey(key: String?) {
        _customApiKey.value = key?.trim()?.ifBlank { null }
    }


    fun updatePrincipalInput(value: String) {
        _simulatorState.value = _simulatorState.value.copy(principalInput = value)
    }

    fun updateAnnualRate(rate: Double) {
        _simulatorState.value = _simulatorState.value.copy(annualRatePercent = rate)
    }

    fun updateExtraPaymentInput(value: String) {
        _simulatorState.value = _simulatorState.value.copy(extraPaymentInput = value)
    }

    fun updateDaysInMonth(days: Int) {
        _simulatorState.value = _simulatorState.value.copy(daysInMonth = days)
    }

    fun updateRegularMonthlyPayment(value: String) {
        _simulatorState.value = _simulatorState.value.copy(regularMonthlyPaymentInput = value)
    }

    fun updatePlannedDaysOffset(offsetDays: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, max(1, offsetDays))
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formatted = sdf.format(cal.time)
        _simulatorState.value = _simulatorState.value.copy(
            plannedDaysOffset = max(1, offsetDays),
            plannedDateText = formatted
        )
    }

    fun updatePlannedDate(dateText: String, offsetDays: Int) {
        _simulatorState.value = _simulatorState.value.copy(
            plannedDateText = dateText,
            plannedDaysOffset = max(1, offsetDays)
        )
    }

    fun applyCurrentOutstandingToSimulator(amount: Double) {
        _simulatorState.value = _simulatorState.value.copy(
            principalInput = amount.toLong().toString()
        )
    }

    fun addTransaction(
        date: String,
        type: TransactionType,
        amount: Double,
        note: String
    ) {
        viewModelScope.launch {
            val currentList = transactions.value
            val lastBalance = currentList.lastOrNull()?.resultingBalance ?: 1592956.0
            val newBalance = when (type) {
                TransactionType.PAYMENT -> max(0.0, lastBalance - amount)
                TransactionType.INTEREST_CHARGED -> lastBalance + amount
                TransactionType.OPENING_BALANCE -> amount
                TransactionType.BALANCE_UPDATE -> amount
            }

            val newTx = LoanTransaction(
                date = date.ifBlank {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                },
                type = type,
                amount = amount,
                resultingBalance = newBalance,
                note = note
            )
            repository.insertTransaction(newTx)
            repository.recalculateBalances()
            _simulatorState.value = _simulatorState.value.copy(
                principalInput = newBalance.toLong().toString()
            )
        }
    }

    fun deleteTransaction(transaction: LoanTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            repository.resetToDefault()
        }
    }

    fun computeDashboardSummary(
        list: List<LoanTransaction>,
        settings: LoanSettings = loanPreferences.loadSettings()
    ): DashboardSummary {
        val normalMonthly = settings.monthlyEmiAmount
        val ratePercent = settings.annualRatePercent
        val monthlyRate = (ratePercent / 100.0) / 12.0

        if (list.isEmpty()) {
            val defaultPrincipal = 1054003.0
            val estimatedMonths: Int
            val estimatedYearsMonths: String
            val completionDateText: String
            if (normalMonthly > (defaultPrincipal * monthlyRate)) {
                val numMonthsDouble = -ln(1.0 - (defaultPrincipal * monthlyRate) / normalMonthly) / ln(1.0 + monthlyRate)
                estimatedMonths = numMonthsDouble.roundToInt()
                estimatedYearsMonths = formatMonthsToYears(estimatedMonths)
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, estimatedMonths)
                completionDateText = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
            } else {
                estimatedMonths = 999
                estimatedYearsMonths = "> 50 Years"
                completionDateText = "Indefinite"
            }
            val initialPrincipal = 1592956.0
            val initialTenureMonths = if (normalMonthly > (initialPrincipal * monthlyRate)) {
                val numInit = -ln(1.0 - (initialPrincipal * monthlyRate) / normalMonthly) / ln(1.0 + monthlyRate)
                numInit.roundToInt()
            } else 100
            val monthsSaved = max(0, initialTenureMonths - estimatedMonths)

            val previousMonthly = settings.previousEmiAmount
            val baselineMonths = if (defaultPrincipal > 0 && previousMonthly > (defaultPrincipal * monthlyRate)) {
                val num = -ln(1.0 - (defaultPrincipal * monthlyRate) / previousMonthly) / ln(1.0 + monthlyRate)
                num.roundToInt()
            } else 59
            val savedByNewEmi = max(0, baselineMonths - estimatedMonths)

            return DashboardSummary(
                currentOutstandingPrincipal = defaultPrincipal,
                lastRecordedInterestCharged = 7663.0,
                estimatedAnnualRate = ratePercent,
                totalPaymentsMade = 600865.0,
                totalInterestCharged = 34814.0,
                initialPrincipal = initialPrincipal,
                principalReductionPercent = 33.8,
                normalMonthlyPayoff = normalMonthly,
                estimatedPayoffMonths = estimatedMonths,
                estimatedPayoffYearsMonthsText = estimatedYearsMonths,
                estimatedPayoffCompletionDateText = completionDateText,
                initialTenureMonths = initialTenureMonths,
                tenureMonthsSavedSoFar = monthsSaved,
                previousMonthlyPayoff = previousMonthly,
                effectiveFromNextMonth = settings.effectiveFromNextMonth,
                baselinePayoffMonths = baselineMonths,
                monthsSavedByNewEmi = savedByNewEmi
            )
        }

        val currentOutstanding = list.lastOrNull()?.resultingBalance ?: 0.0
        val lastInterestTx = list.filter { it.type == TransactionType.INTEREST_CHARGED }.lastOrNull()
        val lastInterest = lastInterestTx?.amount ?: 7663.0

        val estimatedRate = settings.annualRatePercent

        val totalPayments = list.filter { it.type == TransactionType.PAYMENT }.sumOf { it.amount }
        val totalInterest = list.filter { it.type == TransactionType.INTEREST_CHARGED }.sumOf { it.amount }
        val initialPrincipal = list.firstOrNull { it.type == TransactionType.OPENING_BALANCE }?.amount
            ?: (list.firstOrNull()?.resultingBalance ?: 1592956.0)
        val reduction = if (initialPrincipal > 0) {
            val diff = initialPrincipal - currentOutstanding
            ((diff / initialPrincipal) * 100.0 * 10).roundToInt() / 10.0
        } else 0.0

        // Exact Amortization Calculation with normal payoff of 21,500
        val estimatedMonths: Int
        val estimatedYearsMonths: String
        val completionDateText: String

        if (currentOutstanding > 0 && normalMonthly > (currentOutstanding * monthlyRate)) {
            val numMonthsDouble = -ln(1.0 - (currentOutstanding * monthlyRate) / normalMonthly) / ln(1.0 + monthlyRate)
            estimatedMonths = numMonthsDouble.roundToInt()
            estimatedYearsMonths = formatMonthsToYears(estimatedMonths)
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, estimatedMonths)
            completionDateText = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
        } else if (currentOutstanding <= 0.0) {
            estimatedMonths = 0
            estimatedYearsMonths = "0 months (Paid off)"
            completionDateText = "Fully Paid Off"
        } else {
            estimatedMonths = 999
            estimatedYearsMonths = "> 50 Years"
            completionDateText = "Indefinite"
        }

        val initialTenureMonths = if (initialPrincipal > 0 && normalMonthly > (initialPrincipal * monthlyRate)) {
            val numInit = -ln(1.0 - (initialPrincipal * monthlyRate) / normalMonthly) / ln(1.0 + monthlyRate)
            numInit.roundToInt()
        } else 100

        val monthsSaved = max(0, initialTenureMonths - estimatedMonths)

        val previousMonthly = settings.previousEmiAmount
        val baselineMonths = if (currentOutstanding > 0 && previousMonthly > (currentOutstanding * monthlyRate)) {
            val num = -ln(1.0 - (currentOutstanding * monthlyRate) / previousMonthly) / ln(1.0 + monthlyRate)
            num.roundToInt()
        } else 59
        val savedByNewEmi = max(0, baselineMonths - estimatedMonths)

        return DashboardSummary(
            currentOutstandingPrincipal = currentOutstanding,
            lastRecordedInterestCharged = lastInterest,
            estimatedAnnualRate = estimatedRate,
            totalPaymentsMade = totalPayments,
            totalInterestCharged = totalInterest,
            initialPrincipal = initialPrincipal,
            principalReductionPercent = reduction,
            normalMonthlyPayoff = normalMonthly,
            estimatedPayoffMonths = estimatedMonths,
            estimatedPayoffYearsMonthsText = estimatedYearsMonths,
            estimatedPayoffCompletionDateText = completionDateText,
            initialTenureMonths = initialTenureMonths,
            tenureMonthsSavedSoFar = monthsSaved,
            previousMonthlyPayoff = previousMonthly,
            effectiveFromNextMonth = settings.effectiveFromNextMonth,
            baselinePayoffMonths = baselineMonths,
            monthsSavedByNewEmi = savedByNewEmi
        )
    }

    private fun findInterestTxForMonth(
        monthShort: String,
        monthName: String,
        nextMonthShort: String,
        txList: List<LoanTransaction>
    ): LoanTransaction? {
        val interestTxs = txList.filter { it.type == TransactionType.INTEREST_CHARGED }
        return interestTxs.findLast { tx ->
            val note = tx.note.trim()
            val mentionsThisMonth = note.contains(monthShort, ignoreCase = true) ||
                    note.contains(monthName, ignoreCase = true)
            if (mentionsThisMonth) return@findLast true

            // If note mentions another month explicitly, it belongs to that other month
            val otherMonths = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                .filter { !it.equals(monthShort, ignoreCase = true) }
            val mentionsOtherMonth = otherMonths.any { note.contains(it, ignoreCase = true) }
            if (mentionsOtherMonth) return@findLast false

            // Check date within late target month (27..31)
            val isLateTargetMonth = (27..31).any { d ->
                tx.date.contains(String.format(Locale.ENGLISH, "%02d %s", d, monthShort), ignoreCase = true) ||
                tx.date.contains(String.format(Locale.ENGLISH, "%d %s", d, monthShort), ignoreCase = true)
            }
            if (isLateTargetMonth) return@findLast true

            // Or early in following month (01..05)
            val isEarlyNextMonth = (1..5).any { d ->
                tx.date.contains(String.format(Locale.ENGLISH, "%02d %s", d, nextMonthShort), ignoreCase = true) ||
                tx.date.contains(String.format(Locale.ENGLISH, "%d %s", d, nextMonthShort), ignoreCase = true)
            }
            isEarlyNextMonth
        }
    }

    private fun computeReminderStatus(txList: List<LoanTransaction>): ReminderStatusInfo {
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH) // 8=Sep, 9=Oct, 10=Nov
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val fullMonthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val currentMonthName = fullMonthNames[currentMonth]
        val currentMonthShort = monthNames[currentMonth]
        val prevMonthIndex = if (currentMonth == 0) 11 else currentMonth - 1
        val prevMonthName = fullMonthNames[prevMonthIndex]
        val prevMonthShort = monthNames[prevMonthIndex]
        val currentYear = cal.get(Calendar.YEAR)

        val nextMonthIndex = (currentMonth + 1) % 12
        val nextMonthShort = monthNames[nextMonthIndex]

        val dueDay = loanSettings.value.emiDueDayOfMonth
        val expectedEmi = loanSettings.value.monthlyEmiAmount
        val rate = loanSettings.value.annualRatePercent

        val emiTx = txList.findLast { tx ->
            tx.type == TransactionType.PAYMENT &&
            (tx.date.contains(currentMonthShort, ignoreCase = true) || tx.note.contains(currentMonthShort, ignoreCase = true)) &&
            tx.amount >= (expectedEmi * 0.7)
        }

        val interestTx = findInterestTxForMonth(prevMonthShort, prevMonthName, currentMonthShort, txList)

        val detectedYear = txList.lastOrNull()?.date?.let { Regex("""\b(20\d\d)\b""").find(it)?.value }
            ?: currentYear.toString()
        val prevYear = if (currentMonth == 0) {
            ((detectedYear.toIntOrNull() ?: currentYear) - 1).toString()
        } else {
            detectedYear
        }
        val pendingInterest = if (interestTx == null) {
            when {
                prevMonthShort.equals("Sep", ignoreCase = true) -> {
                    calculateSeptemberForecast(txList, rate, loanSettings.value, detectedYear).totalInterest
                }
                prevMonthShort.equals("Oct", ignoreCase = true) -> {
                    val monthOpeningTx = txList.firstOrNull { tx ->
                        (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                        (tx.date.contains("Oct", ignoreCase = true) || tx.date.contains("October", ignoreCase = true))
                    }
                    val sepTx = findInterestTxForMonth("Sep", "September", "Oct", txList)
                    val lastSepTx = txList.lastOrNull { it.date.contains("Sep", ignoreCase = true) }
                    val sepForecast = calculateSeptemberForecast(txList, rate, loanSettings.value, detectedYear)
                    val octOpening = monthOpeningTx?.resultingBalance
                        ?: sepTx?.resultingBalance
                        ?: (if (lastSepTx != null) lastSepTx.resultingBalance + sepForecast.totalInterest else 1060933.0)
                    calculateOctoberForecast(txList, rate, loanSettings.value, octOpening, detectedYear).totalInterest
                }
                prevMonthShort.equals("Nov", ignoreCase = true) -> {
                    val monthOpeningTx = txList.firstOrNull { tx ->
                        (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                        (tx.date.contains("Oct", ignoreCase = true) || tx.date.contains("October", ignoreCase = true))
                    }
                    val sepTx = findInterestTxForMonth("Sep", "September", "Oct", txList)
                    val lastSepTx = txList.lastOrNull { it.date.contains("Sep", ignoreCase = true) }
                    val sepForecast = calculateSeptemberForecast(txList, rate, loanSettings.value, detectedYear)
                    val octOpening = monthOpeningTx?.resultingBalance
                        ?: sepTx?.resultingBalance
                        ?: (if (lastSepTx != null) lastSepTx.resultingBalance + sepForecast.totalInterest else 1060933.0)
                    val octForecast = calculateOctoberForecast(txList, rate, loanSettings.value, octOpening, detectedYear)
                    val octTx = findInterestTxForMonth("Oct", "October", "Nov", txList)
                    val novOpening = octTx?.resultingBalance ?: octForecast.closingWithInterest
                    calculateNovemberForecast(txList, rate, loanSettings.value, novOpening, detectedYear).totalInterest
                }
                else -> {
                    val prevCal = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    val daysInPrev = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val bal = txList.lastOrNull()?.resultingBalance ?: 1054003.0
                    calculateGenericMonthForecast(
                        monthNameOnly = prevMonthName,
                        monthShort = prevMonthShort,
                        monthNumber = prevMonthIndex + 1,
                        totalDays = daysInPrev,
                        defaultOpening = bal,
                        openingOverride = bal,
                        txList = txList,
                        rate = rate,
                        settings = loanSettings.value,
                        year = prevYear
                    ).totalInterest
                }
            }
        } else {
            interestTx.amount
        }

        return ReminderStatusInfo(
            isEmiPaidThisMonth = emiTx != null,
            emiPaidDate = emiTx?.date,
            emiPaidAmount = emiTx?.amount,
            emiDueDay = dueDay,
            emiExpectedAmount = expectedEmi,
            isInterestLoggedForPreviousMonth = interestTx != null,
            interestLoggedDate = interestTx?.date,
            interestLoggedAmount = interestTx?.amount,
            pendingInterestAmount = pendingInterest,
            currentMonthLabel = "$currentMonthName $detectedYear",
            previousMonthLabel = "$prevMonthName $prevYear",
            nextEmiDueDateLabel = "${dueDay}th of $currentMonthName",
            nextInterestLogDateLabel = "1st of $currentMonthName"
        )
    }

    private fun calculateSimulator(sim: SimulatorState, txList: List<LoanTransaction>): SimulatorResult {
        val latestBalance = txList.lastOrNull()?.resultingBalance ?: 1017937.0
        val principal = sim.principalInput.toDoubleOrNull() ?: latestBalance
        val extra = sim.extraPaymentInput.toDoubleOrNull() ?: 0.0
        val newPrincipal = max(0.0, principal - extra)
        val rate = sim.annualRatePercent
        val regularEmi = sim.regularMonthlyPaymentInput.toDoubleOrNull() ?: 21500.0

        val cal = Calendar.getInstance()
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val fullMonthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        // Dynamically resolve active simulation month from ledger transactions or current system calendar
        val latestTx = txList.lastOrNull()
        val latestTxDate = latestTx?.date ?: ""
        var activeMonthIndex = cal.get(Calendar.MONTH)
        for (i in monthNames.indices) {
            if (latestTxDate.contains(monthNames[i], ignoreCase = true)) {
                activeMonthIndex = i
                break
            }
        }
        // If latest transaction was interest charged on the last day of month M, next cycle is active
        if (latestTx?.type == TransactionType.INTEREST_CHARGED) {
            activeMonthIndex = (activeMonthIndex + 1) % 12
        }

        val curMonthName = fullMonthNames[activeMonthIndex]
        val nxtMonthIndex = (activeMonthIndex + 1) % 12
        val nxtMonthName = fullMonthNames[nxtMonthIndex]
        val curMonthShort = monthNames[activeMonthIndex]
        val nxtMonthShort = monthNames[nxtMonthIndex]
        val prevMonthIndex = if (activeMonthIndex == 0) 11 else activeMonthIndex - 1
        val prevMonthName = fullMonthNames[prevMonthIndex]

        val curCal = (cal.clone() as Calendar).apply {
            set(Calendar.MONTH, activeMonthIndex)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val actualDaysInCurMonth = curCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val nxtCal = (curCal.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        val actualDaysInNxtMonth = nxtCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val curMonthDays = if (sim.daysInMonth in 28..31) sim.daysInMonth else actualDaysInCurMonth

        // 1. Daily Reducing Interest Calculations
        // Formula: Principal * (Annual Rate % / 365)
        val dailyRate = rate / 100.0 / 365.0
        val dailyInterestCurrent = principal * dailyRate
        val dailyInterestAfterPrepayment = newPrincipal * dailyRate
        val dailyInterestSaved = max(0.0, extra * dailyRate)

        // Days until planned payoff / prepayment
        val daysUntilPlanned = max(1, sim.plannedDaysOffset)
        val accruedInterestTillPlanned = daysUntilPlanned * dailyInterestCurrent
        val settlementPayoffAmount = principal + accruedInterestTillPlanned

        // Check if 27 Sep 1L payoff is in transactions
        val hasSep27Payoff = txList.any { it.amount == 100000.0 && it.date.contains("27 Sep") }
        val sep27PayoffAmount = 100000.0

        val emiTxThisMonth = txList.findLast { tx ->
            tx.type == TransactionType.PAYMENT &&
            (tx.date.contains(curMonthShort, ignoreCase = true) || tx.note.contains(curMonthShort, ignoreCase = true)) &&
            tx.amount >= (regularEmi * 0.7)
        }
        val isEmiPaidThisMonth = emiTxThisMonth != null

        val emiDay = 10
        val curPart1Days = max(1, emiDay - 1)
        val curPart2Days = max(1, curMonthDays - curPart1Days)

        val isSyncedWithLatest = kotlin.math.abs(principal - latestBalance) < 1.0
        val curBalBeforeEmi: Double
        val curBalAfterEmi: Double

        if (isEmiPaidThisMonth && isSyncedWithLatest) {
            curBalBeforeEmi = principal + regularEmi
            curBalAfterEmi = principal
        } else {
            curBalBeforeEmi = principal
            curBalAfterEmi = max(0.0, principal - regularEmi)
        }

        val currentMonthBaseline = (curBalBeforeEmi * dailyRate * curPart1Days) +
                (curBalAfterEmi * dailyRate * curPart2Days)

        val currentMonthSaved = if (extra > 0 && daysUntilPlanned <= curMonthDays) {
            val remainingDaysCurrent = max(0, curMonthDays - daysUntilPlanned + 1)
            extra * dailyRate * remainingDaysCurrent
        } else {
            0.0
        }
        val currentMonthWithPrepay = max(0.0, currentMonthBaseline - currentMonthSaved)
        val actualSepInterestWithLastPayoff = currentMonthWithPrepay
        val oldSepBaselineWithoutSep27 = currentMonthBaseline
        val sep27SavedInMonth = currentMonthSaved

        // Output 2: Next Month Interest (with month-end capitalized interest added to starting principal)
        val nextMonthTotalDays = actualDaysInNxtMonth
        val nextMonthPart1Days = 9
        val nextMonthPart2Days = max(1, nextMonthTotalDays - nextMonthPart1Days)

        val nextOpeningBaseline = curBalAfterEmi + currentMonthBaseline
        val nextMonthPart1Interest = nextOpeningBaseline * dailyRate * nextMonthPart1Days
        val nextMonthBalanceAfterEmi = max(0.0, nextOpeningBaseline - regularEmi)
        val nextMonthPart2InterestBaseline = nextMonthBalanceAfterEmi * dailyRate * nextMonthPart2Days
        val nextMonthInterestBaseline = nextMonthPart1Interest + nextMonthPart2InterestBaseline

        val nextOpeningPrepaid = max(0.0, curBalAfterEmi - extra) + currentMonthWithPrepay
        val nextMonthPart1InterestPrepaid = nextOpeningPrepaid * dailyRate * nextMonthPart1Days
        val nextMonthBalanceAfterEmiAndPrepay = max(0.0, nextOpeningPrepaid - regularEmi)
        val nextMonthPart2InterestPrepaid = nextMonthBalanceAfterEmiAndPrepay * dailyRate * nextMonthPart2Days
        val nextMonthInterestWithPrepay = nextMonthPart1InterestPrepaid + nextMonthPart2InterestPrepaid
        val nextMonthInterestSaved = max(0.0, nextMonthInterestBaseline - nextMonthInterestWithPrepay)

        val oldNextMonthBaselineWithoutSep27 = nextMonthInterestBaseline
        val recurringMonthlySavingFromLastPayoff = nextMonthInterestSaved

        // Monthly amortization simulation:
        val monthlyRate = (rate / 100.0) / 12.0
        val regularPayoffMonths: Int
        val isRegularPossible: Boolean
        val regularTotalInterest: Double

        if (monthlyRate > 0 && regularEmi > (principal * monthlyRate)) {
            isRegularPossible = true
            val numMonthsDouble = -ln(1.0 - (principal * monthlyRate) / regularEmi) / ln(1.0 + monthlyRate)
            regularPayoffMonths = numMonthsDouble.roundToInt()
            regularTotalInterest = (regularPayoffMonths * regularEmi) - principal
        } else {
            isRegularPossible = false
            regularPayoffMonths = 999
            regularTotalInterest = 0.0
        }

        val prepaidMonths: Int
        val prepaidTotalInterest: Double
        if (monthlyRate > 0 && regularEmi > (newPrincipal * monthlyRate) && newPrincipal > 0) {
            val numMonthsDouble = -ln(1.0 - (newPrincipal * monthlyRate) / regularEmi) / ln(1.0 + monthlyRate)
            prepaidMonths = numMonthsDouble.roundToInt()
            prepaidTotalInterest = (prepaidMonths * regularEmi) - newPrincipal
        } else if (newPrincipal <= 0.0) {
            prepaidMonths = 0
            prepaidTotalInterest = 0.0
        } else {
            prepaidMonths = regularPayoffMonths
            prepaidTotalInterest = regularTotalInterest
        }

        val monthsSaved = if (isRegularPossible) max(0, regularPayoffMonths - prepaidMonths) else 0
        val totalLifetimeInterestSaved = if (isRegularPossible && prepaidMonths > 0) {
            max(0.0, regularTotalInterest - prepaidTotalInterest)
        } else if (newPrincipal <= 0.0 && isRegularPossible) {
            max(0.0, regularTotalInterest)
        } else {
            nextMonthInterestSaved * 12
        }

        // Project Calendar Target Payoff Dates
        val monthSdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        val regularCal = (curCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        regularCal.add(Calendar.MONTH, regularPayoffMonths)
        val regularPayoffDateText = if (isRegularPossible && regularPayoffMonths < 900) {
            monthSdf.format(regularCal.time)
        } else {
            "Never (EMI < Interest)"
        }

        val prepaidCal = (curCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        prepaidCal.add(Calendar.MONTH, prepaidMonths)
        val prepaidPayoffDateText = if (prepaidMonths <= 0) {
            "Immediately (Paid off!)"
        } else if (prepaidMonths < 900) {
            monthSdf.format(prepaidCal.time)
        } else {
            "> 50 Years"
        }

        // Required monthly payment to close loan by the planned date
        val monthsToPlanned = max(1, (daysUntilPlanned / 30.41).roundToInt())
        val requiredMonthlyToPayoff = if (monthlyRate > 0 && monthsToPlanned > 0 && principal > 0) {
            val factor = (1.0 + monthlyRate).pow(monthsToPlanned.toDouble())
            if (factor > 1.0) {
                principal * (monthlyRate * factor) / (factor - 1.0)
            } else {
                principal / monthsToPlanned
            }
        } else {
            principal / max(1, monthsToPlanned)
        }

        return SimulatorResult(
            currentPrincipal = principal,
            extraPayment = extra,
            newPrincipal = newPrincipal,
            dailyInterestCurrent = dailyInterestCurrent,
            dailyInterestAfterPrepayment = dailyInterestAfterPrepayment,
            dailyInterestSaved = dailyInterestSaved,
            plannedDateText = sim.plannedDateText,
            daysUntilPlannedDate = daysUntilPlanned,
            accruedInterestTillPlannedDate = accruedInterestTillPlanned,
            settlementPayoffAmountOnPlannedDate = settlementPayoffAmount,
            // Month Names
            currentMonthName = curMonthName,
            nextMonthName = nxtMonthName,
            currentMonthShort = curMonthShort,
            nextMonthShort = nxtMonthShort,
            previousMonthName = prevMonthName,
            // Output 1: Current Month Interest
            currentMonthInterestBaseline = currentMonthBaseline,
            currentMonthInterestWithPrepayment = currentMonthWithPrepay,
            currentMonthInterestSaved = currentMonthSaved,
            currentMonthWithLastPayoff = actualSepInterestWithLastPayoff,
            currentMonthBeforeLastPayoff = oldSepBaselineWithoutSep27,
            hasSep27PayoffIncluded = hasSep27Payoff,
            sep27PayoffAmount = sep27PayoffAmount,
            sep27SavedInMonth = sep27SavedInMonth,
            // Output 2: Next Month Interest
            nextMonthInterestBaseline = nextMonthInterestBaseline,
            nextMonthInterestWithPrepayment = nextMonthInterestWithPrepay,
            nextMonthInterestSaved = nextMonthInterestSaved,
            nextMonthPart1Days = nextMonthPart1Days,
            nextMonthPart1Interest = nextMonthPart1Interest,
            nextMonthPart2Days = nextMonthPart2Days,
            nextMonthPart2InterestBaseline = nextMonthPart2InterestBaseline,
            nextMonthPart2InterestPrepaid = nextMonthPart2InterestPrepaid,
            nextMonthBalanceAfterEmi = nextMonthBalanceAfterEmi,
            nextMonthBalanceAfterEmiAndPrepay = nextMonthBalanceAfterEmiAndPrepay,
            oldNextMonthBaselineWithoutSep27 = oldNextMonthBaselineWithoutSep27,
            recurringMonthlySavingFromLastPayoff = recurringMonthlySavingFromLastPayoff,
            // Amortization Timelines
            regularMonthsToPayoff = regularPayoffMonths,
            regularYearsMonthsText = formatMonthsToYears(regularPayoffMonths),
            regularPayoffDateText = regularPayoffDateText,
            prepaidMonthsToPayoff = prepaidMonths,
            prepaidYearsMonthsText = formatMonthsToYears(prepaidMonths),
            prepaidPayoffDateText = prepaidPayoffDateText,
            monthsSaved = monthsSaved,
            totalLifetimeInterestSaved = totalLifetimeInterestSaved,
            isRegularPayoffPossible = isRegularPossible,
            requiredMonthlyToPayoffByPlannedDate = requiredMonthlyToPayoff,
            nextMonthStartingPrincipal = if (extra > 0) nextOpeningPrepaid else nextOpeningBaseline
        )
    }

    private fun formatMonthsToYears(months: Int): String {
        if (months <= 0) return "0 months (Paid off!)"
        if (months >= 900) return "> 50 years"
        val years = months / 12
        val remMonths = months % 12
        return when {
            years == 0 -> "$remMonths mo"
            remMonths == 0 -> "$years yr"
            else -> "$years yr $remMonths mo"
        }
    }

    fun calculateCyclePair(
        txList: List<LoanTransaction>,
        rate: Double,
        settings: LoanSettings = loanPreferences.loadSettings(),
        cycleSelection: String = "AUTO"
    ): Triple<MonthInterestBreakdown, MonthInterestBreakdown, ActiveCycleInfo> {
        val interestTxs = txList.filter { it.type == TransactionType.INTEREST_CHARGED }

        val sepInterestTx = findInterestTxForMonth("Sep", "September", "Oct", txList)
        val hasSepInterest = sepInterestTx != null
        val sepAmount = sepInterestTx?.amount ?: 0.0

        val octInterestTx = findInterestTxForMonth("Oct", "October", "Nov", txList)
        val hasOctInterest = octInterestTx != null

        val detectedYear = run {
            val sysYear = Calendar.getInstance().get(Calendar.YEAR).toString()
            val txYear = txList.lastOrNull()?.date?.let { Regex("""\b(20\d\d)\b""").find(it)?.value }
            if (txYear == null || txYear == "2024") sysYear else txYear
        }

        val hasAnyOctTx = txList.any { it.date.contains("Oct", ignoreCase = true) }
        val hasAnyNovTx = txList.any { it.date.contains("Nov", ignoreCase = true) }
        val sysMonth = Calendar.getInstance().get(Calendar.MONTH) // 8=Sep, 9=Oct, 10=Nov, 11=Dec

        val effectiveCycle = if (cycleSelection == "AUTO") {
            when {
                hasOctInterest || hasAnyNovTx || sysMonth >= 10 -> "NOV_DEC"
                hasSepInterest || hasAnyOctTx || sysMonth >= 9 -> "OCT_NOV"
                else -> "SEP_OCT"
            }
        } else {
            cycleSelection
        }

        return when (effectiveCycle) {
            "OCT_NOV" -> {
                // October (This Month) & November (Next Month)
                val monthOpeningTx = txList.firstOrNull { tx ->
                    (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                    (tx.date.contains("Oct", ignoreCase = true) || tx.date.contains("October", ignoreCase = true))
                }
                val lastSepTx = txList.lastOrNull { it.date.contains("Sep", ignoreCase = true) }
                val sepForecast = calculateSeptemberForecast(txList, rate, settings, detectedYear)
                val octOpening = monthOpeningTx?.resultingBalance
                    ?: sepInterestTx?.resultingBalance
                    ?: (if (lastSepTx != null) lastSepTx.resultingBalance + sepForecast.totalInterest else 1060933.0)
                val octForecast = calculateOctoberForecast(txList, rate, settings, octOpening, detectedYear)
                
                val novOpeningTx = txList.firstOrNull { tx ->
                    (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                    (tx.date.contains("Nov", ignoreCase = true) || tx.date.contains("November", ignoreCase = true))
                }
                val novOpening = novOpeningTx?.resultingBalance
                    ?: octInterestTx?.resultingBalance
                    ?: octForecast.closingWithInterest
                val novForecast = calculateNovemberForecast(txList, rate, settings, novOpening, detectedYear)
                val banner = if (hasSepInterest) {
                    "Active Cycle: October $detectedYear. September interest of ${formatCurrency(sepAmount)} is capitalized in ledger. Current Month: October $detectedYear • Next Month: November $detectedYear."
                } else {
                    "Active Cycle: October $detectedYear • Next Month: November $detectedYear."
                }
                val info = ActiveCycleInfo(
                    activeCycleKey = "OCT_NOV",
                    selectedCycleKey = cycleSelection,
                    thisMonthName = "October $detectedYear",
                    nextMonthName = "November $detectedYear",
                    previousMonthName = "September",
                    isAutoAdvanced = (hasSepInterest || hasAnyOctTx) && cycleSelection == "AUTO",
                    hasSepInterestLogged = hasSepInterest,
                    sepInterestAmount = sepAmount,
                    previousMonthInterestAmount = if (sepAmount > 0.0) sepAmount else sepForecast.totalInterest,
                    isPreviousMonthInterestLogged = hasSepInterest,
                    bannerMessage = banner
                )
                Triple(octForecast, novForecast, info)
            }
            "NOV_DEC" -> {
                // November (This Month) & December (Next Month)
                val monthOpeningTx = txList.firstOrNull { tx ->
                    (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                    (tx.date.contains("Oct", ignoreCase = true) || tx.date.contains("October", ignoreCase = true))
                }
                val lastSepTx = txList.lastOrNull { it.date.contains("Sep", ignoreCase = true) }
                val sepForecast = calculateSeptemberForecast(txList, rate, settings, detectedYear)
                val octOpening = monthOpeningTx?.resultingBalance
                    ?: sepInterestTx?.resultingBalance
                    ?: (if (lastSepTx != null) lastSepTx.resultingBalance + sepForecast.totalInterest else 1060933.0)
                val octForecast = calculateOctoberForecast(txList, rate, settings, octOpening, detectedYear)
                
                val novOpeningTx = txList.firstOrNull { tx ->
                    (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                    (tx.date.contains("Nov", ignoreCase = true) || tx.date.contains("November", ignoreCase = true))
                }
                val novOpening = novOpeningTx?.resultingBalance
                    ?: octInterestTx?.resultingBalance
                    ?: octForecast.closingWithInterest
                val novForecast = calculateNovemberForecast(txList, rate, settings, novOpening, detectedYear)
                
                val novInterestTx = findInterestTxForMonth("Nov", "November", "Dec", txList)
                val decOpening = novInterestTx?.resultingBalance ?: novForecast.closingWithInterest
                val decForecast = calculateDecemberForecast(txList, rate, settings, decOpening, detectedYear)
                val info = ActiveCycleInfo(
                    activeCycleKey = "NOV_DEC",
                    selectedCycleKey = cycleSelection,
                    thisMonthName = "November $detectedYear",
                    nextMonthName = "December $detectedYear",
                    previousMonthName = "October",
                    isAutoAdvanced = hasOctInterest && cycleSelection == "AUTO",
                    hasSepInterestLogged = hasSepInterest,
                    sepInterestAmount = sepAmount,
                    previousMonthInterestAmount = octInterestTx?.amount ?: octForecast.totalInterest,
                    isPreviousMonthInterestLogged = hasOctInterest,
                    bannerMessage = "Active Cycle: November $detectedYear. Current Month: November $detectedYear • Next Month: December $detectedYear."
                )
                Triple(novForecast, decForecast, info)
            }
            else -> {
                // September (This Month) & October (Next Month)
                val sepForecast = calculateSeptemberForecast(txList, rate, settings, detectedYear)
                val octOpening = if (hasSepInterest) (sepInterestTx?.resultingBalance ?: 1060933.0) else sepForecast.closingWithInterest
                val octForecast = calculateOctoberForecast(txList, rate, settings, octOpening, detectedYear)
                val banner = if (hasSepInterest) {
                    "September interest (${formatCurrency(sepAmount)}) is logged. Active billing cycle is October & November."
                } else {
                    "Active Cycle: September $detectedYear. Today is 01 Oct $detectedYear — awaiting September interest debit from bank. Once you log September interest, the cycle will immediately switch to October & November."
                }
                val info = ActiveCycleInfo(
                    activeCycleKey = "SEP_OCT",
                    selectedCycleKey = cycleSelection,
                    thisMonthName = "September $detectedYear",
                    nextMonthName = "October $detectedYear",
                    previousMonthName = "September",
                    isAutoAdvanced = false,
                    hasSepInterestLogged = hasSepInterest,
                    sepInterestAmount = sepAmount,
                    previousMonthInterestAmount = if (sepAmount > 0.0) sepAmount else sepForecast.totalInterest,
                    isPreviousMonthInterestLogged = hasSepInterest,
                    bannerMessage = banner
                )
                Triple(sepForecast, octForecast, info)
            }
        }
    }

    private fun calculateGenericMonthForecast(
        monthNameOnly: String,
        monthShort: String,
        monthNumber: Int,
        totalDays: Int,
        defaultOpening: Double,
        openingOverride: Double?,
        txList: List<LoanTransaction>,
        rate: Double,
        settings: LoanSettings,
        year: String
    ): MonthInterestBreakdown {
        val monthName = "$monthNameOnly $year"
        val dailyRate = rate / 100.0 / 365.0
        val scheduledEmi = settings.monthlyEmiAmount
        val dueDay = settings.emiDueDayOfMonth.coerceIn(1, min(28, totalDays))

        // Opening principal
        val openingPrincipal = if (openingOverride != null && openingOverride > 0.0) {
            openingOverride
        } else {
            val monthOpeningTx = txList.firstOrNull { tx ->
                (tx.type == TransactionType.OPENING_BALANCE || tx.type == TransactionType.BALANCE_UPDATE) &&
                (tx.date.contains(monthShort, ignoreCase = true) || tx.date.contains(monthNameOnly, ignoreCase = true))
            }
            if (monthOpeningTx != null) {
                monthOpeningTx.resultingBalance
            } else {
                val preMonthTx = txList.lastOrNull { tx ->
                    when (monthNumber) {
                        9 -> !tx.date.contains("Sep", ignoreCase = true) && !tx.date.contains("Oct", ignoreCase = true) && !tx.date.contains("Nov", ignoreCase = true) && !tx.date.contains("Dec", ignoreCase = true)
                        10 -> !tx.date.contains("Oct", ignoreCase = true) && !tx.date.contains("Nov", ignoreCase = true) && !tx.date.contains("Dec", ignoreCase = true)
                        11 -> !tx.date.contains("Nov", ignoreCase = true) && !tx.date.contains("Dec", ignoreCase = true)
                        else -> !tx.date.contains("Dec", ignoreCase = true)
                    }
                }
                preMonthTx?.resultingBalance ?: defaultOpening
            }
        }

        // Find all PAYMENT transactions occurring in this month
        val monthPayments = txList.filter { tx ->
            tx.type == TransactionType.PAYMENT &&
            (tx.date.contains(monthShort, ignoreCase = true) || tx.date.contains(monthNameOnly, ignoreCase = true) || tx.note.contains(monthNameOnly, ignoreCase = true))
        }

        data class PaymentEvent(val day: Int, val amount: Double, val note: String, val isScheduledEmi: Boolean)
        val events = mutableListOf<PaymentEvent>()

        for (tx in monthPayments) {
            val dayMatch = Regex("""\b(\d{1,2})\b""").find(tx.date)
            val day = dayMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(1, totalDays) ?: dueDay
            val isEmi = (day in (dueDay - 2)..(dueDay + 2)) && abs(tx.amount - scheduledEmi) < 1.0
            events.add(PaymentEvent(day = day, amount = tx.amount, note = tx.note.ifBlank { "Payment of ${formatCurrency(tx.amount)}" }, isScheduledEmi = isEmi))
        }

        // If no payment was recorded around dueDay, and openingPrincipal > 0, project the scheduled EMI
        val hasEmiPaidAroundDueDay = events.any { it.day in (dueDay - 2)..(dueDay + 2) }
        if (!hasEmiPaidAroundDueDay && openingPrincipal > 0.0) {
            events.add(PaymentEvent(day = dueDay, amount = scheduledEmi, note = "Scheduled monthly EMI of ${formatCurrency(scheduledEmi)}", isScheduledEmi = true))
        }

        val eventsByDay = events.groupBy { it.day }.toSortedMap()
        val segments = mutableListOf<InterestSegment>()
        var currentPrincipal = openingPrincipal
        var currentDay = 1

        for ((eventDay, dayEvents) in eventsByDay) {
            val daysBefore = eventDay - currentDay
            if (daysBefore > 0) {
                val segDaily = currentPrincipal * dailyRate
                val segTotal = segDaily * daysBefore
                val fromDayStr = "%02d $monthShort".format(currentDay)
                val toDayStr = "%02d $monthShort".format(eventDay - 1)
                segments.add(
                    InterestSegment(
                        title = "Segment ${segments.size + 1}: Days %02d to %02d".format(currentDay, eventDay - 1),
                        datesDesc = "$fromDayStr – $toDayStr ($daysBefore days)",
                        days = daysBefore,
                        principal = currentPrincipal,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = segDaily,
                        totalSegmentInterest = segTotal,
                        eventDescription = ""
                    )
                )
            }

            val totalDayPayment = dayEvents.sumOf { it.amount }
            currentPrincipal = max(0.0, currentPrincipal - totalDayPayment)
            currentDay = eventDay

            val eventDescs = dayEvents.joinToString("; ") { ev ->
                if (ev.isScheduledEmi) {
                    "On %02d $monthShort: Scheduled EMI of %s paid".format(eventDay, formatCurrency(ev.amount))
                } else {
                    "On %02d $monthShort: Extra payoff of %s paid".format(eventDay, formatCurrency(ev.amount))
                }
            }
            if (segments.isNotEmpty()) {
                val lastIdx = segments.size - 1
                segments[lastIdx] = segments[lastIdx].copy(eventDescription = eventDescs)
            }
        }

        val remainingDays = totalDays - currentDay + 1
        if (remainingDays > 0) {
            val segDaily = currentPrincipal * dailyRate
            val segTotal = segDaily * remainingDays
            val fromDayStr = "%02d $monthShort".format(currentDay)
            val toDayStr = "%02d $monthShort".format(totalDays)
            segments.add(
                InterestSegment(
                    title = "Segment ${segments.size + 1}: Days %02d to %d".format(currentDay, totalDays),
                    datesDesc = "$fromDayStr – $toDayStr ($remainingDays days)",
                    days = remainingDays,
                    principal = currentPrincipal,
                    dailyRatePercent = dailyRate * 100.0,
                    dailyInterestAmount = segDaily,
                    totalSegmentInterest = segTotal,
                    eventDescription = "Principal after repayments: ${formatCurrency(currentPrincipal)}"
                )
            )
        }

        val totalInterest = segments.sumOf { it.totalSegmentInterest }
        val closingWithInterest = currentPrincipal + totalInterest
        val preEmiDays = max(0, dueDay - 1)
        val postEmiDays = totalDays - preEmiDays
        val baselineTotal = (openingPrincipal * dailyRate * preEmiDays) + (max(0.0, openingPrincipal - scheduledEmi) * dailyRate * postEmiDays)
        val saved = max(0.0, baselineTotal - totalInterest)

        val hasSep27 = events.any { it.day == 27 && it.amount == 100000.0 }
        val summaryNote = when {
            events.size > 1 -> "${events.size} payments applied in $monthShort • Capitalized Closing: ${formatCurrency(closingWithInterest)}"
            events.isNotEmpty() -> "1 payment applied in $monthShort • Capitalized Closing: ${formatCurrency(closingWithInterest)}"
            else -> "Opening ${formatCurrency(openingPrincipal)} • Scheduled EMI on %02d $monthShort • Capitalized Closing: ${formatCurrency(closingWithInterest)}".format(dueDay)
        }

        return MonthInterestBreakdown(
            monthName = monthName,
            totalDays = totalDays,
            openingPrincipal = openingPrincipal,
            endingPrincipal = currentPrincipal,
            totalInterest = totalInterest,
            baselineInterestWithoutPrepayment = baselineTotal,
            interestSaved = saved,
            segments = segments,
            tenthDayPayoffIncluded = true,
            tenthDayPayoffAmount = scheduledEmi,
            additionalPayoffsSummary = summaryNote,
            hasSep27OneLakhPayoff = hasSep27,
            closingWithInterest = closingWithInterest
        )
    }

    private fun calculateSeptemberForecast(
        txList: List<LoanTransaction>,
        rate: Double,
        settings: LoanSettings = loanPreferences.loadSettings(),
        year: String = "2026"
    ): MonthInterestBreakdown {
        return calculateGenericMonthForecast(
            monthNameOnly = "September",
            monthShort = "Sep",
            monthNumber = 9,
            totalDays = 30,
            defaultOpening = 1175503.0,
            openingOverride = null,
            txList = txList,
            rate = rate,
            settings = settings,
            year = year
        )
    }

    private fun calculateOctoberForecast(
        txList: List<LoanTransaction>,
        rate: Double,
        settings: LoanSettings = loanPreferences.loadSettings(),
        openingOverride: Double? = null,
        year: String = "2026"
    ): MonthInterestBreakdown {
        return calculateGenericMonthForecast(
            monthNameOnly = "October",
            monthShort = "Oct",
            monthNumber = 10,
            totalDays = 31,
            defaultOpening = 1054003.0,
            openingOverride = openingOverride,
            txList = txList,
            rate = rate,
            settings = settings,
            year = year
        )
    }

    private fun calculateNovemberForecast(
        txList: List<LoanTransaction>,
        rate: Double,
        settings: LoanSettings = loanPreferences.loadSettings(),
        octEndingPrincipal: Double = 1032503.0,
        year: String = "2026"
    ): MonthInterestBreakdown {
        return calculateGenericMonthForecast(
            monthNameOnly = "November",
            monthShort = "Nov",
            monthNumber = 11,
            totalDays = 30,
            defaultOpening = 1032503.0,
            openingOverride = octEndingPrincipal,
            txList = txList,
            rate = rate,
            settings = settings,
            year = year
        )
    }

    private fun calculateDecemberForecast(
        txList: List<LoanTransaction>,
        rate: Double,
        settings: LoanSettings = loanPreferences.loadSettings(),
        novEndingPrincipal: Double = 1011003.0,
        year: String = "2026"
    ): MonthInterestBreakdown {
        return calculateGenericMonthForecast(
            monthNameOnly = "December",
            monthShort = "Dec",
            monthNumber = 12,
            totalDays = 31,
            defaultOpening = 1011003.0,
            openingOverride = novEndingPrincipal,
            txList = txList,
            rate = rate,
            settings = settings,
            year = year
        )
    }

    private fun calculateParticularDateCustom(
        custom: ParticularDateCustomState,
        thisMonth: MonthInterestBreakdown,
        nextMonth: MonthInterestBreakdown,
        rate: Double,
        settings: LoanSettings = loanPreferences.loadSettings()
    ): ParticularDateCalculation {
        val dailyRate = rate / 100.0 / 365.0
        val isThisMonth = custom.selectedMonthOption == 0
        val selectedForecast = if (isThisMonth) thisMonth else nextMonth
        val monthName = "${selectedForecast.monthName} (${if (isThisMonth) "This Month" else "Next Month"})"
        val totalDays = selectedForecast.totalDays
        val baseOpeningPrincipal = selectedForecast.openingPrincipal
        val dueDay = settings.emiDueDayOfMonth.coerceIn(1, 28)
        val payDay = custom.dayOfMonth.coerceIn(1, totalDays)
        val payAmount = custom.amountInput.toDoubleOrNull() ?: 100000.0
        val includeTenth = custom.includeTenthDayPayoff
        val defaultEmiForSelectedMonth = selectedForecast.tenthDayPayoffAmount
        val tenthAmount = if (includeTenth) {
            if (custom.tenthDayPayoffAmount > 0 && custom.tenthDayPayoffAmount != 21500.0 && custom.tenthDayPayoffAmount != defaultEmiForSelectedMonth) {
                custom.tenthDayPayoffAmount
            } else {
                defaultEmiForSelectedMonth
            }
        } else 0.0

        val segments = mutableListOf<InterestSegment>()
        var currentBalance = baseOpeningPrincipal

        val preEmiDays = max(0, dueDay - 1)
        val postEmiDays = totalDays - preEmiDays

        // Baseline interest
        val baselineInterest: Double
        if (includeTenth) {
            val basePart1 = baseOpeningPrincipal * dailyRate * preEmiDays.toDouble()
            val basePart2 = max(0.0, baseOpeningPrincipal - tenthAmount) * dailyRate * postEmiDays.toDouble()
            baselineInterest = basePart1 + basePart2
        } else {
            baselineInterest = baseOpeningPrincipal * dailyRate * totalDays.toDouble()
        }

        if (payDay < dueDay && includeTenth) {
            val d1 = max(0, payDay - 1)
            if (d1 > 0) {
                val tot1 = currentBalance * dailyRate * d1
                segments.add(
                    InterestSegment(
                        title = "Segment 1: Days 1 to ${payDay - 1}",
                        datesDesc = "Days 1 to ${payDay - 1} ($d1 days)",
                        days = d1,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot1,
                        eventDescription = "On Day $payDay: Custom payoff of ${formatCurrency(payAmount)} paid"
                    )
                )
            }
            currentBalance = max(0.0, currentBalance - payAmount)

            val d2 = max(0, dueDay - payDay)
            if (d2 > 0) {
                val tot2 = currentBalance * dailyRate * d2
                segments.add(
                    InterestSegment(
                        title = "Segment ${segments.size + 1}: Days $payDay to ${dueDay - 1}",
                        datesDesc = "Days $payDay to ${dueDay - 1} ($d2 days)",
                        days = d2,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot2,
                        eventDescription = "On Day $dueDay: Scheduled payoff of ${formatCurrency(tenthAmount)} paid"
                    )
                )
            }
            currentBalance = max(0.0, currentBalance - tenthAmount)

            val d3 = totalDays - dueDay + 1
            if (d3 > 0) {
                val tot3 = currentBalance * dailyRate * d3
                segments.add(
                    InterestSegment(
                        title = "Segment ${segments.size + 1}: Days $dueDay to $totalDays",
                        datesDesc = "Days $dueDay to $totalDays ($d3 days)",
                        days = d3,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot3,
                        eventDescription = "Month-end closing balance: ${formatCurrency(currentBalance)}"
                    )
                )
            }
        } else if (payDay == dueDay && includeTenth) {
            val d1 = max(0, dueDay - 1)
            if (d1 > 0) {
                val tot1 = currentBalance * dailyRate * d1
                segments.add(
                    InterestSegment(
                        title = "Segment 1: Days 1 to ${dueDay - 1}",
                        datesDesc = "Days 1 to ${dueDay - 1} ($d1 days)",
                        days = d1,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot1,
                        eventDescription = "On Day $dueDay: Scheduled payoff (${formatCurrency(tenthAmount)}) + Custom payoff (${formatCurrency(payAmount)})"
                    )
                )
            }
            currentBalance = max(0.0, currentBalance - tenthAmount - payAmount)
            val d2 = totalDays - dueDay + 1
            if (d2 > 0) {
                val tot2 = currentBalance * dailyRate * d2
                segments.add(
                    InterestSegment(
                        title = "Segment ${segments.size + 1}: Days $dueDay to $totalDays",
                        datesDesc = "Days $dueDay to $totalDays ($d2 days)",
                        days = d2,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot2,
                        eventDescription = "Month-end closing balance: ${formatCurrency(currentBalance)}"
                    )
                )
            }
        } else if (includeTenth) {
            val d1 = max(0, dueDay - 1)
            if (d1 > 0) {
                val tot1 = currentBalance * dailyRate * d1
                segments.add(
                    InterestSegment(
                        title = "Segment 1: Days 1 to ${dueDay - 1}",
                        datesDesc = "Days 1 to ${dueDay - 1} ($d1 days)",
                        days = d1,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot1,
                        eventDescription = "On Day $dueDay: Scheduled ${dueDay}th payoff of ${formatCurrency(tenthAmount)} paid"
                    )
                )
            }
            currentBalance = max(0.0, currentBalance - tenthAmount)

            val d2 = max(0, payDay - dueDay)
            if (d2 > 0) {
                val tot2 = currentBalance * dailyRate * d2
                segments.add(
                    InterestSegment(
                        title = "Segment 2: Days $dueDay to ${payDay - 1}",
                        datesDesc = "Days $dueDay to ${payDay - 1} ($d2 days)",
                        days = d2,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot2,
                        eventDescription = "On Day $payDay: Custom payoff of ${formatCurrency(payAmount)} paid"
                    )
                )
            }
            currentBalance = max(0.0, currentBalance - payAmount)

            val d3 = totalDays - payDay + 1
            if (d3 > 0) {
                val tot3 = currentBalance * dailyRate * d3
                segments.add(
                    InterestSegment(
                        title = "Segment 3: Days $payDay to $totalDays",
                        datesDesc = "Days $payDay to $totalDays ($d3 days)",
                        days = d3,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot3,
                        eventDescription = "Month-end closing balance: ${formatCurrency(currentBalance)}"
                    )
                )
            }
        } else {
            val d1 = max(0, payDay - 1)
            if (d1 > 0) {
                val tot1 = currentBalance * dailyRate * d1
                segments.add(
                    InterestSegment(
                        title = "Segment 1: Days 1 to ${payDay - 1}",
                        datesDesc = "Days 1 to ${payDay - 1} ($d1 days)",
                        days = d1,
                        principal = currentBalance,
                        dailyRatePercent = dailyRate * 100.0,
                        dailyInterestAmount = currentBalance * dailyRate,
                        totalSegmentInterest = tot1,
                        eventDescription = "On Day $payDay: Custom payoff of ${formatCurrency(payAmount)} paid"
                    )
                )
            }
            currentBalance = max(0.0, currentBalance - payAmount)
            val d2 = totalDays - payDay + 1
            val tot2 = currentBalance * dailyRate * d2
            segments.add(
                InterestSegment(
                    title = "Segment 2: Days $payDay to $totalDays",
                    datesDesc = "Days $payDay to $totalDays ($d2 days)",
                    days = d2,
                    principal = currentBalance,
                    dailyRatePercent = dailyRate * 100.0,
                    dailyInterestAmount = currentBalance * dailyRate,
                    totalSegmentInterest = tot2,
                    eventDescription = "Month-end closing balance: ${formatCurrency(currentBalance)}"
                )
            )
        }

        val newTotalInterest = segments.sumOf { it.totalSegmentInterest }
        val interestSavedInMonth = max(0.0, baselineInterest - newTotalInterest)
        val permanentDailySaving = payAmount * dailyRate

        return ParticularDateCalculation(
            targetMonthName = monthName,
            totalDays = totalDays,
            basePrincipal = baseOpeningPrincipal,
            payDateDay = payDay,
            payAmount = payAmount,
            includeTenthDay = includeTenth,
            tenthDayAmount = tenthAmount,
            newTotalInterest = newTotalInterest,
            baselineInterest = baselineInterest,
            interestSavedInMonth = interestSavedInMonth,
            permanentDailySaving = permanentDailySaving,
            endingPrincipal = currentBalance,
            segments = segments
        )
    }

    companion object {
        fun formatCurrency(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
            formatter.maximumFractionDigits = 0
            formatter.minimumFractionDigits = 0
            return "₹" + formatter.format(amount.roundToInt())
        }

        fun formatCurrencyDecimals(amount: Double): String {
            val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
            formatter.maximumFractionDigits = 2
            formatter.minimumFractionDigits = 0
            return "₹" + formatter.format(amount)
        }
    }
}
