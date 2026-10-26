package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loan_transactions")
data class LoanTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // e.g. "2026-06-30" or "Jun 30, 2026"
    val type: TransactionType,
    val amount: Double,
    val resultingBalance: Double,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
