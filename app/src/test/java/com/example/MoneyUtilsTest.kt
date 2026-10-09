package com.example

import com.example.ui.utils.MoneyUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyUtilsTest {

    @Test
    fun testExactCentAddition() {
        // Acceptance test from prompt: R$ 0,10 + R$ 0,20 resulta exatamente em R$ 0,30
        val cents1 = MoneyUtils.parseBrlToCents("0,10")!!
        val cents2 = MoneyUtils.parseBrlToCents("0,20")!!
        val sum = cents1 + cents2
        assertEquals(10L, cents1)
        assertEquals(20L, cents2)
        assertEquals(30L, sum)
        assertEquals("R$ 0,30".replace("\u00A0", " "), MoneyUtils.formatCents(sum).replace("\u00A0", " "))
    }

    @Test
    fun testParseVariousPtBrFormats() {
        assertEquals(125050L, MoneyUtils.parseBrlToCents("1250,50"))
        assertEquals(125050L, MoneyUtils.parseBrlToCents("R$ 1.250,50"))
        assertEquals(125050L, MoneyUtils.parseBrlToCents("1.250,50"))
        assertEquals(10000L, MoneyUtils.parseBrlToCents("100"))
        assertEquals(100L, MoneyUtils.parseBrlToCents("1"))
        assertEquals(5L, MoneyUtils.parseBrlToCents("0,05"))
        assertEquals(0L, MoneyUtils.parseBrlToCents("0,00"))
    }

    @Test
    fun testInvalidAndNegativeInputs() {
        assertNull(MoneyUtils.parseBrlToCents(""))
        assertNull(MoneyUtils.parseBrlToCents("abc"))
        assertNull(MoneyUtils.parseBrlToCents("-50,00")) // negative not allowed by default
        assertEquals(-5000L, MoneyUtils.parseBrlToCents("-50,00", allowNegative = true))
    }

    @Test
    fun testToCentsFromLegacyDouble() {
        assertEquals(10L, MoneyUtils.toCents(0.10))
        assertEquals(20L, MoneyUtils.toCents(0.20))
        assertEquals(30L, MoneyUtils.toCents(0.30))
        assertEquals(123456L, MoneyUtils.toCents(1234.56))
        assertEquals(0L, MoneyUtils.toCents(Double.NaN))
        assertEquals(0L, MoneyUtils.toCents(Double.POSITIVE_INFINITY))
    }
}
