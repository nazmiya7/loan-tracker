package com.example.data

enum class ChatSender {
    USER,
    AI
}

data class ChatMessage(
    val id: String,
    val sender: ChatSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isContextAttached: Boolean = true,
    val isError: Boolean = false,
    val isGeminiLive: Boolean = false
)

data class LoanLiveContext(
    val currentOutstandingPrincipal: Double,
    val annualRatePercent: Double,
    val dailyRatePercent: Double,
    val dailyInterestToday: Double,
    val monthlyEmi: Double,
    val scheduledDueDay: Int,
    val totalPaymentsMade: Double,
    val totalInterestCharged: Double,
    val principalReductionPercent: Double,
    val estimatedPayoffMonths: Int,
    val estimatedPayoffCompletionDateText: String,
    val estimatedPayoffYearsMonthsText: String,
    val thisMonthEstimatedInterest: Double,
    val recentTransactionsSummary: String
)
