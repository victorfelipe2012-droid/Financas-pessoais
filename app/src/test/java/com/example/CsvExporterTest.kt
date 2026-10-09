package com.example

import com.example.data.FinanceItem
import com.example.ui.utils.CsvExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    @Test
    fun testCsvStartswithUtf8Bom() {
        val items = listOf(
            FinanceItem(id = 1, title = "Teste", type = "BILL", amountCents = 1000L, date = 1000000L)
        )
        val csv = CsvExporter.generateCsv(items)
        assertTrue("CSV deve iniciar com UTF-8 BOM", csv.startsWith("\uFEFF"))
    }

    @Test
    fun testFormulaInjectionSanitization() {
        // Valida que caracteres perigosos no início de células são precedidos por apóstrofo
        assertEquals("'=SUM(A1:A10)", CsvExporter.sanitizeCell("=SUM(A1:A10)"))
        assertEquals("'+12345", CsvExporter.sanitizeCell("+12345"))
        assertEquals("'-cmd /c calc", CsvExporter.sanitizeCell("-cmd /c calc"))
        assertEquals("'@test", CsvExporter.sanitizeCell("@test"))
        
        // Aspas duplas devem ser escapadas
        assertEquals("Texto com \"\"aspas\"\"", CsvExporter.sanitizeCell("Texto com \"aspas\""))

        // Quebras de linha substituídas por espaço
        assertEquals("Linha 1 Linha 2", CsvExporter.sanitizeCell("Linha 1\nLinha 2"))
    }

    @Test
    fun testCsvContentFormatting() {
        val items = listOf(
            FinanceItem(
                id = 42,
                title = "Aluguel =Fórmula",
                type = "APARTMENT",
                category = "Moradia",
                amountCents = 150050L, // 1500,50
                date = 1770000000000L,
                dueDate = 1770100000000L,
                paymentDate = 1770200000000L,
                isCompleted = true,
                description = "Observação teste"
            )
        )

        val csv = CsvExporter.generateCsv(items)
        assertTrue(csv.contains("\"42\""))
        assertTrue(csv.contains("\"Aluguel =Fórmula\"")) // sanitizado
        assertTrue(csv.contains("\"Apartamento & Moradia\""))
        assertTrue(csv.contains("\"Moradia\""))
        assertTrue(csv.contains("\"1500,50\""))
        assertTrue(csv.contains("\"Pago / Concluído\""))
    }
}
