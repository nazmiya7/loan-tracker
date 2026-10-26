package com.example.data

import kotlinx.coroutines.flow.Flow

class LoanRepository(private val dao: TransactionDao) {

    val allTransactionsAsc: Flow<List<LoanTransaction>> = dao.getAllTransactionsAsc()
    val allTransactionsDesc: Flow<List<LoanTransaction>> = dao.getAllTransactionsDesc()

    suspend fun insertTransaction(transaction: LoanTransaction): Long {
        return dao.insert(transaction)
    }

    suspend fun updateTransaction(transaction: LoanTransaction) {
        dao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: LoanTransaction) {
        dao.delete(transaction)
        recalculateBalances()
    }

    suspend fun deleteTransactionById(id: Long) {
        dao.deleteById(id)
        recalculateBalances()
    }

    suspend fun recalculateBalances() {
        val list = dao.getAllList()
        if (list.isEmpty()) return

        var runningBalance = list.first().resultingBalance
        val updatedList = mutableListOf<LoanTransaction>()

        for (i in list.indices) {
            val tx = list[i]
            if (i == 0) {
                runningBalance = tx.resultingBalance
            } else {
                runningBalance = when (tx.type) {
                    TransactionType.PAYMENT -> kotlin.math.max(0.0, runningBalance - tx.amount)
                    TransactionType.INTEREST_CHARGED -> runningBalance + tx.amount
                    TransactionType.OPENING_BALANCE -> tx.amount
                    TransactionType.BALANCE_UPDATE -> tx.amount
                }
                if (tx.resultingBalance != runningBalance) {
                    updatedList.add(tx.copy(resultingBalance = runningBalance))
                }
            }
        }
        if (updatedList.isNotEmpty()) {
            dao.updateAll(updatedList)
        }
    }

    suspend fun resetToDefault() {
        dao.deleteAll()
        dao.insertAll(SampleData.initialTransactions)
    }

    suspend fun ensurePopulated() {
        if (dao.getCount() == 0) {
            dao.insertAll(SampleData.initialTransactions)
        } else {
            val list = dao.getAllList()
            val toUpdate = mutableListOf<LoanTransaction>()
            for (tx in list) {
                if (tx.date.contains("2024") || tx.note.contains("2024")) {
                    toUpdate.add(
                        tx.copy(
                            date = tx.date.replace("2024", "2026"),
                            note = tx.note.replace("2024", "2026")
                        )
                    )
                }
            }
            if (toUpdate.isNotEmpty()) {
                dao.updateAll(toUpdate)
            }
        }
    }
}
