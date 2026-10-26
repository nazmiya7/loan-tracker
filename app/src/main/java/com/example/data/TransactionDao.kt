package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM loan_transactions ORDER BY id ASC")
    fun getAllTransactionsAsc(): Flow<List<LoanTransaction>>

    @Query("SELECT * FROM loan_transactions ORDER BY id DESC")
    fun getAllTransactionsDesc(): Flow<List<LoanTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: LoanTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<LoanTransaction>)

    @Update
    suspend fun update(transaction: LoanTransaction)

    @Update
    suspend fun updateAll(transactions: List<LoanTransaction>)

    @Delete
    suspend fun delete(transaction: LoanTransaction)

    @Query("DELETE FROM loan_transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM loan_transactions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM loan_transactions")
    suspend fun getCount(): Int

    @Query("SELECT * FROM loan_transactions ORDER BY id ASC")
    suspend fun getAllList(): List<LoanTransaction>
}
