package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharDecoderTest {
    @Serializable
    data class MyClass(
        val a: Char,
        val b: Char,
        val c: Char,
    )

    @Test
    fun charBasicTest() {
        val test =
            """
                a = 123
                b = '4'
                c = 'D'
            """

        val decoded = Toml.decodeFromString<MyClass>(test)
        assertEquals(decoded, MyClass('{', '4', 'D'))
    }

    @Test
    fun charLiteralBasicTest() {
        val test =
            """
                a = '\r'
                b = '\n'
                c = '\t'
            """

            val decoded = Toml.decodeFromString<MyClass>(test)
            assertEquals(decoded, MyClass('\r', '\n', '\t'))
    }

    @Test
    fun charUnicodeSymbolsTest() {
        val test =
            """
                a = '\u0048'
                b = '\u0065'
                c = '\u006C'
            """

            val decoded = Toml.decodeFromString<MyClass>(test)
            assertEquals(decoded, MyClass('H', 'e', 'l'))
    }

    @Test
    fun charSeveralUnicodeSymbolsTest() {
        val test =
            """
                a = '\u0048\u0065'
                b = '\u0065\t'
                c = '\u006Cdd'
            """

        assertFailsWith<IllegalTypeException> { Toml.decodeFromString<MyClass>(test) }
    }
}
