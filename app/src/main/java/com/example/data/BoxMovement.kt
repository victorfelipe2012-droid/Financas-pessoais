package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "box_movements",
    indices = [Index(value = ["boxId"])]
)
@JsonClass(generateAdapter = true)
data class BoxMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val boxId: Int,
    val amountCents: Long,
    val isDeposit: Boolean, // true = aporte / depósito, false = resgate / saque
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
