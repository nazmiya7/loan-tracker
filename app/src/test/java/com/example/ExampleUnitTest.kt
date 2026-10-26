package com.example

import com.example.ui.LoanViewModel
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDailyReducingInterestFormula() {
        val principal = 1167840.0
        val rate = 7.35
        val days = 31

        // Principal * (Rate / 100 / 365) * Days
        val interest = principal * (rate / 100.0 / 365.0) * days
        assertTrue("Interest should be approximately 7289.85", interest in 7280.0..7300.0)

        val newPrincipal = 1067840.0 // After 100,000 prepayment
        val newInterest = newPrincipal * (rate / 100.0 / 365.0) * days
        val saved = interest - newInterest
        assertTrue("Saved monthly interest should be approximately 624.13", saved in 620.0..630.0)
    }

    @Test
    fun testCurrencyFormatting() {
        val formatted = LoanViewModel.formatCurrency(1167840.0)
        assertTrue(formatted.contains("11,67,840") || formatted.contains("1,167,840"))
    }

    @Test
    fun testEmiValuationPayoffTenure() {
        val principal = 1054003.0
        val rate = 7.35
        val monthlyRate = (rate / 100.0) / 12.0

        // Base EMI = 21,500
        val baseEmi = 21500.0
        val baseMonthsDouble = -kotlin.math.ln(1.0 - (principal * monthlyRate) / baseEmi) / kotlin.math.ln(1.0 + monthlyRate)
        val baseMonths = Math.round(baseMonthsDouble).toInt()
        assertTrue("Payoff tenure at ₹21,500 should be ~58-59 months", baseMonths in 58..60)

        // Updated higher EMI = 30,000
        val newEmi = 30000.0
        val newMonthsDouble = -kotlin.math.ln(1.0 - (principal * monthlyRate) / newEmi) / kotlin.math.ln(1.0 + monthlyRate)
        val newMonths = Math.round(newMonthsDouble).toInt()
        assertTrue("Higher EMI of 30,000 should significantly reduce payoff months to ~39-41", newMonths in 38..42)
        assertTrue("Tenure reduction should be at least 17 months", (baseMonths - newMonths) >= 17)
    }

    @Test
    fun testEmiTransitionFromNextMonthPolicy() {
        val settings = com.example.data.LoanSettings(
            monthlyEmiAmount = 30000.0,
            annualRatePercent = 7.35,
            emiDueDayOfMonth = 10,
            previousEmiAmount = 21500.0,
            effectiveFromNextMonth = true
        )

        // When effectiveFromNextMonth is true:
        // This Month (September) should resolve to previous EMI (₹21,500)
        val thisMonthEmi = if (settings.effectiveFromNextMonth) settings.previousEmiAmount else settings.monthlyEmiAmount
        org.junit.Assert.assertEquals(21500.0, thisMonthEmi, 0.001)

        // Next Month (October) resolves to the new EMI (₹30,000)
        val nextMonthEmi = settings.monthlyEmiAmount
        org.junit.Assert.assertEquals(30000.0, nextMonthEmi, 0.001)

        // Future remaining amortization calculates based on new EMI
        val principal = 1054003.0
        val monthlyRate = (settings.annualRatePercent / 100.0) / 12.0
        val newTenureDouble = -kotlin.math.ln(1.0 - (principal * monthlyRate) / nextMonthEmi) / kotlin.math.ln(1.0 + monthlyRate)
        val newTenure = Math.round(newTenureDouble).toInt()
        assertTrue(newTenure in 38..42)
    }

    @Test
    fun testAiAdvisorPrepaymentAnalysisWithLiveContext() {
        val context = com.example.data.LoanLiveContext(
            currentOutstandingPrincipal = 926450.0,
            annualRatePercent = 8.5,
            dailyRatePercent = 8.5 / 365.0,
            dailyInterestToday = (926450.0 * 0.085) / 365.0,
            monthlyEmi = 21500.0,
            scheduledDueDay = 10,
            totalPaymentsMade = 250000.0,
            totalInterestCharged = 75000.0,
            principalReductionPercent = 25.5,
            estimatedPayoffMonths = 54,
            estimatedPayoffCompletionDateText = "Dec 2028",
            estimatedPayoffYearsMonthsText = "4 yrs 6 mos",
            thisMonthEstimatedInterest = 6500.0,
            recentTransactionsSummary = "No recent transactions"
        )

        val response = com.example.data.LocalLoanExpertEngine.generateExpertResponse(
            "What if I prepay ₹50,000 next month?",
            context
        )

        assertTrue("Response should contain prepayment analysis", response.contains("Prepayment Impact"))
        assertTrue("Response should contain principal reduction info", response.contains("9,26,450") || response.contains("926,450"))
        assertTrue("Response should calculate daily savings", response.contains("Daily Savings") || response.contains("/day"))
    }

    @Test
    fun testAiAdvisorDailyFormulaProofWithLiveContext() {
        val context = com.example.data.LoanLiveContext(
            currentOutstandingPrincipal = 926450.0,
            annualRatePercent = 8.5,
            dailyRatePercent = 8.5 / 365.0,
            dailyInterestToday = (926450.0 * 0.085) / 365.0,
            monthlyEmi = 21500.0,
            scheduledDueDay = 10,
            totalPaymentsMade = 250000.0,
            totalInterestCharged = 75000.0,
            principalReductionPercent = 25.5,
            estimatedPayoffMonths = 54,
            estimatedPayoffCompletionDateText = "Dec 2028",
            estimatedPayoffYearsMonthsText = "4 yrs 6 mos",
            thisMonthEstimatedInterest = 6500.0,
            recentTransactionsSummary = "No recent transactions"
        )

        val response = com.example.data.LocalLoanExpertEngine.generateExpertResponse(
            "Explain my daily interest calculation proof",
            context
        )

        assertTrue("Response should contain formula breakdown", response.contains("Daily-Reducing Interest Formula") || response.contains("SBI"))
        assertTrue("Response should reference actual interest rate", response.contains("8.5"))
    }
    @Test
    fun testDynamicNextMonthInterestSegmentCalculation() {
        val rate = 7.35
        val dailyRate = (rate / 100.0) / 365.0
        val scheduledEmi = 21500.0

        // October 2026 Calculation (31 days)
        // Opening balance after September 27 prepayment
        val octOpening = 1054003.0
        val octDaysBeforeEmi = 9 // Days 1 to 9
        val octDaysAfterEmi = 22 // Days 10 to 31
        val octBalanceAfterEmi = octOpening - scheduledEmi // 1,032,503

        val octSeg1Interest = octOpening * dailyRate * octDaysBeforeEmi
        val octSeg2Interest = octBalanceAfterEmi * dailyRate * octDaysAfterEmi
        val octTotalInterest = octSeg1Interest + octSeg2Interest

        assertTrue("October Segment 1 (9d) should be ~1910.45", octSeg1Interest in 1900.0..1920.0)
        assertTrue("October Segment 2 (22d) should be ~4574.65", octSeg2Interest in 4560.0..4590.0)
        assertTrue("October Total interest should dynamically be ~6485.10", octTotalInterest in 6470.0..6500.0)

        // Prepayment in October: What if user prepays ₹50,000 on Oct 20?
        val days10to19 = 10
        val days20to31 = 12
        val balAfterPrepay = octBalanceAfterEmi - 50000.0 // 982,503
        val octNewInterest = (octOpening * dailyRate * octDaysBeforeEmi) +
                             (octBalanceAfterEmi * dailyRate * days10to19) +
                             (balAfterPrepay * dailyRate * days20to31)
        val octSaved = octTotalInterest - octNewInterest
        assertTrue("Prepaying ₹50,000 on Oct 20 must dynamically save interest in October", octSaved > 100.0)

        // November 2026 Calculation (30 days) dynamically chained
        val novOpening = balAfterPrepay
        val novDaysBeforeEmi = 9
        val novDaysAfterEmi = 21
        val novBalanceAfterEmi = novOpening - scheduledEmi // 961,003
        val novTotalInterest = (novOpening * dailyRate * novDaysBeforeEmi) +
                               (novBalanceAfterEmi * dailyRate * novDaysAfterEmi)

        assertTrue("November interest must dynamically be lower than October", novTotalInterest < octNewInterest)
        assertTrue("November interest should be ~5800-6000", novTotalInterest in 5800.0..6050.0)
    }

    @Test
    fun testDynamicMonthCycleProgression() {
        // Test automatic cycle keys and progression
        fun computeCycle(hasSepInterest: Boolean, hasAnyOctTx: Boolean, hasOctInterest: Boolean, hasAnyNovTx: Boolean): String {
            return when {
                hasOctInterest || hasAnyNovTx -> "NOV_DEC"
                hasSepInterest || hasAnyOctTx -> "OCT_NOV"
                else -> "SEP_OCT"
            }
        }

        // Initially: awaiting September interest
        org.junit.Assert.assertEquals("SEP_OCT", computeCycle(false, false, false, false))

        // September interest logged: advances to October & November
        org.junit.Assert.assertEquals("OCT_NOV", computeCycle(true, false, false, false))

        // October transaction added: stays or moves to October & November
        org.junit.Assert.assertEquals("OCT_NOV", computeCycle(false, true, false, false))

        // October interest logged: advances to November & December
        org.junit.Assert.assertEquals("NOV_DEC", computeCycle(true, true, true, false))

        // November transaction added: advances to November & December
        org.junit.Assert.assertEquals("NOV_DEC", computeCycle(true, true, false, true))
    }

    @Test
    fun testSeptemberDailyReducingInterestAndBankCapitalization() {
        val rate = 7.35
        val dailyRate = (rate / 100.0) / 365.0
        val sep1Principal = 1175503.0

        // Segment 1: Sep 1 to Sep 9 (9 days)
        val seg1Days = 9
        val seg1Interest = sep1Principal * dailyRate * seg1Days
        assertTrue("Sep Segment 1 should be ~2,129.58", seg1Interest in 2125.0..2135.0)

        // On Sep 10: payment of 21,500
        val sep10Principal = sep1Principal - 21500.0 // 1,154,003
        org.junit.Assert.assertEquals(1154003.0, sep10Principal, 0.001)

        // Segment 2: Sep 10 to Sep 26 (17 days)
        val seg2Days = 17
        val seg2Interest = sep10Principal * dailyRate * seg2Days
        assertTrue("Sep Segment 2 should be ~3,950.48", seg2Interest in 3945.0..3955.0)

        // On Sep 27: extra payoff of 100,000
        val sep27Principal = sep10Principal - 100000.0 // 1,054,003
        org.junit.Assert.assertEquals(1054003.0, sep27Principal, 0.001)

        // Segment 3: Sep 27 to Sep 30 (4 days)
        val seg3Days = 4
        val seg3Interest = sep27Principal * dailyRate * seg3Days
        assertTrue("Sep Segment 3 should be ~848.97", seg3Interest in 845.0..855.0)

        // Total September Interest
        val totalSepInterest = seg1Interest + seg2Interest + seg3Interest
        assertTrue("Total September interest should be ~6,929.03 matching Bank's ₹6,930", totalSepInterest in 6925.0..6935.0)

        // Month-End Capitalization on Sep 30: Bank debits interest to principal
        val bankActualInterest = 6930.0
        val sepClosingBalance = sep27Principal + bankActualInterest
        org.junit.Assert.assertEquals(1060933.0, sepClosingBalance, 0.001)
    }

    @Test
    fun testOctoberDailyReducingInterestAndCapitalizationScenarioA() {
        // User scenario: Oct 1 principal is 1,039,437, Oct 10 pays 21,500 reducing to 1,017,937
        val rate = 7.35
        val dailyRate = (rate / 100.0) / 365.0
        val oct1Principal = 1039437.0

        // Segment 1: Oct 1 to Oct 9 (9 days)
        val seg1Days = 9
        val seg1Interest = oct1Principal * dailyRate * seg1Days
        assertTrue("Oct Segment 1 (9d on 1,039,437) should be ~1,883.80", seg1Interest in 1880.0..1888.0)

        // On Oct 10: 21,500 EMI paid -> Principal reduced to 1,017,937
        val oct10Principal = oct1Principal - 21500.0
        org.junit.Assert.assertEquals(1017937.0, oct10Principal, 0.001)

        // Segment 2: Oct 10 to Oct 31 (22 days)
        val seg2Days = 22
        val seg2Interest = oct10Principal * dailyRate * seg2Days
        assertTrue("Oct Segment 2 (22d on 1,017,937) should be ~4,509.60", seg2Interest in 4505.0..4515.0)

        // Total October calculated interest
        val totalOctInterest = seg1Interest + seg2Interest
        assertTrue("Total October interest should be ~6,393.40", totalOctInterest in 6390.0..6396.0)

        // Month-End Capitalization on Oct 31: Interest is added to principal
        val octClosingCapitalized = oct10Principal + totalOctInterest
        assertTrue("Oct Closing Capitalized Principal should be ~1,024,330.40", octClosingCapitalized in 1024325.0..1024335.0)

        // November Starting Principal chains from October Capitalized Closing Balance
        val novOpeningPrincipal = octClosingCapitalized
        val novSeg1Days = 9
        val novSeg1Interest = novOpeningPrincipal * dailyRate * novSeg1Days
        val novBalanceAfterEmi = novOpeningPrincipal - 21500.0 // ~1,002,830.40
        val novSeg2Days = 21 // 30 - 9 = 21 days
        val novSeg2Interest = novBalanceAfterEmi * dailyRate * novSeg2Days
        val totalNovInterest = novSeg1Interest + novSeg2Interest

        assertTrue("November interest should be ~6,095", totalNovInterest in 6080.0..6120.0)
    }

    @Test
    fun testOctoberDailyReducingInterestAndCapitalizationScenarioB() {
        // Direct chain from September 30 capitalized balance of 1,060,933
        val rate = 7.35
        val dailyRate = (rate / 100.0) / 365.0
        val oct1Principal = 1060933.0

        // Segment 1: Oct 1 to Oct 9 (9 days)
        val seg1Days = 9
        val seg1Interest = oct1Principal * dailyRate * seg1Days
        assertTrue("Oct Segment 1 on 1,060,933 should be ~1,922.75", seg1Interest in 1920.0..1926.0)

        // On Oct 10: 21,500 EMI paid -> Principal reduced to 1,039,433
        val oct10Principal = oct1Principal - 21500.0
        org.junit.Assert.assertEquals(1039433.0, oct10Principal, 0.001)

        // Segment 2: Oct 10 to Oct 31 (22 days)
        val seg2Days = 22
        val seg2Interest = oct10Principal * dailyRate * seg2Days
        assertTrue("Oct Segment 2 on 1,039,433 should be ~4,604.83", seg2Interest in 4600.0..4610.0)

        // Total October calculated interest
        val totalOctInterest = seg1Interest + seg2Interest
        assertTrue("Total October interest should be ~6,527.59", totalOctInterest in 6520.0..6535.0)

        // Month-End Capitalization on Oct 31
        val octClosingCapitalized = oct10Principal + totalOctInterest
        assertTrue("Oct Closing Capitalized Principal should be ~1,045,960.59", octClosingCapitalized in 1045950.0..1045970.0)
    }

    @Test
    fun testPayoffMonthAndYearVerification() {
        val principal = 1054003.0
        val rate = 7.35
        val monthlyRate = (rate / 100.0) / 12.0
        val normalEmi = 21500.0

        // Amortization formula: N = -ln(1 - (P * r) / EMI) / ln(1 + r)
        val numMonthsDouble = -kotlin.math.ln(1.0 - (principal * monthlyRate) / normalEmi) / kotlin.math.ln(1.0 + monthlyRate)
        val estimatedMonths = Math.round(numMonthsDouble).toInt()

        org.junit.Assert.assertEquals(58, estimatedMonths)

        // Convert to Years and Months: 58 months = 4 years and 10 months
        val years = estimatedMonths / 12
        val remMonths = estimatedMonths % 12
        org.junit.Assert.assertEquals(4, years)
        org.junit.Assert.assertEquals(10, remMonths)

        // Calendar Completion Date Calculation from October 2026
        val cal = java.util.Calendar.getInstance()
        cal.set(2026, java.util.Calendar.OCTOBER, 1)
        cal.add(java.util.Calendar.MONTH, estimatedMonths)

        val completionDateText = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.ENGLISH).format(cal.time)
        org.junit.Assert.assertEquals("Aug 2031", completionDateText)

        // Scenario with higher EMI = ₹30,000
        val higherEmi = 30000.0
        val higherMonthsDouble = -kotlin.math.ln(1.0 - (principal * monthlyRate) / higherEmi) / kotlin.math.ln(1.0 + monthlyRate)
        val higherMonths = Math.round(higherMonthsDouble).toInt()
        org.junit.Assert.assertEquals(40, higherMonths) // 3 yrs 4 mos

        val higherCal = java.util.Calendar.getInstance()
        higherCal.set(2026, java.util.Calendar.OCTOBER, 1)
        higherCal.add(java.util.Calendar.MONTH, higherMonths)
        val higherCompletionDate = java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.ENGLISH).format(higherCal.time)
        org.junit.Assert.assertEquals("Feb 2030", higherCompletionDate)

        // Tenure saved
        val tenureSavedMonths = estimatedMonths - higherMonths
        org.junit.Assert.assertEquals(18, tenureSavedMonths)
    }

    @Test
    fun testPreviousMonthInterestDateCalculation() {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val fullMonthNames = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

        fun computeLastDayDate(monthLabel: String, currentYear: Int = 2026): String {
            val matchedMonthIdx = fullMonthNames.indexOfFirst { it.equals(monthLabel, ignoreCase = true) }
                .takeIf { it >= 0 } ?: monthNames.indexOfFirst { it.equals(monthLabel, ignoreCase = true) }
            val labelCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.YEAR, currentYear)
                set(java.util.Calendar.MONTH, matchedMonthIdx)
                set(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            val labelLastDay = labelCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            val labelShort = monthNames[matchedMonthIdx]
            return "$labelLastDay $labelShort $currentYear"
        }

        // September interest must be logged on the last day of September: 30 Sep 2026
        org.junit.Assert.assertEquals("30 Sep 2026", computeLastDayDate("September"))

        // October interest must be logged on the last day of October: 31 Oct 2026
        org.junit.Assert.assertEquals("31 Oct 2026", computeLastDayDate("October"))

        // November interest must be logged on the last day of November: 30 Nov 2026
        org.junit.Assert.assertEquals("30 Nov 2026", computeLastDayDate("November"))

        // February 2027 interest must be logged on 28 Feb 2027
        org.junit.Assert.assertEquals("28 Feb 2027", computeLastDayDate("February", 2027))
    }

    @Test
    fun testPayoffSimulatorCalculationAndDynamicMonthValues() {
        val seedTransactions = listOf(
            com.example.data.LoanTransaction(1L, "01 Sep 2026", com.example.data.TransactionType.OPENING_BALANCE, 1175503.0, 1175503.0, "Opening balance"),
            com.example.data.LoanTransaction(2L, "10 Sep 2026", com.example.data.TransactionType.PAYMENT, 21500.0, 1154003.0, "EMI Payment"),
            com.example.data.LoanTransaction(3L, "27 Sep 2026", com.example.data.TransactionType.PAYMENT, 100000.0, 1054003.0, "Part Payment"),
            com.example.data.LoanTransaction(4L, "30 Sep 2026", com.example.data.TransactionType.INTEREST_CHARGED, 6930.0, 1060933.0, "Sep interest debit"),
            com.example.data.LoanTransaction(5L, "10 Oct 2026", com.example.data.TransactionType.PAYMENT, 21500.0, 1039433.0, "Oct EMI Payment")
        )

        val latestBalance = seedTransactions.last().resultingBalance
        org.junit.Assert.assertEquals(1039433.0, latestBalance, 0.01)

        // Adding a new transaction: e.g. Extra payment of ₹21,496 on 14 Oct reducing balance to ₹10,17,937
        val newTx = com.example.data.LoanTransaction(6L, "14 Oct 2026", com.example.data.TransactionType.PAYMENT, 21496.0, 1017937.0, "Principal prepayment")
        val updatedList = seedTransactions + newTx
        val updatedBalance = updatedList.last().resultingBalance
        org.junit.Assert.assertEquals(1017937.0, updatedBalance, 0.01)

        val rate = 7.35
        val dailyRate = rate / 100.0 / 365.0
        val dailyInterest = updatedBalance * dailyRate
        assertTrue("Daily interest on 10,17,937 at 7.35% should be ~204.98", dailyInterest in 204.9..205.1)

        // 15 days accrued interest
        val accrued15Days = dailyInterest * 15
        assertTrue("15 days accrued interest should be ~3,074.7", accrued15Days in 3070.0..3080.0)

        // Settlement Payoff amount
        val payoffAmount = updatedBalance + accrued15Days
        assertTrue("Payoff settlement should be ~10,21,011.7", payoffAmount in 1021000.0..1021030.0)

        // Monthly saving from ₹1,00,000 extra prepayment
        val extra = 100000.0
        val dailySaved = extra * dailyRate
        val monthlySaved30Days = dailySaved * 30
        assertTrue("Daily saved should be ~20.14", dailySaved in 20.1..20.2)
        assertTrue("Monthly saved in 30 days should be ~604.1", monthlySaved30Days in 603.5..604.5)

        // Amortization tenure calculation on ₹1,017,937 with EMI ₹21,500
        val monthlyRate = (rate / 100.0) / 12.0
        val emi = 21500.0
        val nMonths = -kotlin.math.ln(1.0 - (updatedBalance * monthlyRate) / emi) / kotlin.math.ln(1.0 + monthlyRate)
        val regularMonths = Math.round(nMonths).toInt()
        org.junit.Assert.assertEquals(56, regularMonths) // 4 years 8 months

        // With extra prepayment of ₹1,00,000 (new principal = 917,937)
        val prepaidPrincipal = updatedBalance - extra
        val nPrepaid = -kotlin.math.ln(1.0 - (prepaidPrincipal * monthlyRate) / emi) / kotlin.math.ln(1.0 + monthlyRate)
        val prepaidMonths = Math.round(nPrepaid).toInt()
        org.junit.Assert.assertEquals(50, prepaidMonths) // 4 years 2 months

        val monthsSaved = regularMonths - prepaidMonths
        org.junit.Assert.assertEquals(6, monthsSaved) // 6 months saved

        // Output 1: Current Month Interest (October 2026, 31 Days)
        val octPart1 = (updatedBalance + emi) * dailyRate * 9
        val octPart2 = updatedBalance * dailyRate * 22
        val octBaseline = octPart1 + octPart2
        assertTrue("Oct Baseline Interest should be ~6393.41", octBaseline in 6390.0..6396.0)

        val octSavedWithPrepay = extra * dailyRate * 17 // 17 days from Oct 15 to Oct 31
        val octWithPrepay = octBaseline - octSavedWithPrepay
        assertTrue("Oct Interest with prepay should be ~6051.08", octWithPrepay in 6048.0..6054.0)

        // Output 2: Next Month Interest (November 2026, 30 Days) with Oct capitalized interest
        val novOpeningBaseline = updatedBalance + octBaseline // 1,024,330.41
        val novPart1Baseline = novOpeningBaseline * dailyRate * 9
        val novBalanceAfterEmi = novOpeningBaseline - emi // 1,002,830.41
        val novPart2Baseline = novBalanceAfterEmi * dailyRate * 21
        val novBaseline = novPart1Baseline + novPart2Baseline
        assertTrue("Nov Baseline Interest with Oct interest capitalized should be ~6097.53", novBaseline in 6095.0..6100.0)

        val novOpeningPrepay = (updatedBalance - extra) + octWithPrepay // 923,988.08
        val novPart1Prepay = novOpeningPrepay * dailyRate * 9
        val novBalanceAfterEmiPrepay = novOpeningPrepay - emi // 902,488.08
        val novPart2Prepay = novBalanceAfterEmiPrepay * dailyRate * 21
        val novWithPrepay = novPart1Prepay + novPart2Prepay
        assertTrue("Nov Interest with prepay should be ~5490.94", novWithPrepay in 5488.0..5493.0)

        val novMonthlySaving = novBaseline - novWithPrepay
        assertTrue("Nov Monthly Interest Saving should be ~606.59", novMonthlySaving in 605.0..608.0)
    }
}

