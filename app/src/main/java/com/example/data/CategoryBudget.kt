package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "category_budgets")
@JsonClass(generateAdapter = true)
data class CategoryBudget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val category: String,
    val limitCents: Long,
    val monthCompetence: String = "DEFAULT", // "DEFAULT" ou "YYYY-MM"
    val createdAt: Long = System.currentTimeMillis()
)
