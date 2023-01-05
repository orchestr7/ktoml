package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals


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

        // FixMe #177: actually this logic is invalid, because Literal Strings should not be making a conversion of str
        val decoded = Toml.decodeFromString<MyClass>(test)
        assertEquals(decoded, MyClass('{', '4', 'D'))
    }

    @Test
    @Ignore
    fun charUnicodeSymbolsTest() {
        val test =
            """
                a = '\u0048'
                b = '\u0FCA'
                c = '\u0002'
            """

        val decoded = Toml.decodeFromString<MyClass>(test)
        assertEquals(decoded, MyClass('{', '\n', '\t'))
    }
}
