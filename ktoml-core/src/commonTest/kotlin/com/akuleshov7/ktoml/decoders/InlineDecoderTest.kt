package com.akuleshov7.ktoml.decoders

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.test.Test

class InlineDecoderTest {
    @Serializable
    data class Color(val rgb: Long = 0)

    @JvmInline
    @Serializable
    value class ColorWrapper(val color: Color)

    @Test
    fun testDecodingWithCustomSerializer() {
        """
            rgb = 0
        """.trimIndent()
            .shouldDecodeInto(Color(0))

        """
            [color]
            rgb = 0
        """.trimIndent()
            .shouldDecodeInto(ColorWrapper(Color(0)))

        """
        """.trimIndent()
                .shouldDecodeInto(Color(0))
    }
}
