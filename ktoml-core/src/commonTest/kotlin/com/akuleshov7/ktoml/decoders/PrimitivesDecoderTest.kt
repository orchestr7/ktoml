package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class PrimitivesDecoderTest {
    @Test
    fun decodeByte() {
        fun test(expected: Byte, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Byte)

            val data = Toml.decodeFromString<Data>(toml)

            assertEquals(expected, data.value)
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
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Byte)

            assertFailsWith<IllegalTypeException>(input) {
                Toml.decodeFromString<Data>(toml)
            }
        }

        testFails("-129")
        testFails("128")
    }

    @Test
    fun decodeChar() {
        fun test(expected: Char, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Char)

            assertFailsWith<IllegalTypeException> {
                val data = Toml.decodeFromString<Data>(toml)
                assertEquals(expected, data.value)
            }
        }

        test((0).toChar(), "0")
        test((-1).toChar(), "-1")
        test((1).toChar(), "1")
        test(Char.MAX_VALUE, "0")
        test(Char.MIN_VALUE, "1")
    }

    @Test
    fun decodeShort() {
        fun test(expected: Short, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Short)

            val data = Toml.decodeFromString<Data>(toml)

            assertEquals(expected, data.value)
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
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Short)

            assertFailsWith<IllegalTypeException>(input) {
                Toml.decodeFromString<Data>(toml)
            }
        }

        testFails("${Short.MAX_VALUE.toInt() + 1}")
        testFails("${Short.MIN_VALUE.toInt() - 1}")
    }

    @Test
    fun decodeInt() {
        fun test(expected: Int, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Int)

            val data = Toml.decodeFromString<Data>(toml)

            assertEquals(expected, data.value)
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
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Int)

            assertFailsWith<IllegalTypeException>(input) {
                Toml.decodeFromString<Data>(toml)
            }
        }

        testFails("${Int.MIN_VALUE.toLong() - 1}")
        testFails("${Int.MAX_VALUE.toLong() + 1}")
    }

    @Test
    fun decodeLong() {
        fun test(expected: Long, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Long)

            val data = Toml.decodeFromString<Data>(toml)

            assertEquals(expected, data.value)
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
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Long)

            assertFailsWith<IllegalTypeException>(input) {
                Toml.decodeFromString<Data>(toml)
            }
        }

        testFails("${Long.MIN_VALUE}0")
        testFails("${Long.MAX_VALUE}0")
    }

    @Test
    fun decodeFloat() {
        fun test(expected: Float, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Float)

            val data = Toml.decodeFromString<Data>(toml)
            assertEquals(expected, data.value)
        }

        test(0f, "0.0")
        test(1f, "1.0")
        test(-1f, "-1.0")
        test(-128f, "-128.0")
        test(127f, "127.0")
    }

    @Test
    fun decodeFloatFailure() {
        fun testFails(input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Float)

            assertFailsWith<IllegalTypeException>(input) {
                Toml.decodeFromString<Data>(toml)
            }
        }

        testFails((-Double.MAX_VALUE).toString())
        testFails("-129")
        testFails("128")
    }

    @Test
    fun decodeDouble() {
        fun test(expected: Double, input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Double)

            val data = Toml.decodeFromString<Data>(toml)

            assertEquals(expected, data.value)
        }

        test(0.0, "0.0")
        test(1.0, "1.0")
        test(-1.0, "-1.0")
        test(-128.0, "-128.0")
        test(127.0, "127.0")
    }

    @Test
    fun decodeDoubleFailure() {
        fun testFails(input: String) {
            val toml = /*language=TOML*/ """value = $input"""

            @Serializable
            data class Data(val value: Double)

            assertFailsWith<IllegalTypeException>(input) {
                Toml.decodeFromString<Data>(toml)
            }
        }

        testFails("-129")
        testFails("128")
        testFails("0")
    }
}
