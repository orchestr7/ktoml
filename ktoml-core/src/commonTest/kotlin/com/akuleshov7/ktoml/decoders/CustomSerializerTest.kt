package com.akuleshov7.ktoml.decoders

import kotlinx.serialization.decodeFromString
import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test

class CustomSerializerTest {
    object ColorAsStringSerializer : KSerializer<Color> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Color) {
            TODO()
        }

        override fun deserialize(decoder: Decoder): Color {
            val value = decoder.decodeString()
            return Color(value.toLong())
        }
    }

    @Serializable(with = ColorAsStringSerializer::class)
    data class Color(val rgb: Long)

    @Serializable
    data class Settings(val background: Color, val foreground: Color)

    @Test
    fun testDecodingWithCustomSerializer() {
        println(Toml.decodeFromString<Settings>(
            """
                [background]
                    rgb = 0
                [foreground]
                    rgb = 0
            """.trimIndent()
        ))
    }
}
