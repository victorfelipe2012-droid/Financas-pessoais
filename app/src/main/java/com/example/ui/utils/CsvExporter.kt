package com.example.ui.utils

import com.example.data.FinanceItem

object CsvExporter {

    /**
     * Gera o conteúdo CSV formatado em UTF-8 com BOM, compatível com Excel, LibreOffice e Google Planilhas.
     * Inclui proteção contra CSV Formula Injection.
     */
    fun generateCsv(items: List<FinanceItem>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // Byte Order Mark para UTF-8

        // Cabeçalho
        sb.append("ID;Título;Tipo;Categoria;Valor (R$);Data Lançamento;Data Vencimento;Data Pagamento;Situação;Observação\n")

        for (item in items) {
            val safeTitle = sanitizeCell(item.title)
            val typeLabel = when (item.type) {
                "SALARY" -> "Salário / Receita"
                "APARTMENT" -> "Apartamento & Moradia"
                "BILL" -> "Conta a Pagar"
                "BOX" -> "Caixinha & Meta"
                "LENT" -> "Empréstimo"
                "INVESTMENT" -> "Investimento"
                "CHALLENGE" -> "Desafio 52 Semanas"
                else -> item.type
            }
            val safeCategory = sanitizeCell(item.category)
            val formattedAmount = MoneyUtils.formatCentsToInput(item.amountCents)
            val launchDate = FormatUtils.formatDate(item.date)
            val dueDate = item.dueDate?.let { FormatUtils.formatDate(it) } ?: ""
            val paymentDate = item.paymentDate?.let { FormatUtils.formatDate(it) } ?: ""
            val status = if (item.isCompleted) "Pago / Concluído" else "Pendente"
            val safeNote = sanitizeCell(item.description)

            sb.append("\"${item.id}\";\"$safeTitle\";\"$typeLabel\";\"$safeCategory\";\"$formattedAmount\";\"$launchDate\";\"$dueDate\";\"$paymentDate\";\"$status\";\"$safeNote\"\n")
        }

        return sb.toString()
    }

    /**
     * Sanitiza a célula para evitar quebras de CSV e protege contra fórmulas maliciosas.
     */
    fun sanitizeCell(text: String): String {
        var clean = text.replace("\"", "\"\"").replace("\r", "").replace("\n", " ")
        if (clean.startsWith("=") || clean.startsWith("+") || clean.startsWith("-") || clean.startsWith("@")) {
            clean = "'$clean"
        }
        return clean
    }

    /**
     * Salva o CSV em arquivo temporário no cache do aplicativo para compartilhamento seguro.
     */
    fun exportToTempFile(context: android.content.Context, items: List<FinanceItem>): java.io.File {
        val csvContent = generateCsv(items)
        val dir = java.io.File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        val file = java.io.File(dir, "privafin_export_${System.currentTimeMillis()}.csv")
        file.writeText(csvContent, Charsets.UTF_8)
        return file
    }
}
