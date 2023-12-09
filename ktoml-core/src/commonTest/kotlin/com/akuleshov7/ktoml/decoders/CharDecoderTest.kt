package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlin.test.Test

class CharDecoderTest {
    @Serializable
    data class MyClass(
        val a: Char,
        val b: Char,
        val c: Char,
    )

    @Test
    fun charBasicTest() {
        """
            a = 123
            b = '4'
            c = 'D'
        """.shouldDecodeInto(MyClass('{', '4', 'D'))
    }

    @Test
    fun charLiteralBasicTest() {
        """
            a = '\r'
            b = '\n'
            c = '\t'
        """.shouldDecodeInto(MyClass('\r', '\n', '\t'))
    }

    @Test
    fun charUnicodeSymbolsTest() {
        """
            a = '\u0048'
            b = '\u0065'
            c = '\u006C'
        """.shouldDecodeInto(MyClass('H', 'e', 'l'))
    }

    @Test
    fun charSeveralUnicodeSymbolsTest() {
        """
            a = '\u0048\u0065'
            b = '\u0065\t'
            c = '\u006Cdd'
        """.shouldThrowExceptionWhileDecoding<MyClass, IllegalTypeException>()
    }
}
