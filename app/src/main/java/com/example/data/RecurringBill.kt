package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "recurring_bills")
@JsonClass(generateAdapter = true)
data class RecurringBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val category: String,
    val amountCents: Long,
    val dueDay: Int, // 1 a 31
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val type: String = "BILL", // "BILL" ou "APARTMENT"
    val createdAt: Long = System.currentTimeMillis()
)
