package com.example.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val localeBr = Locale("pt", "BR")
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", localeBr)

    fun formatCurrency(amount: Double): String {
        return MoneyUtils.formatCents(MoneyUtils.toCents(amount))
    }

    fun formatCurrencyCents(cents: Long): String {
        return MoneyUtils.formatCents(cents)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun parseDouble(input: String): Double? {
        val cents = MoneyUtils.parseBrlToCents(input, allowNegative = true) ?: return null
        return MoneyUtils.centsToDouble(cents)
    }

    fun parseCents(input: String): Long? {
        return MoneyUtils.parseBrlToCents(input)
    }
}
