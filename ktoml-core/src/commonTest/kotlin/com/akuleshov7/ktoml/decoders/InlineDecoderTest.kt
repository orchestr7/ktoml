package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.jvm.JvmInline
import kotlin.test.Test

class InlineDecoderTest {
    @JvmInline
    @Serializable
    value class Color(val rgb: Long)

    @Test
    fun testDecodingWithCustomSerializer() {
            Toml.decodeFromString<Color>(
            """
                rgb = 0
            """.trimIndent()
            )
    }
}
