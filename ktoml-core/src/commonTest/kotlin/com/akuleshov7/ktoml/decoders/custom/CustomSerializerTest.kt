package com.akuleshov7.ktoml.decoders.custom

import com.akuleshov7.ktoml.Toml
import kotlinx.datetime.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
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
    fun primitiveSerializerWithSingleProperty() {
        assertEquals(
            SingleProperty(15),
            Toml.decodeFromString(
                """
                    rgb = "0"
                """.trimIndent()
            )
        )
    }

    object ColorAsStringSerializer : KSerializer<Color> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Color) {
        }

        override fun deserialize(decoder: Decoder): Color {
            val value = decoder.decodeString()
            return Color(value.toLong())
        }
    }

    @Serializable(with = ColorAsStringSerializer::class)
    data class Color(val rgb: Long)

    @Serializable
    data class Settings(
        val background: Color,
        val foreground: Color,
    )

    @Test
    fun primitiveInsideTableCustomSerializer() {
        assertEquals(
            Settings(Color(1), Color(2)),
            Toml.decodeFromString<Settings>(
                """
                [background]
                    rgb = "1"
                [foreground]
                    rgb = "2"
            """
            ),
        )
    }

    class ColorIntArraySerializer : KSerializer<ColorIntArray> {
        private val delegateSerializer = IntArraySerializer()

        // Serial names of descriptors should be unique, this is why we advise including app package in the name.
        override val descriptor = SerialDescriptor("my.app.ColorIntArray", delegateSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: ColorIntArray) {
        }

        override fun deserialize(decoder: Decoder): ColorIntArray {
            val array = decoder.decodeSerializableValue(delegateSerializer)
            return ColorIntArray((array[0] shl 16) or (array[1] shl 8) or array[2])
        }
    }

    @Serializable(with = ColorIntArraySerializer::class)
    data class ColorIntArray(val rgb: Int)

    @Test
    fun delegatingSerializer() {
        val toml = """
            rgb = [1, 2, 3]
        """.trimIndent()

        assertEquals(
            ColorIntArray((1 shl 16) or (2 shl 8) or 3),
            Toml.decodeFromString<ColorIntArray>(toml),
        )
    }

    @Test
    fun delegatingSerializerInsideTable() {
        @Serializable
        data class RgbArrayInsideTable(
            val background: ColorIntArray,
            val foreground: ColorIntArray,
        )

        val toml = """
            [background]
                rgb = [1, 2, 3]
            [foreground]
                rgb = [4, 5, 6]
        """.trimIndent()

        assertEquals(
            RgbArrayInsideTable(
                background = ColorIntArray((1 shl 16) or (2 shl 8) or 3),
                foreground = ColorIntArray((4 shl 16) or (5 shl 8) or 6),
            ),
            Toml.decodeFromString<RgbArrayInsideTable>(toml),
        )
    }

    object ColorAsObjectSerializer : KSerializer<ColorHandwritten> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("my.app.ColorHandwritten") {
                element<Int>("r")
                element<Int>("g")
                element<Int>("b")
            }

        override fun deserialize(decoder: Decoder): ColorHandwritten {
            return decoder.decodeStructure(descriptor) {
                var r = -1
                var g = -1
                var b = -1
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> r = decodeIntElement(descriptor, 0)
                        1 -> g = decodeIntElement(descriptor, 1)
                        2 -> b = decodeIntElement(descriptor, 2)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                require(r in 0..255 && g in 0..255 && b in 0..255)

                ColorHandwritten((r shl 16) or (g shl 8) or b)
            }
        }

        override fun serialize(encoder: Encoder, value: ColorHandwritten) {
        }
    }

    @Serializable(with = ColorAsObjectSerializer::class)
    data class ColorHandwritten(val rgb: Int)

    @Test
    fun handwrittenCompositeSerializer() {
        val toml = """
            r = 1
            g = 2
            b = 3
        """.trimIndent()

        assertEquals(
            ColorHandwritten((1 shl 16) or (2 shl 8) or 3),
            Toml.decodeFromString<ColorHandwritten>(toml),
        )
    }

    @Test
    fun handwrittenCompositeSerializerInsideTable() {
        @Serializable
        data class ColorHandwrittenInsideTable(
            val background: ColorHandwritten,
            val foreground: ColorHandwritten,
        )

        val toml = """
            [background]
                r = 1
                g = 2
                b = 3
            [foreground]
                r = 4
                g = 5
                b = 6
        """.trimIndent()

        assertEquals(
            ColorHandwrittenInsideTable(
                ColorHandwritten((1 shl 16) or (2 shl 8) or 3),
                ColorHandwritten((4 shl 16) or (5 shl 8) or 6),
            ),
            Toml.decodeFromString<ColorHandwrittenInsideTable>(toml),
        )
    }

    object DateAsLongSerializer : KSerializer<Date> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
            "my.app.DateAsLong",
            PrimitiveKind.LONG,
        )

        override fun serialize(encoder: Encoder, value: Date) {
        }

        override fun deserialize(decoder: Decoder): Date {
            return Date(LocalDate(decoder.decodeInt(), 1, 1))
        }
    }

    // For instance, we don't control this class code and don't have a serializer for it
    data class Date(val date: LocalDate)

    @Test
    fun thirdPartyClass() {
        val toml = """
            year = 2025
        """.trimIndent()

        assertEquals(
            Date(LocalDate(2025, 1, 1)),
            Toml.decodeFromString(DateAsLongSerializer, toml),
        )
    }

    @Serializable
    data class ProgrammingLanguage(
        val name: String,
        @Serializable(with = DateAsLongSerializer::class)
        val stableReleaseDate: Date
    )

    @Test
    fun specifyingSerializerOnProperty() {
        val toml = """
            name = "Kotlin"
            stableReleaseDate = 2025
        """.trimIndent()

        assertEquals(
            ProgrammingLanguage("Kotlin", Date(LocalDate(2025, 1, 1))),
            Toml.decodeFromString<ProgrammingLanguage>(toml),
        )
    }


    @Serializable
    data class ProgrammingLanguages(
        val name: String,
        val releaseDates: List<@Serializable(DateAsLongSerializer::class) Date>
    )

    @Test
    fun specifyingSerializerForParticularType() {
        val toml = """
            name = "Kotlin"
            releaseDates = [2025, 2026]
        """.trimIndent()

        assertEquals(
            ProgrammingLanguages(
                "Kotlin",
                listOf(
                    Date(LocalDate(2025, 1, 1)),
                    Date(LocalDate(2026, 1, 1))
                ),
            ),
            Toml.decodeFromString<ProgrammingLanguages>(toml),
        )
    }


    @Serializable(with = BoxSerializer::class)
    data class Box<T>(val contents: T)

    @Serializable
    data class Project(val name: String)

    class BoxSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Box<T>> {
        override val descriptor: SerialDescriptor = SerialDescriptor("my.app.Box", dataSerializer.descriptor)

        override fun serialize(encoder: Encoder, value: Box<T>) {
        }

        override fun deserialize(decoder: Decoder) = Box(dataSerializer.deserialize(decoder))
    }

    @Test
    fun customSerializerForGenericType() {
        val toml = """
            name = "Kotlin"
        """.trimIndent()

        assertEquals(
            Box(Project("Kotlin")),
            Toml.decodeFromString<Box<Project>>(toml),
        )
    }

    @Test
    fun customSerializerForGenericTypeInsideTable() {
        @Serializable
        data class ProjectInsideTable(
            val project1: Box<Project>,
            val project2: Box<Project>,
        )

        val toml = """
            [project1]
                name = "Kotlin"
            [project2]
                name = "Java"
        """.trimIndent()

        assertEquals(
            ProjectInsideTable(Box(Project("Kotlin")), Box(Project("Java"))),
            Toml.decodeFromString<ProjectInsideTable>(toml),
        )
    }
}
