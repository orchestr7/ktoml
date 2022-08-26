package com.akuleshov7.ktoml.encoders

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlin.test.Test

class CustomSerializerTest {
    object SinglePropertyAsStringSerializer : KSerializer<SingleProperty> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SingleProperty") {
            element<String>("rgb")
        }

        override fun serialize(encoder: Encoder, value: SingleProperty) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, "${value.rgb - 15}")
            }
        }

        override fun deserialize(decoder: Decoder): SingleProperty =
                throw UnsupportedOperationException()
    }

    @Serializable(with = SinglePropertyAsStringSerializer::class)
    data class SingleProperty(val rgb: Long = 15)

    @Test
    fun singlePropertyCustomSerializerTest() {
        assertEncodedEquals(
            value = SingleProperty(),
            expectedToml = """rgb = "0""""
        )
    }
}
