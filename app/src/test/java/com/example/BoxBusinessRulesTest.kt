package com.example

import com.example.data.BoxMovement
import com.example.data.FinanceItem
import com.example.ui.screens.BoxCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class BoxBusinessRulesTest {

    @Test
    fun testBalanceCalculatedFromMovements() {
        val box = FinanceItem(
            id = 5,
            title = "Viagem",
            type = "BOX",
            amountCents = 0L,
            targetAmountCents = 200000L // Meta R$ 2.000,00
        )

        val movements = listOf(
            BoxMovement(id = 1L, boxId = 5, amountCents = 50000L, isDeposit = true, date = 1000L, note = "Depósito 1"),
            BoxMovement(id = 2L, boxId = 5, amountCents = 30000L, isDeposit = true, date = 2000L, note = "Depósito 2"),
            BoxMovement(id = 3L, boxId = 5, amountCents = 15000L, isDeposit = false, date = 3000L, note = "Saque emergencial")
        )

        // 500 + 300 - 150 = 650 (65000 centavos)
        val balance = BoxCalculator.calculateBalanceCents(box, movements)
        assertEquals(65000L, balance)
    }

    @Test
    fun testReversalRecalculatesCorrectly() {
        val box = FinanceItem(
            id = 5,
            title = "Viagem",
            type = "BOX",
            amountCents = 0L
        )

        val initialMovements = listOf(
            BoxMovement(id = 1L, boxId = 5, amountCents = 50000L, isDeposit = true, date = 1000L, note = "Depósito 1"),
            BoxMovement(id = 2L, boxId = 5, amountCents = 20000L, isDeposit = true, date = 2000L, note = "Depósito 2")
        )
        assertEquals(70000L, BoxCalculator.calculateBalanceCents(box, initialMovements))

        // Estorno do depósito 2 (simulado removendo a movimentação 2 da lista)
        val afterReversal = initialMovements.filter { it.id != 2L }
        assertEquals(50000L, BoxCalculator.calculateBalanceCents(box, afterReversal))
    }

    @Test
    fun testFallbackToItemAmountWhenNoMovements() {
        val box = FinanceItem(
            id = 7,
            title = "Carro",
            type = "BOX",
            amountCents = 45000L
        )

        val balance = BoxCalculator.calculateBalanceCents(box, emptyList())
        assertEquals(45000L, balance)
    }
}
