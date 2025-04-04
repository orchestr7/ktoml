package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnsignedNumbersDecoderTest {

    @Serializable
    data class UnsignedIntegers(
        val b: UByte = (Byte.MAX_VALUE + 1).toUByte(),
        val s: UShort = (Short.MAX_VALUE + 1).toUShort(),
        val i: UInt = (Int.MAX_VALUE + 1).toUInt(),
        val l: ULong = (Long.MAX_VALUE + 1).toULong(),
    )

    @Test
    fun decodeUnsignedIntegers() {
        val toml = """
              b = 128
              s = 32768
              i = 2147483648
              l = 9_223_372_036_854_775_808
        """.trimIndent()

        assertEquals(UnsignedIntegers(), Toml.decodeFromString<UnsignedIntegers>(toml))
    }

    @Test
    fun decodeUnsignedIntegersInArray() {
        @Serializable
        data class UnsignedIntegersArray(
            val b: List<UByte> = listOf(1u, 2u, 3u, 4u),
            val s: List<UInt> = listOf(1u, 2u, 3u, 4u),
            val i: List<UInt> = listOf(1u, 2u, 3u, 4u),
            val l: List<ULong> = listOf(1u, 2u, 3u, 4u),
        )

        val toml = """
            b = [1, 2, 3, 4]
            s = [1, 2, 3, 4]
            i = [1, 2, 3, 4]
            l = [1, 2, 3, 4]
        """.trimIndent()
        assertEquals(UnsignedIntegersArray(), Toml.decodeFromString<UnsignedIntegersArray>(toml))
    }

    @Test
    fun shouldThrowExceptionOnOverflow() {
        val toml = """
            b = 256
        """.trimIndent()

        assertFailsWith<IllegalTypeException> {
            Toml.decodeFromString<UnsignedIntegers>(toml)
        }
    }
}
