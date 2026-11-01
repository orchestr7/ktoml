package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.parseValue
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLong
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlUnsignedLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class IntegerFormatTest {
    private val config = TomlInputConfig.compliant()

    @Test
    fun rejectsMisplacedUnderscores() {
        val invalid = listOf(
            "_1", "1_", "1__0", "+_1", "-_1",
            "_0b1", "0b_1", "0b1_", "0b1__0",
            "_0o7", "0o_7", "0o7_", "0o7__0",
            "_0xF", "0x_F", "0xF_", "0xF__0",
        )
        for (literal in invalid) {
            assertFailsWith<ParseException>(literal) { literal.parseValue(1, config) }
        }
    }

    @Test
    fun rejectsLeadingZerosInDecimalIntegers() {
        for (literal in listOf("00", "01", "+01", "-01", "0_1", "+0_0", "-0_0")) {
            assertFailsWith<ParseException>(literal) { literal.parseValue(1, config) }
        }
    }

    @Test
    fun rejectsSignsAfterRadixPrefixes() {
        for (literal in listOf("0b+1", "0b-1", "0o+7", "0o-7", "0x+F", "0x-F")) {
            assertFailsWith<ParseException>(literal) { literal.parseValue(1, config) }
        }
    }

    @Test
    fun validatesUnsignedIntegerSyntax() {
        val invalid = listOf(
            "9__223_372_036_854_775_808",
            "18446744073709551615_",
            "018446744073709551615",
            "0x_FFFF_FFFF_FFFF_FFFF",
            "0x+FFFF_FFFF_FFFF_FFFF",
        )
        for (literal in invalid) {
            assertFailsWith<ParseException>(literal) { literal.parseValue(1, config) }
        }
    }

    @Test
    fun acceptsValidIntegerSpellings() {
        val valid = listOf(
            "0" to 0L, "+0" to 0L, "-0" to 0L,
            "1_000" to 1000L, "+1_000" to 1000L, "-1_000" to -1000L,
            "0b0010_1010" to 42L, "0o0_52" to 42L, "0x0_2a" to 42L,
            "9223372036854775807" to Long.MAX_VALUE,
            "-9223372036854775808" to Long.MIN_VALUE,
        )
        for ((literal, expected) in valid) {
            assertEquals(expected, assertIs<TomlLong>(literal.parseValue(1, config)).content, literal)
        }
        assertEquals(
            ULong.MAX_VALUE,
            assertIs<TomlUnsignedLong>("18_446_744_073_709_551_615".parseValue(1, config)).content,
        )
        assertEquals(
            ULong.MAX_VALUE,
            assertIs<TomlUnsignedLong>("0xFFFF_FFFF_FFFF_FFFF".parseValue(1, config)).content,
        )
    }
}
