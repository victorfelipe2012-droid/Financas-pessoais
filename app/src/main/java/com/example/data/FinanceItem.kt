package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "finance_items")
@JsonClass(generateAdapter = true)
data class FinanceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val type: String, // "SALARY", "INVESTMENT", "BOX", "LENT", "BILL"
    val category: String, // e.g. "CDB", "Luz", "Lazer", "João"
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val isCompleted: Boolean = false, // true for Paid Bill or Repaid Lent money
    val targetAmount: Double = 0.0, // for BOX goals
    val dueDate: Long? = null // for BILLS due date
)
