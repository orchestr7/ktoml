package com.akuleshov7.ktoml.decoders.custom

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class SurrogateTest {
    object ColorSerializer : KSerializer<Color> {
        override val descriptor: SerialDescriptor = ColorSurrogate.serializer().descriptor

        override fun serialize(encoder: Encoder, value: Color) {
            val surrogate = ColorSurrogate((value.rgb shr 16) and 0xff, (value.rgb shr 8) and 0xff, value.rgb and 0xff)
            encoder.encodeSerializableValue(ColorSurrogate.serializer(), surrogate)
        }

        override fun deserialize(decoder: Decoder): Color {
            val surrogate = decoder.decodeSerializableValue(ColorSurrogate.serializer())
            return Color((surrogate.r shl 16) or (surrogate.g shl 8) or surrogate.b)
        }
    }

    @Serializable
    @SerialName("Color")
    private class ColorSurrogate(val r: Int, val g: Int, val b: Int) {
        init {
            require(r in 0..255 && g in 0..255 && b in 0..255)
        }
    }

    @Serializable(with = ColorSerializer::class)
    class Color(val rgb: Int)

    @Test
    fun decodingWithSurrogateSerializer() {
        val test = Toml.decodeFromString<Color>(
            """
                r = 5
                g = 6
                b = 7
            """.trimIndent()
        )
        assertEquals(Color(329223).rgb, test.rgb)
    }

    @Test
    fun decodingWithSurrogateSerializerInsideTable() {
        @Serializable
        data class WrapperTable(
            val table1: Color,
            val table2: Color,
        )

        val toml = """
            [table1]
                r = 1
                g = 2
                b = 3
            [table2]
                r = 4
                g = 5
                b = 6
        """.trimIndent()

        val result = Toml.decodeFromString<WrapperTable>(toml)
        assertEquals(Color(66051).rgb, result.table1.rgb)
        assertEquals(Color(263430).rgb, result.table2.rgb)
    }
}
