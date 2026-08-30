package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allItems: Flow<List<FinanceItem>> = financeDao.getAllItems()

    fun getItemsByType(type: String): Flow<List<FinanceItem>> = financeDao.getItemsByType(type)

    suspend fun insertItem(item: FinanceItem): Long = financeDao.insertItem(item)

    suspend fun insertAll(items: List<FinanceItem>) = financeDao.insertAll(items)

    suspend fun updateItem(item: FinanceItem) = financeDao.updateItem(item)

    suspend fun deleteItem(item: FinanceItem) = financeDao.deleteItem(item)

    suspend fun deleteItemById(id: Int) = financeDao.deleteItemById(id)

    suspend fun clearAll() = financeDao.clearAll()
}
