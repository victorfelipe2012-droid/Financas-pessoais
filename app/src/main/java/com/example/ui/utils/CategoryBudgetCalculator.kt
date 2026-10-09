package com.example.ui.utils

import com.example.data.CategoryBudget
import com.example.data.FinanceItem
import java.util.Calendar

data class CategoryBudgetStatus(
    val category: String,
    val limitCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val percentageUsed: Int // 0 a 100+
)

object CategoryBudgetCalculator {

    /**
     * Calcula o status orçamentário da categoria para um determinado mês e ano.
     * Regra Contábil Estrita:
     * - Apenas despesas do mês entram no cômputo (BILL e APARTMENT).
     * - Exclui estritamente: transferências internas, aportes em caixinhas (BOX), investimentos (INVESTMENT),
     *   empréstimos concedidos (LENT) e receitas/salários (SALARY).
     */
    fun calculateCategoryStatus(
        budget: CategoryBudget,
        items: List<FinanceItem>,
        year: Int,
        month: Int // 1 a 12
    ): CategoryBudgetStatus {
        val cal = Calendar.getInstance()

        val matchingExpenses = items.filter { item ->
            // Apenas despesas operacionais da categoria indicada
            if (item.type != "BILL" && item.type != "APARTMENT") return@filter false
            if (!item.category.trim().equals(budget.category.trim(), ignoreCase = true)) return@filter false

            // Considera gasto realizado (concluído/pago) no mês
            if (!item.isCompleted) return@filter false

            val targetDate = item.paymentDate ?: item.dueDate ?: item.date
            cal.timeInMillis = targetDate
            cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
        }

        val spentCents = matchingExpenses.sumOf { it.amountCents }
        val remainingCents = budget.limitCents - spentCents
        val percentage = if (budget.limitCents > 0L) {
            ((spentCents.toDouble() / budget.limitCents.toDouble()) * 100).toInt()
        } else 0

        return CategoryBudgetStatus(
            category = budget.category,
            limitCents = budget.limitCents,
            spentCents = spentCents,
            remainingCents = remainingCents,
            percentageUsed = percentage
        )
    }
}
