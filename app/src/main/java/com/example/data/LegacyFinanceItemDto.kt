package com.example.data

import com.example.ui.utils.MoneyUtils
import com.squareup.moshi.JsonClass

/**
 * DTO para desserialização fiel do formato de backup legado v1 (onde amount e targetAmount eram Doubles em reais).
 */
@JsonClass(generateAdapter = true)
data class LegacyFinanceItemDto(
    val id: Int = 0,
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val date: Long = 0L,
    val description: String = "",
    val isCompleted: Boolean = false,
    val targetAmount: Double = 0.0,
    val dueDate: Long? = null,
    val paymentDate: Long? = null
) {
    fun toFinanceItem(): FinanceItem {
        return FinanceItem(
            id = id,
            title = title,
            amountCents = MoneyUtils.toCents(amount),
            type = type,
            category = category,
            date = date,
            description = description,
            isCompleted = isCompleted,
            targetAmountCents = MoneyUtils.toCents(targetAmount),
            dueDate = dueDate,
            paymentDate = paymentDate ?: if (isCompleted) date else null,
            isHistoryMigrated = false
        )
    }
}
