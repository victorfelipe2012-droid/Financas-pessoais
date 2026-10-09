package com.example

import com.example.data.FinanceItem
import com.example.ui.utils.MonthlyFinanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class MonthlyFinanceCalculatorTest {

    private fun createTimestamp(year: Int, month: Int, day: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    @Test
    fun testRealizedCashflowAndPendingCalculations() {
        val octDate = createTimestamp(2026, 10, 5)
        val sepDate = createTimestamp(2026, 9, 20)
        val octDueDate = createTimestamp(2026, 10, 10)
        val octOverdueDate = createTimestamp(2026, 10, 1) // Before ref time
        val referenceTime = createTimestamp(2026, 10, 8)

        val items = listOf(
            // Receita em Outubro: R$ 5.000,00 (500000 centavos)
            FinanceItem(id = 1, title = "Salário", type = "SALARY", amountCents = 500000L, date = octDate),
            
            // Receita em Setembro: não deve entrar no mês de Outubro
            FinanceItem(id = 2, title = "Salário Setembro", type = "SALARY", amountCents = 500000L, date = sepDate),
            
            // Despesa Moradia Paga em Outubro: R$ 1.500,00 (150000 centavos)
            FinanceItem(id = 3, title = "Aluguel", type = "APARTMENT", amountCents = 150000L, date = octDate, isCompleted = true),
            
            // Conta de Luz Paga em Outubro: R$ 250,00 (25000 centavos)
            FinanceItem(id = 4, title = "Luz", type = "BILL", amountCents = 25000L, date = octDate, isCompleted = true),
            
            // Conta de Água Pendente em Outubro: R$ 80,00 (8000 centavos), vence dia 10 (não vencida)
            FinanceItem(id = 5, title = "Água", type = "BILL", amountCents = 8000L, date = octDate, dueDate = octDueDate, isCompleted = false),
            
            // Conta de Internet Vencida dia 01/10: R$ 120,00 (12000 centavos)
            FinanceItem(id = 6, title = "Internet", type = "BILL", amountCents = 12000L, date = octDate, dueDate = octOverdueDate, isCompleted = false),

            // Empréstimo Ativo concedido: R$ 1.000,00 (100000 centavos)
            FinanceItem(id = 7, title = "Empréstimo João", type = "LENT", amountCents = 100000L, date = octDate, isCompleted = false),

            // Caixinha: R$ 3.000,00 (300000 centavos)
            FinanceItem(id = 8, title = "Reserva Emergência", type = "BOX", amountCents = 300000L, date = octDate),

            // Investimento: R$ 10.000,00 (1000000 centavos)
            FinanceItem(id = 9, title = "Tesouro Selic", type = "INVESTMENT", amountCents = 1000000L, date = octDate),

            // Desafio 52 Semanas: Não deve entrar no fluxo de caixa operacional
            FinanceItem(id = 10, title = "Semana 40", type = "CHALLENGE", amountCents = 4000L, date = octDate)
        )

        val summary = MonthlyFinanceCalculator.calculateSummary(items, 2026, 10, referenceTime)

        // 1. Receitas realizadas = 5000.00
        assertEquals(500000L, summary.realizedIncomeCents)

        // 2. Despesas pagas = 1500.00 + 250.00 = 1750.00
        assertEquals(175000L, summary.realizedExpensesCents)

        // 3. Fluxo de caixa realizado = 5000.00 - 1750.00 = 3250.00
        assertEquals(325000L, summary.realizedNetCashflowCents)

        // 4. Contas a pagar no mês (água + internet) = 80.00 + 120.00 = 200.00
        assertEquals(20000L, summary.pendingBillsCents)

        // 5. Contas vencidas (internet apenas, vencimento 01/10 < ref 08/10) = 120.00
        assertEquals(12000L, summary.overdueBillsCents)

        // 6. Empréstimos a receber = 1000.00
        assertEquals(100000L, summary.activeLoansReceivableCents)

        // 7. Caixinhas = 3000.00, Investimentos = 10000.00, Total = 13000.00
        assertEquals(300000L, summary.totalBoxesCents)
        assertEquals(1000000L, summary.totalInvestmentsCents)
        assertEquals(1300000L, summary.totalInvestedAndBoxesCents)
    }

    @Test
    fun testMonthFilteringExcludesChallengeAndDifferentMonths() {
        val sepDate = createTimestamp(2026, 9, 30)
        val octDate = createTimestamp(2026, 10, 1)

        val items = listOf(
            FinanceItem(id = 1, title = "Item Setembro", type = "BILL", amountCents = 100L, date = sepDate),
            FinanceItem(id = 2, title = "Item Outubro", type = "BILL", amountCents = 200L, date = octDate),
            FinanceItem(id = 3, title = "Desafio Outubro", type = "CHALLENGE", amountCents = 300L, date = octDate)
        )

        val filteredOct = MonthlyFinanceCalculator.filterItemsByMonth(items, 2026, 10)
        assertEquals(1, filteredOct.size)
        assertEquals(2, filteredOct[0].id)
    }

    @Test
    fun testActiveLoansReceivableCalculatesPrincipalMinusStructuredPayments() {
        val octDate = createTimestamp(2026, 10, 5)
        val items = listOf(
            FinanceItem(id = 1, title = "Empréstimo A", type = "LENT", amountCents = 50000L, date = octDate, isCompleted = false),
            FinanceItem(id = 2, title = "Empréstimo B Quitada", type = "LENT", amountCents = 30000L, date = octDate, isCompleted = true)
        )
        val payments = listOf(
            com.example.data.LoanPayment(id = 101L, loanId = 1, amountCents = 20000L, paymentDate = octDate, note = "Parcial")
        )

        val summary = MonthlyFinanceCalculator.calculateSummary(items, payments, 2026, 10)
        // Principal 500 - pagamento 200 = saldo a receber 300 (30000L). Empréstimo B quitado é ignorado.
        assertEquals(30000L, summary.activeLoansReceivableCents)
    }
}
