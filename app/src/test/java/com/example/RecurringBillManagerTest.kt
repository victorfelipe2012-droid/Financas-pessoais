package com.example

import com.example.ui.utils.RecurringBillManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class RecurringBillManagerTest {

    @Test
    fun testFebruaryDueDay31ClampingNonLeapYear() {
        // Fevereiro de 2025 (ano não bissexto: 28 dias)
        val timestamp = RecurringBillManager.calculateDueDate(dueDay = 31, year = 2025, month = 2)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }

        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
        assertEquals(28, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testFebruaryDueDay31ClampingLeapYear() {
        // Fevereiro de 2024 (ano bissexto: 29 dias)
        val timestamp = RecurringBillManager.calculateDueDate(dueDay = 31, year = 2024, month = 2)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }

        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
        assertEquals(29, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testAprilDueDay31Clamping() {
        // Abril tem 30 dias
        val timestamp = RecurringBillManager.calculateDueDate(dueDay = 31, year = 2026, month = 4)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }

        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.APRIL, cal.get(Calendar.MONTH))
        assertEquals(30, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testOctoberDueDay31Preserved() {
        // Outubro tem 31 dias
        val timestamp = RecurringBillManager.calculateDueDate(dueDay = 31, year = 2026, month = 10)
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }

        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testCompetenceTagAndIdempotency() {
        val tag = RecurringBillManager.buildCompetenceTag(ruleId = 42L, year = 2026, month = 10)
        assertEquals("[Recorrência #RecID_42_2026-10]", tag)

        val existingList = listOf(
            "[Recorrência #RecID_42_2026-09] Conta do mês anterior",
            "[Recorrência #RecID_42_2026-10] Conta deste mês"
        )

        // Mês 10 já gerado -> true
        assertTrue(RecurringBillManager.isAlreadyGenerated(existingList, ruleId = 42L, year = 2026, month = 10))

        // Mês 11 ainda não gerado -> false
        assertFalse(RecurringBillManager.isAlreadyGenerated(existingList, ruleId = 42L, year = 2026, month = 11))

        // Outra regra (ex: ID 99) ainda não gerada -> false
        assertFalse(RecurringBillManager.isAlreadyGenerated(existingList, ruleId = 99L, year = 2026, month = 10))
    }
}
