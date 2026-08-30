package com.example.sync

import com.example.data.FinanceItem
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SyncPayload(
    val deviceName: String,
    val timestamp: Long,
    val items: List<FinanceItem>,
    val apartmentSubcategories: List<String>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    val success: Boolean,
    val message: String,
    val serverItems: List<FinanceItem>,
    val serverApartmentSubcategories: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class EncryptedSyncEnvelope(
    val iv: String,
    val salt: String,
    val cipherText: String
)
