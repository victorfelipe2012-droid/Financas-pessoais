package com.example.ui.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    private val localeBr = Locale("pt", "BR")
    private val currencyFormat = NumberFormat.getCurrencyInstance(localeBr)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", localeBr)

    fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun parseDouble(input: String): Double? {
        val cleaned = input.trim()
        if (cleaned.isEmpty()) return null
        return try {
            if (cleaned.contains(",")) {
                val parsedStr = cleaned.replace(".", "").replace(",", ".")
                parsedStr.toDoubleOrNull()
            } else {
                cleaned.toDoubleOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}
