package com.example

import com.example.data.FinanceItem
import com.example.data.LoanPayment
import com.example.ui.screens.LentMovementParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanBusinessRulesTest {

    @Test
    fun testStructuredPaymentsWithIndependentIds() {
        val loan = FinanceItem(
            id = 10,
            title = "Empréstimo Teste",
            type = "LENT",
            amountCents = 100000L, // R$ 1.000,00
            targetAmountCents = 100000L,
            date = 1700000000000L
        )

        val payments = listOf(
            LoanPayment(id = 101L, loanId = 10, amountCents = 20000L, paymentDate = 1700010000000L, note = "Abatimento 1"),
            // Dois pagamentos de mesmo valor e mesma data são independentes com IDs distintos
            LoanPayment(id = 102L, loanId = 10, amountCents = 20000L, paymentDate = 1700010000000L, note = "Abatimento 2")
        )

        val movements = LentMovementParser.getMovements(loan, payments)

        // Deve conter a concessão inicial + 2 pagamentos = 3 movimentos
        assertEquals(3, movements.size)

        // Concessão
        assertEquals("creation_10", movements[0].id)
        assertTrue(movements[0].isCreation)
        assertEquals(100000L, movements[0].amountCents)
        assertEquals(1700000000000L, movements[0].date)

        // Pagamentos independentes
        assertEquals("payment_101", movements[1].id)
        assertEquals(101L, movements[1].paymentId)
        assertEquals(20000L, movements[1].amountCents)
        assertFalse(movements[1].isCreation)

        assertEquals("payment_102", movements[2].id)
        assertEquals(102L, movements[2].paymentId)
        assertEquals(20000L, movements[2].amountCents)
        assertFalse(movements[2].isCreation)

        // Saldo restante: 1000.00 - 400.00 = 600.00 (60000 centavos)
        val totalPaid = payments.sumOf { it.amountCents }
        val remaining = loan.targetAmountCents - totalPaid
        assertEquals(60000L, remaining)
    }

    @Test
    fun testLegacyDescriptionFallbackParser() {
        val legacyLoan = FinanceItem(
            id = 20,
            title = "Empréstimo Legado",
            type = "LENT",
            amountCents = 30000L, // Saldo restante R$ 300,00
            date = 1700000000000L,
            description = "Abatido R$ 100,00 em 10/10/2025 - Parcela 1\nAbatido R$ 100,00 em 11/10/2025 - Parcela 2"
        )

        val movements = LentMovementParser.getMovements(legacyLoan, emptyList())
        assertTrue("Deve processar movimentos textuais legados", movements.isNotEmpty())
        assertTrue(movements.any { it.isCreation })
        assertTrue(movements.any { it.note.contains("Parcela 1") })
    }
}
