package com.example.ui.utils

import com.example.data.FinanceItem
import java.util.Calendar

data class MonthlyFinancialSummary(
    val year: Int,
    val month: Int, // 1 a 12
    val realizedIncomeCents: Long,
    val realizedExpensesCents: Long,
    val realizedNetCashflowCents: Long, // Receitas Realizadas - Despesas Pagas
    val pendingBillsCents: Long,
    val overdueBillsCents: Long,
    val pendingApartmentCents: Long,
    val activeLoansReceivableCents: Long,
    val totalBoxesCents: Long,
    val totalInvestmentsCents: Long,
    val totalInvestedAndBoxesCents: Long
)

object MonthlyFinanceCalculator {

    /**
     * Calcula o resumo financeiro do mês selecionado.
     * Centraliza todas as regras contábeis fora dos Composables.
     */
    fun calculateSummary(
        items: List<FinanceItem>,
        year: Int,
        month: Int, // 1-12
        referenceTime: Long = System.currentTimeMillis()
    ): MonthlyFinancialSummary {
        val monthItems = filterItemsByMonth(items, year, month)

        // 1. Receitas Realizadas (Salários recebidos no mês)
        val realizedIncome = monthItems
            .filter { it.type == "SALARY" }
            .sumOf { it.amountCents }

        // 2. Despesas Pagas no mês (Apartamento pago + Contas pagas)
        val realizedApartment = monthItems
            .filter { it.type == "APARTMENT" && it.isCompleted }
            .sumOf { it.amountCents }

        val realizedBills = monthItems
            .filter { it.type == "BILL" && it.isCompleted }
            .sumOf { it.amountCents }

        val totalRealizedExpenses = realizedApartment + realizedBills

        // 3. Saldo Realizado do Período (Fluxo de caixa operacional)
        val realizedNetCashflow = realizedIncome - totalRealizedExpenses

        // 4. Contas a Pagar no mês (pendentes)
        val pendingBills = monthItems
            .filter { it.type == "BILL" && !it.isCompleted }
            .sumOf { it.amountCents }

        // 5. Contas Vencidas (vencimento anterior ao momento de referência e pendentes)
        val overdueBills = items
            .filter { it.type == "BILL" && !it.isCompleted && (it.dueDate ?: it.date) < referenceTime }
            .sumOf { it.amountCents }

        // 6. Apartamento pendente
        val pendingApartment = monthItems
            .filter { it.type == "APARTMENT" && !it.isCompleted }
            .sumOf { it.amountCents }

        // 7. Empréstimos a receber (ativos)
        val activeLoans = items
            .filter { it.type == "LENT" && !it.isCompleted }
            .sumOf { it.amountCents }

        // 8. Patrimônio acumulado em Caixinhas e Investimentos
        val boxesTotal = items
            .filter { it.type == "BOX" }
            .sumOf { it.amountCents }

        val investmentsTotal = items
            .filter { it.type == "INVESTMENT" }
            .sumOf { it.amountCents }

        return MonthlyFinancialSummary(
            year = year,
            month = month,
            realizedIncomeCents = realizedIncome,
            realizedExpensesCents = totalRealizedExpenses,
            realizedNetCashflowCents = realizedNetCashflow,
            pendingBillsCents = pendingBills,
            overdueBillsCents = overdueBills,
            pendingApartmentCents = pendingApartment,
            activeLoansReceivableCents = activeLoans,
            totalBoxesCents = boxesTotal,
            totalInvestmentsCents = investmentsTotal,
            totalInvestedAndBoxesCents = boxesTotal + investmentsTotal
        )
    }

    /**
     * Filtra itens cuja competência (data do lançamento ou vencimento) pertença ao ano/mês solicitado.
     */
    fun filterItemsByMonth(items: List<FinanceItem>, year: Int, month: Int): List<FinanceItem> {
        val cal = Calendar.getInstance()
        return items.filter { item ->
            // Exclui Desafio 52 Semanas e registros arquivados do fluxo de caixa mensal
            if (item.type == "CHALLENGE" || item.type == "CHALLENGE_ARCHIVED") return@filter false

            val targetDate = item.paymentDate ?: item.dueDate ?: item.date
            cal.timeInMillis = targetDate
            cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
        }
    }
}
