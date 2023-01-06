package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Ignore
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

        assertFailsWith<IllegalTypeException> {
            val decoded = Toml.decodeFromString<MyClass>(test)
            assertEquals(decoded, MyClass('\r', '\n', '\t'))
        }
    }

    @Test
    fun charUnicodeSymbolsTest() {
        val test =
            """
                a = '\u0048'
                b = '\u0FCA'
                c = '\u0002'
            """

        assertFailsWith<IllegalTypeException> {
            val decoded = Toml.decodeFromString<MyClass>(test)
            assertEquals(decoded, MyClass('{', '\n', '\t'))
        }
    }
}
