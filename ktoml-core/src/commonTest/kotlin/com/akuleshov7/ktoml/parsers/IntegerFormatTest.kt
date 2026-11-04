package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLong
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlUnsignedLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/** Exercise integer syntax through complete TOML documents, not individual value nodes. */
class IntegerFormatTest {
    private val parser = TomlParser(TomlInputConfig.compliant())

    private fun parseInteger(literal: String) =
        assertIs<TomlKeyValuePrimitive>(parser.parseString("value = $literal").children.single()).value

    private fun assertInvalid(literals: List<String>) {
        for (literal in literals) {
            assertFailsWith<ParseException>(literal) {
                parser.parseString("value = $literal")
            }
        }
    }

    @Test
    fun rejectsMisplacedUnderscores() {
        assertInvalid(
            listOf(
                "_1", "1_", "1__0", "+_1", "-_1",
                "_0b1", "0b_1", "0b1_", "0b1__0",
                "_0o7", "0o_7", "0o7_", "0o7__0",
                "_0xF", "0x_F", "0xF_", "0xF__0",
            )
        )
    }

    @Test
    fun rejectsLeadingZerosInDecimalIntegers() {
        assertInvalid(listOf("00", "01", "+01", "-01", "0_1", "+0_0", "-0_0"))
    }

    @Test
    fun rejectsSignsAfterRadixPrefixes() {
        assertInvalid(listOf("0b+1", "0b-1", "0o+7", "0o-7", "0x+F", "0x-F"))
    }

    @Test
    fun validatesUnsignedIntegerSyntax() {
        assertInvalid(
            listOf(
                "9__223_372_036_854_775_808",
                "18446744073709551615_",
                "018446744073709551615",
                "0x_FFFF_FFFF_FFFF_FFFF",
                "0x+FFFF_FFFF_FFFF_FFFF",
            )
        )
    }

    @Test
    fun acceptsValidIntegerSpellings() {
        val signedValues = listOf(
            "0" to 0L, "+0" to 0L, "-0" to 0L,
            "1_000" to 1000L, "+1_000" to 1000L, "-1_000" to -1000L,
            "0b0010_1010" to 42L, "0o0_52" to 42L, "0x0_2a" to 42L,
            "9223372036854775807" to Long.MAX_VALUE,
            "-9223372036854775808" to Long.MIN_VALUE,
        )
        for ((literal, expected) in signedValues) {
            assertEquals(expected, assertIs<TomlLong>(parseInteger(literal)).content, literal)
        }
        assertEquals(
            ULong.MAX_VALUE,
            assertIs<TomlUnsignedLong>(parseInteger("18_446_744_073_709_551_615")).content,
        )
        assertEquals(
            ULong.MAX_VALUE,
            assertIs<TomlUnsignedLong>(parseInteger("0xFFFF_FFFF_FFFF_FFFF")).content,
        )
    }
}
