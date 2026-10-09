package com.example.ui.utils

import com.example.data.FinanceItem
import com.example.data.RecurringBill
import java.util.Calendar
import java.util.Locale

object RecurringBillManager {

    /**
     * Formata a competência no padrão estruturado YYYY-MM.
     */
    fun formatCompetence(year: Int, month: Int): String {
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    /**
     * Verifica se a conta recorrente deve ser gerada para a competência indicada respeitando startDate e status ativo.
     * Se startDate estiver no futuro em relação à competência, não deve gerar.
     */
    fun shouldGenerateForCompetence(bill: RecurringBill, year: Int, month: Int): Boolean {
        if (!bill.isActive) return false

        val cal = Calendar.getInstance().apply { timeInMillis = bill.startDate }
        val startYear = cal.get(Calendar.YEAR)
        val startMonth = cal.get(Calendar.MONTH) + 1

        if (year < startYear) return false
        if (year == startYear && month < startMonth) return false
        return true
    }

    /**
     * Cria a entidade FinanceItem com campos estruturados para a competência.
     */
    fun buildOccurrenceItem(bill: RecurringBill, year: Int, month: Int): FinanceItem {
        val dueTimestamp = calculateDueDate(bill.dueDay, year, month)
        return FinanceItem(
            title = bill.title,
            amountCents = bill.amountCents,
            type = bill.type,
            category = bill.category,
            date = dueTimestamp,
            dueDate = dueTimestamp,
            isCompleted = false,
            description = "Conta mensal recorrente",
            recurringBillId = bill.id,
            competence = formatCompetence(year, month)
        )
    }

    /**
     * Calcula o timestamp exato do vencimento de uma conta recorrente para a competência dada (ano e mês).
     * Trata com precisão meses curtos e anos bissextos:
     * - Se dueDay for 31 e o mês for Fevereiro (28 ou 29 dias), ajusta automaticamente para o último dia do mês.
     * - Se dueDay for 31 e o mês tiver 30 dias (Abril, Junho, Setembro, Novembro), ajusta para 30.
     *
     * @param dueDay Dia do vencimento configurado (1 a 31)
     * @param year Ano da competência (ex: 2026)
     * @param month Mês da competência de 1 a 12 (ex: 2 para Fevereiro)
     */
    fun calculateDueDate(dueDay: Int, year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1) // Previne overflow temporário ao trocar mês
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)

        val maxDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val actualDay = dueDay.coerceIn(1, maxDayOfMonth)

        cal.set(Calendar.DAY_OF_MONTH, actualDay)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return cal.timeInMillis
    }

    /**
     * Tag de compatibilidade legada (mantida para verificação de registros antigos caso existam).
     */
    fun buildCompetenceTag(ruleId: Long, year: Int, month: Int): String {
        return String.format(Locale.US, "[Recorrência #RecID_%d_%04d-%02d]", ruleId, year, month)
    }

    /**
     * Verifica idempotência via lista estruturada ou tag legada na descrição.
     */
    fun isAlreadyGenerated(
        existingItems: List<FinanceItem>,
        ruleId: Long,
        year: Int,
        month: Int
    ): Boolean {
        val comp = formatCompetence(year, month)
        // 1. Verificação estruturada por recurringBillId e competence
        val hasStructured = existingItems.any { it.recurringBillId == ruleId && it.competence == comp }
        if (hasStructured) return true

        // 2. Fallback para itens migrados com tag no texto
        val tag = buildCompetenceTag(ruleId, year, month)
        return existingItems.any { it.description.contains(tag) }
    }
}
