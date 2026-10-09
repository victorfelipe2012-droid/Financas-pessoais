package com.example

import com.example.data.FinanceItem
import com.example.data.RecurringBill
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
    fun testRespectsFutureStartDate() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.DECEMBER, 15, 0, 0, 0)
        val futureStart = cal.timeInMillis

        val bill = RecurringBill(
            id = 5,
            title = "Academia Plano Futuro",
            category = "Saúde",
            amountCents = 12000L,
            dueDay = 10,
            startDate = futureStart,
            isActive = true
        )

        // Competência Outubro 2026 (anterior a Dezembro 2026) -> NÃO deve gerar
        assertFalse(
            "Recorrência com início futuro não deve gerar para mês anterior",
            RecurringBillManager.shouldGenerateForCompetence(bill, year = 2026, month = 10)
        )

        // Competência Novembro 2026 -> NÃO deve gerar
        assertFalse(
            RecurringBillManager.shouldGenerateForCompetence(bill, year = 2026, month = 11)
        )

        // Competência Dezembro 2026 -> DEVE gerar
        assertTrue(
            "Recorrência deve gerar no mês de início",
            RecurringBillManager.shouldGenerateForCompetence(bill, year = 2026, month = 12)
        )

        // Competência Janeiro 2027 -> DEVE gerar
        assertTrue(
            "Recorrência deve gerar em meses futuros",
            RecurringBillManager.shouldGenerateForCompetence(bill, year = 2027, month = 1)
        )
    }

    @Test
    fun testEditingDescriptionPreservesRecurringLink() {
        val bill = RecurringBill(
            id = 15,
            title = "Internet Fibra",
            category = "Casa",
            amountCents = 15000L,
            dueDay = 20,
            startDate = 1000L,
            isActive = true
        )

        val item = RecurringBillManager.buildOccurrenceItem(bill, year = 2026, month = 10)
        assertEquals(15L, item.recurringBillId)
        assertEquals("2026-10", item.competence)

        // Usuário edita a descrição na interface
        val userEditedItem = item.copy(description = "Paguei adiantado via Pix no Itaú")

        // O vínculo estruturado é 100% preservado
        assertEquals(15L, userEditedItem.recurringBillId)
        assertEquals("2026-10", userEditedItem.competence)

        val list = listOf(userEditedItem)
        assertTrue(
            "Idempotência estruturada deve reconhecer a ocorrência mesmo com descrição modificada",
            RecurringBillManager.isAlreadyGenerated(list, ruleId = 15L, year = 2026, month = 10)
        )
    }
}
