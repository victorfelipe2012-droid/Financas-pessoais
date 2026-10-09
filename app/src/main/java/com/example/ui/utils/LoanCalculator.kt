package com.example.ui.utils

import com.example.data.FinanceItem
import com.example.data.LoanPayment

/**
 * Utilitário centralizado e compartilhado para cálculo de saldos e valores a receber de empréstimos.
 * Garante que a mesma regra de saldo (principal menos amortizações válidas) seja utilizada
 * no Dashboard, na listagem de empréstimos e no modal de detalhes.
 */
object LoanCalculator {

    /**
     * Calcula o saldo restante a receber de um empréstimo específico:
     * Saldo = max(0, principal - soma dos pagamentos estruturados)
     * Se o empréstimo estiver quitado, o saldo restante é zero.
     */
    fun calculateRemainingBalanceCents(loan: FinanceItem, payments: List<LoanPayment>): Long {
        if (loan.isCompleted) return 0L
        val loanPayments = payments.filter { it.loanId == loan.id }
        val totalPaid = loanPayments.sumOf { it.amountCents }
        return (loan.amountCents - totalPaid).coerceAtLeast(0L)
    }

    /**
     * Calcula o total amortizado/pago de um empréstimo específico.
     */
    fun calculateTotalPaidCents(loan: FinanceItem, payments: List<LoanPayment>): Long {
        return payments.filter { it.loanId == loan.id }.sumOf { it.amountCents }
    }

    /**
     * Calcula a soma total a receber de todos os empréstimos ativos (abertos).
     */
    fun calculateTotalReceivableCents(loans: List<FinanceItem>, payments: List<LoanPayment>): Long {
        return loans
            .filter { it.type == "LENT" && !it.isCompleted }
            .sumOf { loan -> calculateRemainingBalanceCents(loan, payments) }
    }
}
