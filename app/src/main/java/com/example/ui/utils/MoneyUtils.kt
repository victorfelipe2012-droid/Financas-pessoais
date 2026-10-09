package com.example.ui.utils

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Utilitário centralizado para cálculos financeiros, conversão e formatação monetária em pt-BR.
 * Garante que todos os valores sejam manipulados em centavos (Long), eliminando erros de ponto flutuante.
 */
object MoneyUtils {

    private val ptBrLocale = Locale("pt", "BR")

    /**
     * Converte um valor decimal em Double (legado) para centavos exatos (Long)
     * utilizando BigDecimal com arredondamento HALF_UP explícito.
     * NUNCA multiplica diretamente Double por 100 para evitar imprecisões IEEE 754.
     */
    fun toCents(amount: Double): Long {
        if (!amount.isFinite() || amount.isNaN()) return 0L
        return BigDecimal.valueOf(amount)
            .setScale(2, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))
            .toLong()
    }

    /**
     * Converte centavos (Long) para Double (para interoperabilidade retrocompatível quando estritamente necessário).
     */
    fun centsToDouble(cents: Long): Double {
        return cents / 100.0
    }

    /**
     * Faz o parse seguro de uma string digitada pelo usuário em formato pt-BR para centavos (Long).
     * Suporta formatos:
     * - "1250,50" -> 125050L
     * - "R$ 1.250,50" -> 125050L
     * - "1250.50" -> 125050L
     * - "0,10" -> 10L
     * Retorna null se a entrada for inválida ou negativa (quando não permitido).
     */
    fun parseBrlToCents(text: String, allowNegative: Boolean = false): Long? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null

        try {
            // Remove símbolo de moeda, espaços normais e non-breaking spaces
            var clean = trimmed
                .replace("R$", "")
                .replace("r$", "")
                .replace("\u00A0", "")
                .replace(" ", "")

            // Identificar se usa notação brasileira (vírgula como decimal) ou americana
            val hasComma = clean.contains(",")
            val hasDot = clean.contains(".")

            clean = if (hasComma && hasDot) {
                // Notação brasileira: "1.250,50" -> ponto é milhar, vírgula é decimal
                clean.replace(".", "").replace(",", ".")
            } else if (hasComma) {
                // "1250,50"
                clean.replace(",", ".")
            } else {
                // "1250.50" ou "1250"
                clean
            }

            val bd = clean.toBigDecimalOrNull() ?: return null
            if (!allowNegative && bd < BigDecimal.ZERO) return null

            return bd.setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toLong()
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Formata um valor em centavos para exibição monetária pt-BR (ex: "R$ 1.250,50").
     */
    fun formatCents(cents: Long): String {
        val nf = NumberFormat.getCurrencyInstance(ptBrLocale)
        return nf.format(cents / 100.0)
    }

    /**
     * Formata centavos para formato numérico simples pt-BR (ex: "1.250,50") para campos de edição.
     */
    fun formatCentsToInput(cents: Long): String {
        if (cents == 0L) return ""
        val nf = NumberFormat.getNumberInstance(ptBrLocale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
            isGroupingUsed = false
        }
        return nf.format(cents / 100.0)
    }

    /**
     * Faz o parse seguro de entrada de texto e retorna centavos (Long), ou 0L se inválido.
     */
    fun parseInputToCents(text: String, allowNegative: Boolean = false): Long {
        return parseBrlToCents(text, allowNegative) ?: 0L
    }
}
