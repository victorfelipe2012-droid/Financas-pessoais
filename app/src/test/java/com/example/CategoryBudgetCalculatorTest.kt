package com.example

import com.example.data.CategoryBudget
import com.example.data.FinanceItem
import com.example.ui.utils.CategoryBudgetCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CategoryBudgetCalculatorTest {

    private fun createTimestamp(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testBudgetSpendingExcludesTransfersAndDeposits() {
        val octDate = createTimestamp(2026, 10, 5)

        val budgetAlimentacao = CategoryBudget(category = "Alimentação", limitCents = 100000L) // R$ 1.000,00

        val items = listOf(
            // Despesa realizada de Alimentação: R$ 350,00
            FinanceItem(id = 1, title = "Mercado", type = "BILL", category = "Alimentação", amountCents = 35000L, date = octDate, isCompleted = true),

            // Despesa realizada de Alimentação: R$ 150,00
            FinanceItem(id = 2, title = "Feira", type = "BILL", category = "Alimentação", amountCents = 15000L, date = octDate, isCompleted = true),

            // Despesa pendente (ainda não realizada): R$ 100,00 - NÃO deve entrar como gasto realizado
            FinanceItem(id = 3, title = "Padaria Pendente", type = "BILL", category = "Alimentação", amountCents = 10000L, date = octDate, isCompleted = false),

            // Aporte em Caixinha categorizado como "Alimentação": DEVE SER EXCLUÍDO
            FinanceItem(id = 4, title = "Reserva Alimentação", type = "BOX", category = "Alimentação", amountCents = 20000L, date = octDate, isCompleted = true),

            // Investimento categorizado como "Alimentação": DEVE SER EXCLUÍDO
            FinanceItem(id = 5, title = "Aporte Bolsa", type = "INVESTMENT", category = "Alimentação", amountCents = 50000L, date = octDate, isCompleted = true),

            // Empréstimo concedido: DEVE SER EXCLUÍDO
            FinanceItem(id = 6, title = "Empréstimo Amigo", type = "LENT", category = "Alimentação", amountCents = 30000L, date = octDate, isCompleted = true),

            // Receita / Salário: DEVE SER EXCLUÍDO
            FinanceItem(id = 7, title = "Bônus", type = "SALARY", category = "Alimentação", amountCents = 80000L, date = octDate, isCompleted = true)
        )

        val status = CategoryBudgetCalculator.calculateCategoryStatus(
            budget = budgetAlimentacao,
            items = items,
            year = 2026,
            month = 10
        )

        // Gasto realizado: 350,00 + 150,00 = 500,00 (50000 centavos)
        assertEquals(50000L, status.spentCents)

        // Limite: 1000,00 (100000 centavos)
        assertEquals(100000L, status.limitCents)

        // Restante: 500,00 (50000 centavos)
        assertEquals(50000L, status.remainingCents)

        // Percentual utilizado: 50%
        assertEquals(50, status.percentageUsed)
    }

    @Test
    fun testBudgetOverspent() {
        val octDate = createTimestamp(2026, 10, 10)
        val budgetTransporte = CategoryBudget(category = "Transporte", limitCents = 20000L) // R$ 200,00

        val items = listOf(
            FinanceItem(id = 1, title = "Combustível", type = "BILL", category = "Transporte", amountCents = 25000L, date = octDate, isCompleted = true)
        )

        val status = CategoryBudgetCalculator.calculateCategoryStatus(
            budget = budgetTransporte,
            items = items,
            year = 2026,
            month = 10
        )

        assertEquals(25000L, status.spentCents)
        assertEquals(-5000L, status.remainingCents)
        assertEquals(125, status.percentageUsed)
    }
}
