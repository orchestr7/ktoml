package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlin.test.Test

class PrimitivesDecoderTest {
    @Test
    fun decodeByte() {
        fun test(expected: Byte, input: String) {
            @Serializable
            data class Data(val value: Byte)

            /*language=TOML*/
            """value = $input"""
                .shouldDecodeInto(Data(expected))
        }

        test(0, "0")
        test(1, "1")
        test(-1, "-1")
        test(-128, "-128")
        test(127, "127")
    }

    @Test
    fun decodeByteFailure() {
        fun testFails(input: String) {
            @Serializable
            data class Data(val value: Byte)

            /*language=TOML*/
            """value = $input"""
                .shouldThrowExceptionWhileDecoding<Data, IllegalTypeException>()
        }

        testFails("-129")
        testFails("128")
    }

    @Test
    fun decodeChar() {
        fun test(expected: Char, input: String) {

            @Serializable
            data class Data(val value: Char)

            /*language=TOML*/
            "value = $input"
                .shouldDecodeInto(Data(expected))
        }

        test('1', "\'1\'")
        test((1).toChar(), "1")
        test(Char.MAX_VALUE, "65535")
        test(Char.MIN_VALUE, "0")
    }

    @Test
    fun decodeCharFailure() {
        fun test(input: String) {
            @Serializable
            data class Data(val value: Char)

            /*language=TOML*/
            "value = $input"
                .shouldThrowExceptionWhileDecoding<Data, IllegalTypeException>()
        }

        test("-1")
        test("65536")
    }

    @Test
    fun decodeShort() {
        fun test(expected: Short, input: String) {
            @Serializable
            data class Data(val value: Short)

            /*language=TOML*/
            """value = $input"""
                .shouldDecodeInto(Data(expected))
        }

        test(0, "0")
        test(1, "1")
        test(-1, "-1")
        test(-128, "-128")
        test(127, "127")
    }

    @Test
    fun decodeShortFailure() {
        fun testFails(input: String) {
            @Serializable
            data class Data(val value: Short)

            /*language=TOML*/
            """value = $input"""
                .shouldThrowExceptionWhileDecoding<Data, IllegalTypeException>()
        }

        testFails("${Short.MAX_VALUE.toInt() + 1}")
        testFails("${Short.MIN_VALUE.toInt() - 1}")
    }

    @Test
    fun decodeInt() {
        fun test(expected: Int, input: String) {
            @Serializable
            data class Data(val value: Int)

            /*language=TOML*/
            """value = $input"""
                .shouldDecodeInto(Data(expected))
        }

        test(0, "0")
        test(1, "1")
        test(-1, "-1")
        test(-128, "-128")
        test(127, "127")
        test(Int.MAX_VALUE, "${Int.MAX_VALUE}")
        test(Int.MIN_VALUE, "${Int.MIN_VALUE}")
    }

    @Test
    fun decodeIntFailure() {
        fun testFails(input: String) {
            @Serializable
            data class Data(val value: Int)

            /*language=TOML*/
            """value = $input"""
                .shouldThrowExceptionWhileDecoding<Data, IllegalArgumentException>()
        }

        testFails("${Int.MIN_VALUE.toLong() - 1}")
        testFails("${Int.MAX_VALUE.toLong() + 1}")
    }

    @Test
    fun decodeLong() {
        fun test(expected: Long, input: String) {
            @Serializable
            data class Data(val value: Long)

            /*language=TOML*/
            """value = $input"""
                .shouldDecodeInto(Data(expected))
        }

        test(0, "0")
        test(1, "1")
        test(-1, "-1")
        test(-128, "-128")
        test(127, "127")
        test(Long.MIN_VALUE, "${Long.MIN_VALUE}")
        test(Long.MAX_VALUE, "${Long.MAX_VALUE}")
    }

    @Test
    fun decodeLongFailure() {
        fun testFails(input: String) {
            @Serializable
            data class Data(val value: Long)

            /*language=TOML*/
            """value = $input"""
                .shouldThrowExceptionWhileDecoding<Data, IllegalArgumentException>()
        }

        testFails("${Long.MIN_VALUE}0")
        testFails("${Long.MAX_VALUE}0")
    }

    @Test
    fun decodeFloat() {
        fun test(expected: Float, input: String) {
            @Serializable
            data class Data(val value: Float)

            /*language=TOML*/
            """value = $input"""
                .shouldDecodeInto(Data(expected))
        }

        test(0f, "0.0")
        test(1f, "1.0")
        test(-1f, "-1.0")
        test(-128f, "-128.0")
        test(127f, "127.0")
        test(Float.NEGATIVE_INFINITY, "-inf")
        test(Float.POSITIVE_INFINITY, "+inf")
        test(Float.NaN, "nan")
    }

    @Test
    fun decodeFloatFailure() {
        fun testFails(input: String) {
            @Serializable
            data class Data(val value: Float)

            /*language=TOML*/
            """value = $input"""
                .shouldThrowExceptionWhileDecoding<Data, IllegalArgumentException>()
        }

        testFails((-Double.MAX_VALUE).toString())
        testFails("-129")
        testFails("128")
    }

    @Test
    fun decodeDouble() {
        fun test(expected: Double, input: String) {
            @Serializable
            data class Data(val value: Double)

            /*language=TOML*/
            """value = $input"""
                .shouldDecodeInto(Data(expected))
        }
        test(0.0, "0.0")
        test(1.0, "1.0")
        test(-1.0, "-1.0")
        test(-128.0, "-128.0")
        test(127.0, "127.0")
        test(Double.NEGATIVE_INFINITY, "-inf")
        test(Double.POSITIVE_INFINITY, "+inf")
        test(Double.NaN, "nan")
    }

    @Test
    fun decodeDoubleFailure() {
        fun testFails(input: String) {
            @Serializable
            data class Data(val value: Double)

            /*language=TOML*/
            """value = $input"""
                .shouldThrowExceptionWhileDecoding<Data, IllegalArgumentException>()
        }

        testFails("-129")
        testFails("128")
        testFails("0")
    }
}
