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
    fun validUnicodeEscapesAreConverted() {
        assertEquals("A", "\\u0041".convertSpecialCharacters(1))
        assertEquals("\uD83D\uDCA9", "\\U0001F4A9".convertSpecialCharacters(1))
        assertEquals("\u00FF", "\\u00ff".convertSpecialCharacters(1))
        assertEquals("3", "\\x33".convertSpecialCharacters(1))
    }

    @Test
    fun malformedUnicodeEscapesAreRejected() {
        val malformed = listOf(
            "\\x+3",
            "\\x-3",
            "\\uZZZZ",
            "\\uabag",
            // TOML HEXDIG is ASCII-only; Unicode digits must not be accepted as escape digits
            "\\u００４１",
            "\\u+041",
            "\\u-041",
            "\\UFFFFFFFF",
            "\\U0011FFFF",
            "\\U00D80000",
            "\\uD800",
            "\\u041",
        )

        malformed.forEach { input ->
            assertFailsWith<UnknownEscapeSymbolsException>("expected <$input> to be rejected") {
                input.convertSpecialCharacters(1)
            }
        }
    }

    @Test
    fun malformedUnicodeEscapeInDecodedTomlRaisesParseException() {
        assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>("a = \"\\uZZZZ\"")
        }
        assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>("a = \"\\u+041\"")
        }
    }
}
