package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.UnknownEscapeSymbolsException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SpecialCharactersTest {
    @Serializable
    data class SimpleString(val a: String)

    @Test
    fun testValidUnicodeEscapesAreConverted() {
        assertEquals("A", "\\u0041".convertSpecialCharacters(1))
        assertEquals("\uD83D\uDCA9", "\\U0001F4A9".convertSpecialCharacters(1))
        assertEquals("\u00FF", "\\u00ff".convertSpecialCharacters(1))
    }

    @Test
    fun testMalformedUnicodeEscapesAreRejected() {
        val malformed = listOf(
            "\\uZZZZ",
            // a single non-hex digit in an otherwise well-formed code
            "\\uabag",
            // both toInt and toLong accept a leading sign, which would smuggle these
            // through as U+0041
            "\\u+041",
            "\\u-041",
            // eight hex digits overflow an Int
            "\\UFFFFFFFF",
            // parses cleanly but sits above the highest code point
            "\\U0011FFFF",
            "\\U00D80000",
            // the code is cut short by the end of the string
            "\\u041",
        )

        malformed.forEach { input ->
            assertFailsWith<UnknownEscapeSymbolsException>("expected <$input> to be rejected") {
                input.convertSpecialCharacters(1)
            }
        }
    }

    @Test
    fun testMalformedUnicodeEscapeInDecodedTomlRaisesParseException() {
        assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>("a = \"\\uZZZZ\"")
        }
        assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>("a = \"\\u+041\"")
        }
    }
}
