package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.Serializable
import kotlin.test.Test

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
        """
            s = 5
            b = +5
            i = -5
            l = 5
        """.shouldDecodeInto(Integers(5, 5, -5, 5))

        """
            s = 32767
            b = -128
            i = 5
            l = 5
        """.shouldDecodeInto(Integers(32767, -128, 5, 5))
    }

    @Test
    fun negativeScenario() {
        """
            s = 32768
            b = 5
            i = 5
            l = 5
        """.trimMargin()
            .shouldThrowExceptionWhileDecoding<Integers, IllegalTypeException>()

        """
            s = -32769
            b = 5
            i = 5
            l = 5
        """.trimMargin()
            .shouldThrowExceptionWhileDecoding<Integers, IllegalTypeException>()

        """
            s = 0.25
            b = 5
            i = 5
            l = 5
        """.trimMargin()
            .shouldThrowExceptionWhileDecoding<Integers, IllegalTypeException>()
    }
}
