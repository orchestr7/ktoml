package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlIndentation
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlLiteral
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeCollection
import kotlin.test.Test

class TomlDocsEncoderTest {
    @Serializable
    data class ReadMe(
        val title: String = "TOML Example",
        val owner: Owner = Owner(),
        val database: Database = Database(),
        val servers: Servers = Servers()
    )

    @Serializable
    data class Owner(
        val name: String = "Tom Preston-Werner",
        val dob: Instant =
                LocalDateTime(1979, 5, 27, 15, 32, 0)
                    .toInstant(TimeZone.UTC)
    )

    @Serializable
    data class Database(
        val enabled: Boolean = true,
        val ports: List<Long> = listOf(8000, 8001, 8002),
        @Serializable(with = DataSerializer::class)
        val data: List<List<@Contextual Any>> = listOf(listOf("delta", "phi"), listOf(3.14)),
        @SerialName("temp_targets")
        @TomlInlineTable
        val tempTargets: Map<String, Double> = mapOf("cpu" to 79.5, "case" to 72.0)
    ) {
        // Serializing this as a hard-coded list instead of making Any @Polymorphic
        // because these would be serialized as [ "kotlin.<type>", <value> ] rather
        // than as primitives. Making an exception for primitives in the encoder
        // works, but could make types difficult to resolve on deserialization.
        @OptIn(ExperimentalSerializationApi::class)
        object DataSerializer : KSerializer<List<List<Any>>> {
            @OptIn(InternalSerializationApi::class)
            override val descriptor = buildSerialDescriptor("data", StructureKind.LIST) {
                element<List<String>>("0")
                element<List<Double>>("1")
            }

            @Suppress("UNCHECKED_CAST")
            override fun serialize(encoder: Encoder, value: List<List<Any>>) {
                encoder.encodeCollection(descriptor, 2) {
                    encodeSerializableElement(descriptor, 0, serializer<_>(), value[0] as List<String>)
                    encodeSerializableElement(descriptor, 1, serializer<_>(), value[1] as List<Double>)
                }
            }

            override fun deserialize(decoder: Decoder): List<List<Any>> {
                throw IllegalStateException()
            }
        }
    }

    @Serializable
    data class Servers(
        val alpha: Entry = Entry("10.0.0.1", "frontend"),
        val beta: Entry = Entry("10.0.0.2", "backend")
    ) {
        @Serializable
        data class Entry(val ip: String, val role: String)
    }

    @Test
    fun readMeTest() {
        assertEncodedEquals(
            value = ReadMe(),
            expectedToml = """
                title = "TOML Example"
                
                [owner]
                name = "Tom Preston-Werner"
                dob = 1979-05-27T15:32:00Z
                
                [database]
                enabled = true
                ports = [ 8000, 8001, 8002 ]
                data = [ [ "delta", "phi" ], [ 3.14 ] ]
                temp_targets = { cpu = 79.5, case = 72.0 }
                
                [servers]
                [servers.alpha]
                ip = "10.0.0.1"
                role = "frontend"
                
                [servers.beta]
                ip = "10.0.0.2"
                role = "backend"
            """.trimIndent(),
            tomlInstance = Toml(
                outputConfig = TomlOutputConfig.compliant(
                    indentation = TomlIndentation.NONE,
                    explicitTables = true
                )
            )
        )
    }

    @Test
    fun basicStringTest() {
        @Serializable
        data class File(
            val str1: String = "I'm a string.",
            val str2: String = "You can \"quote\" me.",
            val str3: String = "Name\tJos\u00E9\nLoc\tSF."
        )

        assertEncodedEquals(
            value = File(),
            expectedToml = """
                str1 = "I'm a string."
                str2 = "You can \"quote\" me."
                str3 = "Name	José\nLoc	SF."
            """.trimIndent()
        )
    }

    @Test
    fun literalStringTest() {
        @Serializable
        data class File(
            @TomlLiteral
            val winpath: String = """C:\Users\nodejs\templates""",
            @TomlLiteral
            val winpath2: String = """\\ServerX\admin${'$'}\system32\""",
            @TomlLiteral
            val quoted: String = """Tom "Dubs" Preston-Werner""",
            @TomlLiteral
            val regex: String = """<\i\c*\s*>"""
        )

        assertEncodedEquals(
            value = File(),
            expectedToml = """
                winpath = 'C:\Users\nodejs\templates'
                winpath2 = '\\ServerX\admin${'$'}\system32\'
                quoted = 'Tom "Dubs" Preston-Werner'
                regex = '<\i\c*\s*>'
            """.trimIndent()
        )
    }
}
