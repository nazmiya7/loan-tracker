package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [LoanTransaction::class], version = 1, exportSchema = false)
abstract class LoanDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: LoanDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): LoanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LoanDatabase::class.java,
                    "loan_tracker_database"
                )
                    .addCallback(LoanDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class LoanDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database.transactionDao())
                }
            }
        }

        suspend fun populateInitialData(dao: TransactionDao) {
            if (dao.getCount() == 0) {
                dao.insertAll(SampleData.initialTransactions)
            }
        }
    }
}
