package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayloadV2(
    val version: Int = 2,
    val exportedAt: Long = System.currentTimeMillis(),
    val appVersion: String = "2.0",
    val items: List<FinanceItem> = emptyList(),
    val loanPayments: List<LoanPayment> = emptyList(),
    val boxMovements: List<BoxMovement> = emptyList(),
    val recurringBills: List<RecurringBill> = emptyList(),
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val apartmentSubcategories: List<String> = emptyList(),
    val metadata: List<AppMetadata> = emptyList()
)
