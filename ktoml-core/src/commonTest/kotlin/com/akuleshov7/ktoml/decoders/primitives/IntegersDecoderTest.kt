package com.akuleshov7.ktoml.decoders.primitives

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IntegersDecoderTest {
    @Serializable
    data class Integers(
        val s: Short,
        val b: Byte,
        val i: Int,
        val l: Long,
    )

    @Test
    fun positiveScenario() {
        var test = """
                s = 5
                b = +5
                i = -5
                l = 5
            """

        var decoded = Toml.decodeFromString<Integers>(test)
        assertEquals(
            Integers(5, 5, -5, 5),
            decoded
        )

        test = """
                s = 32767
                b = -128
                i = 5
                l = 5
            """

        decoded = Toml.decodeFromString(test)
        assertEquals(
            Integers(32767, -128, 5, 5),
            decoded
        )
    }

    @Test
    fun negativeScenario() {
        var test = """
                s = 32768
                b = 5
                i = 5
                l = 5
            """.trimMargin()
        assertFailsWith<IllegalTypeException> { Toml.decodeFromString<Integers>(test) }

        test = """
                s = -32769
                b = 5
                i = 5
                l = 5
            """.trimMargin()
        assertFailsWith<IllegalTypeException> { Toml.decodeFromString<Integers>(test) }

        test = """
                s = 0.25
                b = 5
                i = 5
                l = 5
            """.trimMargin()
        assertFailsWith<IllegalTypeException> { Toml.decodeFromString<Integers>(test) }
    }
}