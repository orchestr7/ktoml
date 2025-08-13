package com.akuleshov7.ktoml.encoders.custom

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.encoders.assertEncodedEquals
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun serializerWithSingleProperty() {
        assertEncodedEquals(
            value = SingleProperty(),
            expectedToml = """
                rgb = "0"
            """.trimIndent()
        )
    }

    object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Color") {
        element<String>("rgb")
    }

        override fun serialize(encoder: Encoder, value: Color) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(SinglePropertyAsStringSerializer.descriptor, 0, "${value.rgb - 15}")
            }
        }

        override fun deserialize(decoder: Decoder): Color =
            throw UnsupportedOperationException()
    }

    @Serializable(with = ColorAsStringSerializer::class)
    data class Color(val rgb: Long)

    @Serializable
    data class Settings(
        val background: Color,
        val foreground: Color,
    )

    @Test
    fun tableValueCustomSerializer() {
        assertEncodedEquals(
            value = Settings(Color(16), Color(17)),
            expectedToml = """
                [background]
                    rgb = "1"

                [foreground]
                    rgb = "2"
            """.trimIndent()
        )
    }



    object ColorIntArraySerializer : KSerializer<ColorIntArray> {
        private val delegateSerializer = IntArraySerializer()

        // Serial names of descriptors should be unique, this is why we advise including app package in the name.
        override val descriptor = SerialDescriptor("my.app.ColorIntArray", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: ColorIntArray) {
            val data = intArrayOf(
                (value.rgb shr 16) and 0xFF,
                (value.rgb shr 8) and 0xFF,
                value.rgb and 0xFF
            )
            encoder.encodeSerializableValue(delegateSerializer, data)
        }

        override fun deserialize(decoder: Decoder): ColorIntArray {
            val array = decoder.decodeSerializableValue(delegateSerializer)
            return ColorIntArray((array[0] shl 16) or (array[1] shl 8) or array[2])
        }
    }

    @Serializable(with = ColorIntArraySerializer::class)
    data class ColorIntArray(val rgb: Int)

//    @Test
    fun delegatingSerializer() {
        assertEncodedEquals(
            value = ColorIntArray((1 shl 16) or (2 shl 8) or 3),
            expectedToml = """
                rgb = [1, 2, 3]
            """.trimIndent()
        )
    }






    // For instance, we don't control this class code and don't have a serializer for it
    data class Date(val date: LocalDate)

    object DateAsLongSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "my.app.DateAsLong",
            PrimitiveKind.LONG,
        )

        override fun serialize(encoder: Encoder, value: Date) {
            encoder.encodeLong(value.date.year.toLong())
        }

        override fun deserialize(decoder: Decoder): Date {
            return Date(LocalDate(decoder.decodeInt(), 1, 1))
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
                stableReleaseDate = Date(LocalDate(2025, 1, 1))
            ),
            expectedToml = toml,
            tomlInstance = Toml(serializersModule = module),
        )
    }
}