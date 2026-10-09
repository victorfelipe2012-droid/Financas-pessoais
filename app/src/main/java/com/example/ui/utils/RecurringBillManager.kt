package com.example.ui.utils

import java.util.Calendar
import java.util.Locale

object RecurringBillManager {

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
     * Gera a chave única imutável para a recorrência na competência.
     */
    fun buildCompetenceTag(ruleId: Long, year: Int, month: Int): String {
        return String.format(Locale.US, "[Recorrência #RecID_%d_%04d-%02d]", ruleId, year, month)
    }

    /**
     * Verifica se a recorrência já foi gerada neste mês para evitar duplicações (idempotência).
     */
    fun isAlreadyGenerated(existingDescriptions: List<String>, ruleId: Long, year: Int, month: Int): Boolean {
        val tag = buildCompetenceTag(ruleId, year, month)
        return existingDescriptions.any { it.contains(tag) }
    }
}
