package com.akuleshov7.ktoml.decoders

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Ignore
import kotlin.test.Test

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
        """
            rgb = "0" 
        """.trimIndent()
            .shouldDecodeInto(SingleProperty(15))
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
        """
            rgb = "0" 
            brg = "1"
        """.shouldDecodeInto(SeveralProperties(15, 1))
    }

    @Serializable
    data class Settings(val background: SingleProperty, val foreground: SingleProperty)

    @Test
    @Ignore
    fun testDecodingWithCustomSerializer() {
        """
            [background]
                rgb = "0"
            [foreground]
                rgb = "0"
        """.shouldDecodeInto(Settings(SingleProperty(15), SingleProperty(15)))
    }
}
