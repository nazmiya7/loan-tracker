package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Loan & Interest Tracker", appName)
    }

    @Test
    fun launchMainActivity() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        org.junit.Assert.assertNotNull(activity)
    }

    @Test
    fun verifyEmiEffectiveFromNextMonthLogic() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.data.LoanPreferences(context)
        val initialSettings = prefs.loadSettings()
        assertEquals(21500.0, initialSettings.monthlyEmiAmount, 0.01)

        // Save new EMI of 30,000 starting from next month
        val updatedSettings = com.example.data.LoanSettings(
            monthlyEmiAmount = 30000.0,
            annualRatePercent = 7.35,
            emiDueDayOfMonth = 10,
            effectiveFromNextMonth = true,
            previousEmiAmount = 21500.0
        )
        prefs.saveSettings(updatedSettings)

        val loaded = prefs.loadSettings()
        assertEquals(30000.0, loaded.monthlyEmiAmount, 0.01)
        assertEquals(21500.0, loaded.previousEmiAmount, 0.01)
        org.junit.Assert.assertTrue(loaded.effectiveFromNextMonth)
    }

    @Test
    fun verifyMonthRollForwardWhenSeptemberInterestIsLogged() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)
        val initialTxList = com.example.data.SampleData.initialTransactions
        val settings = com.example.data.LoanSettings()

        // 1. Before Sep interest is logged: Active cycle must be September 2026 & October 2026
        val (sepMonth, octMonth, initialInfo) = vm.calculateCyclePair(initialTxList, 7.35, settings, "AUTO")
        assertEquals("September 2026", sepMonth.monthName)
        assertEquals(30, sepMonth.totalDays)
        assertEquals("October 2026", octMonth.monthName)
        assertEquals(31, octMonth.totalDays)
        org.junit.Assert.assertFalse(initialInfo.hasSepInterestLogged)

        // 2. Now user logs September interest debit (e.g. 7,143 on 30 Sep)
        val updatedListWithSepInterest = initialTxList + listOf(
            com.example.data.LoanTransaction(
                id = 9,
                date = "30 Sep 2026",
                type = com.example.data.TransactionType.INTEREST_CHARGED,
                amount = 7143.0,
                resultingBalance = 1061146.0,
                note = "September Month-End Interest charged"
            )
        )

        // 3. After Sep interest is logged: Active cycle must immediately become October 2026 & November 2026!
        val (thisMonthAfter, nextMonthAfter, afterInfo) = vm.calculateCyclePair(updatedListWithSepInterest, 7.35, settings, "AUTO")
        assertEquals("October 2026", thisMonthAfter.monthName)
        assertEquals(31, thisMonthAfter.totalDays)
        assertEquals(1061146.0, thisMonthAfter.openingPrincipal, 0.01)

        assertEquals("November 2026", nextMonthAfter.monthName)
        assertEquals(30, nextMonthAfter.totalDays)
        org.junit.Assert.assertTrue(afterInfo.hasSepInterestLogged)
        assertEquals(7143.0, afterInfo.sepInterestAmount, 0.01)
    }

    @Test
    fun verifyAiAdvisorLiveContextGeneration() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)
        val context = vm.buildLiveLoanContext()
        org.junit.Assert.assertNotNull(context)
        org.junit.Assert.assertTrue(context.currentOutstandingPrincipal > 0.0)
        org.junit.Assert.assertTrue(context.annualRatePercent > 0.0)
        org.junit.Assert.assertTrue(context.dailyInterestToday > 0.0)
        org.junit.Assert.assertTrue(context.monthlyEmi > 0.0)
        org.junit.Assert.assertTrue(context.recentTransactionsSummary.isNotBlank())
        org.junit.Assert.assertTrue(context.estimatedPayoffCompletionDateText.isNotBlank())
    }

    @Test
    fun verifyAiAdvisorPrepaymentAndDailyProofExpertQueries() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)
        val context = vm.buildLiveLoanContext()

        // 1. Prepayment doubt
        val prepayResp = com.example.data.LocalLoanExpertEngine.generateExpertResponse(
            "What if I prepay ₹50,000 next month?",
            context
        )
        org.junit.Assert.assertTrue(prepayResp.contains("Prepayment Impact Analysis"))
        org.junit.Assert.assertTrue(prepayResp.contains("Daily Interest Reduction"))

        // 2. Daily reducing interest proof
        val dailyResp = com.example.data.LocalLoanExpertEngine.generateExpertResponse(
            "Step-by-step daily interest calculation proof",
            context
        )
        org.junit.Assert.assertTrue(dailyResp.contains("Daily-Reducing Interest Formula"))
        org.junit.Assert.assertTrue(dailyResp.contains(context.annualRatePercent.toString()))

        // 3. Payment timing doubt
        val timingResp = com.example.data.LocalLoanExpertEngine.generateExpertResponse(
            "Why is paying on the 5th better than the 25th?",
            context
        )
        org.junit.Assert.assertTrue(timingResp.contains("Payment Timing"))
        org.junit.Assert.assertTrue(timingResp.contains("5th vs the 25th"))
    }

    @Test
    fun verifyTransactionAddAndDeleteBalanceRecalculation() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)
        val initialTxList = com.example.data.SampleData.initialTransactions
        val settings = com.example.data.LoanSettings()

        val initialSummary = vm.computeDashboardSummary(initialTxList, settings)
        val initialBalance = initialSummary.currentOutstandingPrincipal
        val lastTx = initialTxList.last()
        assertEquals(lastTx.resultingBalance, initialBalance, 0.01)

        // Add a prepayment of 50,000 using PAYMENT transaction type
        val prepaymentTx = com.example.data.LoanTransaction(
            id = 999,
            date = "15 Oct 2026",
            type = com.example.data.TransactionType.PAYMENT,
            amount = 50000.0,
            resultingBalance = initialBalance - 50000.0,
            note = "Extra principal payment"
        )
        val listWithPrepayment = initialTxList + prepaymentTx
        val updatedSummary = vm.computeDashboardSummary(listWithPrepayment, settings)

        assertEquals(initialTxList.size + 1, listWithPrepayment.size)
        assertEquals(initialBalance - 50000.0, updatedSummary.currentOutstandingPrincipal, 0.01)
        assertEquals(initialSummary.totalPaymentsMade + 50000.0, updatedSummary.totalPaymentsMade, 0.01)
        org.junit.Assert.assertTrue(updatedSummary.principalReductionPercent > initialSummary.principalReductionPercent)

        // Verify removing/deleting transaction restores original balance
        val restoredList = listWithPrepayment.filter { it.id != 999L }
        val restoredSummary = vm.computeDashboardSummary(restoredList, settings)
        assertEquals(initialTxList.size, restoredList.size)
        assertEquals(initialBalance, restoredSummary.currentOutstandingPrincipal, 0.01)
    }

    @Test
    fun verifyPrepaymentSimulatorTenureSavings() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)
        vm.updatePrincipalInput("1054003")
        vm.updateAnnualRate(7.35)
        vm.updateExtraPaymentInput("100000")
        vm.updateRegularMonthlyPayment("21500")

        val simResult = vm.simulatorResult.value
        org.junit.Assert.assertNotNull(simResult)
        org.junit.Assert.assertTrue("Total lifetime interest saved should be positive", simResult.totalLifetimeInterestSaved > 0.0)
        org.junit.Assert.assertTrue("Tenure months saved should be positive", simResult.monthsSaved > 0)
        org.junit.Assert.assertTrue(simResult.prepaidPayoffDateText.isNotBlank())
    }

    @Test
    fun verifyParticularDateAccrualCalculation() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)
        vm.updateParticularDateDay(15)
        vm.updateParticularDateMonth(0)

        val result = vm.particularDateResult.value
        org.junit.Assert.assertNotNull(result)
        org.junit.Assert.assertTrue(result.newTotalInterest > 0.0)
        org.junit.Assert.assertTrue(result.totalDays in 28..31)
    }

    @Test
    fun verifyNextMonthDynamicInterestCalculationAndCycleAdvance() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = com.example.ui.LoanViewModel(app)

        // Initial cycle is SEP_OCT awaiting September interest
        val initialCycle = vm.activeCycleInfo.value
        org.junit.Assert.assertEquals("SEP_OCT", initialCycle.activeCycleKey)
        org.junit.Assert.assertFalse(initialCycle.hasSepInterestLogged)
        org.junit.Assert.assertTrue("Pending interest must be dynamic > 6000", vm.reminderStatusInfo.value.pendingInterestAmount > 6000.0)

        // Log September interest
        vm.logPreviousMonthInterest(monthLabel = "September")

        // Cycle automatically advances to OCT_NOV
        val advancedCycle = vm.activeCycleInfo.value
        org.junit.Assert.assertEquals("OCT_NOV", advancedCycle.activeCycleKey)
        org.junit.Assert.assertTrue(advancedCycle.hasSepInterestLogged)

        // Verify October forecast has dynamic interest calculated with 10th EMI
        val octForecast = vm.thisMonthForecast.value
        org.junit.Assert.assertTrue(octForecast.monthName.contains("October"))
        org.junit.Assert.assertTrue("October interest should be between 6000 and 7000", octForecast.totalInterest in 6000.0..7000.0)
        org.junit.Assert.assertTrue(octForecast.tenthDayPayoffIncluded)

        // Verify November forecast is chained dynamically
        val novForecast = vm.nextMonthForecast.value
        org.junit.Assert.assertTrue(novForecast.monthName.contains("November"))
        org.junit.Assert.assertTrue("November interest should be less than October due to principal reduction", novForecast.totalInterest < octForecast.totalInterest)
    }
}
