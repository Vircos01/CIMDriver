package com.cimdriver.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportUtilTest {

    @Test
    fun testEscapeCsvField_plainText() {
        val input = "Amsterdam Centraal"
        val expected = "Amsterdam Centraal"
        assertEquals(expected, ExportUtil.escapeCsvField(input))
    }

    @Test
    fun testEscapeCsvField_nullAndEmpty() {
        assertEquals("", ExportUtil.escapeCsvField(null))
        assertEquals("", ExportUtil.escapeCsvField(""))
    }

    @Test
    fun testEscapeCsvField_semicolon() {
        val input = "Stationsplein 1; 3511 ED Utrecht"
        val expected = "\"Stationsplein 1; 3511 ED Utrecht\""
        assertEquals(expected, ExportUtil.escapeCsvField(input))
    }

    @Test
    fun testEscapeCsvField_quotes() {
        val input = "Klantbezoek \"De Vries\""
        val expected = "\"Klantbezoek \"\"De Vries\"\"\""
        assertEquals(expected, ExportUtil.escapeCsvField(input))
    }

    @Test
    fun testEscapeCsvField_newlines() {
        val input = "Eerste regel\nTweede regel"
        val expected = "\"Eerste regel\nTweede regel\""
        assertEquals(expected, ExportUtil.escapeCsvField(input))
    }

    @Test
    fun testEscapeCsvField_quotesAndSemicolon() {
        val input = "Overleg \"Project A\"; incl. btw"
        val expected = "\"Overleg \"\"Project A\"\"; incl. btw\""
        assertEquals(expected, ExportUtil.escapeCsvField(input))
    }
}

