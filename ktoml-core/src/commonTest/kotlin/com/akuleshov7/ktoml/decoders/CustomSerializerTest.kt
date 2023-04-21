package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class CustomSerializerTest {
    object SinglePropertyAsStringSerializer : KSerializer<SingleProperty> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SingleProperty", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SingleProperty) {
        }

        override fun deserialize(decoder: Decoder): SingleProperty {
            val string = decoder.decodeString()
            return SingleProperty(string.toLong() + 15)
        }
    }

    @Serializable(with = SinglePropertyAsStringSerializer::class)
    data class SingleProperty(val rgb: Long)

    @Test
    fun testDecodingWithCustomSerializerSingleProperty() {
        assertEquals(
            SingleProperty(15),
            Toml.decodeFromString(
                """
                    rgb = "0" 
                """.trimIndent()
            )
        )
    }

    @Serializable(with = SeveralPropertiesAsStringSerializer::class)
    data class SeveralProperties(val rgb: Long, val brg: Long)

    object SeveralPropertiesAsStringSerializer : KSerializer<SeveralProperties> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SeveralProperties) {
        }

        override fun deserialize(decoder: Decoder): SeveralProperties {
            val string = decoder.decodeString()
            return SeveralProperties(string.toLong() + 15, string.toLong())
        }
    }

    @Test
    @Ignore
    fun testDecodingWithCustomSerializerSeveralProperties() {
        assertEquals(
            SeveralProperties(15, 1),
            Toml.decodeFromString(
                """
                    rgb = "0" 
                    brg = "1"
                """
            )
        )
        UInt.MAX_VALUE
    }

    @Serializable
    data class Settings(val background: SingleProperty, val foreground: SingleProperty)

    @Test
    @Ignore
    fun testDecodingWithCustomSerializer() {
        Toml.decodeFromString<Settings>(
            """
                [background]
                    rgb = "0"
                [foreground]
                    rgb = "0"
            """
        )
    }
}
