package com.akuleshov7.ktoml.encoders.custom

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.encoders.assertEncodedEquals
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
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

    // For instance, we don't control this class code and don't have a serializer for it
    data class Date(val year: Int)

    object DateAsLongSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "my.app.DateAsLong",
            PrimitiveKind.LONG,
        )

        override fun serialize(encoder: Encoder, value: Date) {
            encoder.encodeLong(value.year.toLong())
        }

        override fun deserialize(decoder: Decoder): Date {
            return Date(decoder.decodeInt())
        }
    }

    private val module = SerializersModule {
        contextual(DateAsLongSerializer)
    }

    @Test
    fun contextualSerializer() {
        @Serializable
        data class ProgrammingLanguage(
            val name: String,
            @Contextual
            val stableReleaseDate: Date
        )

        val toml = """
            name = "Kotlin"
            stableReleaseDate = 2025
        """.trimIndent()

        assertEncodedEquals(
            value = ProgrammingLanguage(
                name = "Kotlin",
                stableReleaseDate = Date(2025)
            ),
            expectedToml = toml,
            tomlInstance = Toml(serializersModule = module),
        )
    }
}