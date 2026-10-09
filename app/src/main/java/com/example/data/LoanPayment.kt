package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "loan_payments",
    indices = [Index(value = ["loanId"])]
)
@JsonClass(generateAdapter = true)
data class LoanPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val loanId: Int,
    val amountCents: Long,
    val paymentDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
