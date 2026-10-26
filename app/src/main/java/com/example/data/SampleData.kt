package com.example.data

object SampleData {
    val initialTransactions = listOf(
        LoanTransaction(
            id = 1,
            date = "30 Jun 2026",
            type = TransactionType.INTEREST_CHARGED,
            amount = 9604.0,
            resultingBalance = 1592956.0,
            note = "June Interest charged (Closing balance: 1,592,956)"
        ),
        LoanTransaction(
            id = 2,
            date = "31 Jul 2026",
            type = TransactionType.PAYMENT,
            amount = 295000.0,
            resultingBalance = 1297956.0,
            note = "Monthly Repayment (Balance drops to 1,297,956)"
        ),
        LoanTransaction(
            id = 3,
            date = "31 Jul 2026",
            type = TransactionType.INTEREST_CHARGED,
            amount = 9884.0,
            resultingBalance = 1307840.0,
            note = "July Interest charged (Balance: 1,307,840)"
        ),
        LoanTransaction(
            id = 4,
            date = "09 Aug 2026",
            type = TransactionType.PAYMENT,
            amount = 97000.0,
            resultingBalance = 1210840.0,
            note = "Part Payment (Resulting balance: 1,210,840)"
        ),
        LoanTransaction(
            id = 5,
            date = "26 Aug 2026",
            type = TransactionType.PAYMENT,
            amount = 43000.0,
            resultingBalance = 1167840.0,
            note = "Part Payment (21,500 + 21,500 = 43,000)"
        ),
        LoanTransaction(
            id = 6,
            date = "31 Aug 2026",
            type = TransactionType.INTEREST_CHARGED,
            amount = 7663.0,
            resultingBalance = 1175503.0,
            note = "August Month-End Interest charged"
        ),
        LoanTransaction(
            id = 7,
            date = "10 Sep 2026",
            type = TransactionType.PAYMENT,
            amount = 21500.0,
            resultingBalance = 1154003.0,
            note = "Payment Made (10th-day payoff of 21,500, balance drops to 1,154,003)"
        ),
        LoanTransaction(
            id = 8,
            date = "27 Sep 2026",
            type = TransactionType.PAYMENT,
            amount = 100000.0,
            resultingBalance = 1054003.0,
            note = "Part Payment / Payoff (1 Lakh paid on 27 Sep, balance drops to 1,054,003)"
        )
    )
}
