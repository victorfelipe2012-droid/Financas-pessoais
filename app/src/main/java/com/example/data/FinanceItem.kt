package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.ui.utils.MoneyUtils
import com.squareup.moshi.JsonClass

@Entity(
    tableName = "finance_items",
    indices = [
        Index(value = ["recurringBillId", "competence"])
    ]
)
@JsonClass(generateAdapter = true)
data class FinanceItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amountCents: Long = 0L,
    val type: String, // "SALARY", "INVESTMENT", "BOX", "LENT", "BILL", "APARTMENT", "CHALLENGE", "CHALLENGE_ARCHIVED"
    val category: String = "",
    val date: Long = System.currentTimeMillis(), // Data de lançamento
    val description: String = "",
    val isCompleted: Boolean = false, // Pago / Devolvido / Concluído
    val targetAmountCents: Long = 0L, // Meta em centavos
    val dueDate: Long? = null, // Data de vencimento
    val paymentDate: Long? = null, // Data do pagamento efetivo
    val recurringBillId: Long? = null, // Vínculo estruturado com a regra de recorrência
    val competence: String? = null, // Competência mensal (ex: "2026-10")
    val isHistoryMigrated: Boolean = false // Marcador persistente de migração para impedir remigração após estorno
) {
    val amount: Double
        get() = amountCents / 100.0

    val targetAmount: Double
        get() = targetAmountCents / 100.0

    constructor(
        id: Int = 0,
        title: String,
        amount: Double,
        type: String,
        category: String = "",
        date: Long = System.currentTimeMillis(),
        description: String = "",
        isCompleted: Boolean = false,
        targetAmount: Double = 0.0,
        dueDate: Long? = null,
        paymentDate: Long? = null
    ) : this(
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
        paymentDate = paymentDate,
        recurringBillId = null,
        competence = null,
        isHistoryMigrated = false
    )
}
