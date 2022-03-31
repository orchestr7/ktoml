package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

class InlineDecoderTest {
    @Serializable
    data class Color(val rgb: Long = 0)

    @JvmInline
    @Serializable
    value class ColorWrapper(val color: Color)

    @Test
    fun testDecodingWithCustomSerializer() {
        var res = Toml.decodeFromString<Color>(
            """
                rgb = 0
            """.trimIndent()
        )

        assertEquals(Color(0), res)

        val newRes = Toml.decodeFromString<ColorWrapper>(
            """
                [color]
                rgb = 0
            """.trimIndent()
        )

        assertEquals(ColorWrapper(Color(0)), newRes)


        res = Toml.decodeFromString<Color>(
            """
            """.trimIndent()
        )

        assertEquals(Color(0), res)
    }
}
