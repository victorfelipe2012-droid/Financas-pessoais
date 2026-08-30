package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_items ORDER BY date DESC")
    fun getAllItems(): Flow<List<FinanceItem>>

    @Query("SELECT * FROM finance_items WHERE type = :type ORDER BY date DESC")
    fun getItemsByType(type: String): Flow<List<FinanceItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: FinanceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FinanceItem>)

    @Update
    suspend fun updateItem(item: FinanceItem)

    @Delete
    suspend fun deleteItem(item: FinanceItem)

    @Query("DELETE FROM finance_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM finance_items")
    suspend fun clearAll()
}
