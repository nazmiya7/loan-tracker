package com.example.data

enum class TransactionType(val displayName: String) {
    OPENING_BALANCE("Opening Principal"),
    PAYMENT("Payment / Credit"),
    INTEREST_CHARGED("Interest Debit"),
    BALANCE_UPDATE("Balance Adjustment");

    val isPayment: Boolean
        get() = this == PAYMENT

    val isInterest: Boolean
        get() = this == INTEREST_CHARGED
}
