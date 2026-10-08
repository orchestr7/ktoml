package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.UnknownEscapeSymbolsException
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnicodeEscapeTest {
    @Test
    fun validScalarBoundariesAreDecoded() {
        val cases = listOf(
            "\\u0000" to "\u0000",
            "\\uD7FF" to "\uD7FF",
            "\\uE000" to "\uE000",
            "\\uFFFF" to "\uFFFF",
            "\\U00000000" to "\u0000",
            "\\U0000D7FF" to "\uD7FF",
            "\\U0000E000" to "\uE000",
            "\\U0000FFFF" to "\uFFFF",
            "\\U00010000" to "\uD800\uDC00",
            "\\U0010FFFF" to "\uDBFF\uDFFF",
            "\\u00e9\\U0001f615" to "é\uD83D\uDE15",
        )
        for (config in listOf(TomlInputConfig(), TomlInputConfig.compliant())) {
            for ((escape, expected) in cases) {
                for (quote in listOf("\"", "\"\"\"")) {
                    val input = "a = $quote${escape}$quote"
                    assertEquals(
                        mapOf("a" to expected),
                        Toml(config).decodeFromString<Map<String, String>>(input),
                        input,
                    )
                }
            }
        }
    }

    @Test
    fun everySurrogateCodePointIsRejectedInBothEscapeForms() {
        for (codePoint in 0xD800..0xDFFF) {
            val hex = codePoint.toString(16)
            for (escape in listOf("\\u$hex", "\\U${hex.padStart(8, '0')}")) {
                assertFailsWith<UnknownEscapeSymbolsException>(escape) {
                    escape.convertSpecialCharacters(1)
                }
            }
        }
    }

    @Test
    fun surrogateEscapesAreRejectedInStringsAndKeys() {
        val escapes = listOf(
            "\\uD800", "\\uDBFF", "\\uDC00", "\\uDFFF",
            "\\U0000D800", "\\U0000DBFF", "\\U0000DC00", "\\U0000DFFF",
            // TOML escapes represent scalar values, not UTF-16 code units.
            "\\uD83D\\uDE15",
        )
        for (config in listOf(TomlInputConfig(), TomlInputConfig.compliant())) {
            for (escape in escapes) {
                for (input in listOf("a = \"$escape\"", "a = \"\"\"$escape\"\"\"", "\"$escape\" = \"value\"")) {
                    assertFailsWith<UnknownEscapeSymbolsException>(input) {
                        Toml(config).decodeFromString<Map<String, String>>(input)
                    }
                }
            }
        }
    }

    @Test
    fun codePointsAboveUnicodeRangeAreRejected() {
        for (escape in listOf("\\U00110000", "\\U0011FFFF", "\\U7FFFFFFF")) {
            assertFailsWith<UnknownEscapeSymbolsException>(escape) {
                escape.convertSpecialCharacters(1)
            }
        }
    }

    @Test
    fun literalAndEscapedBackslashStringsPreserveUnicodeEscapeText() {
        for (config in listOf(TomlInputConfig(), TomlInputConfig.compliant())) {
            for (escape in listOf("\\uD800", "\\U0000DFFF", "\\uD83D\\uDE15")) {
                for (quote in listOf("'", "'''")) {
                    assertEquals(
                        mapOf("a" to escape),
                        Toml(config).decodeFromString<Map<String, String>>("a = $quote${escape}$quote"),
                    )
                }
                val escapedBackslashes = escape.replace("\\", "\\\\")
                assertEquals(
                    mapOf("a" to escape),
                    Toml(config).decodeFromString<Map<String, String>>("a = \"$escapedBackslashes\""),
                )
            }
        }
    }
}
