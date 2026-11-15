package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.TomlOutputConfig

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("DEPRECATION")
class TomlFileWriterTest {
    @Test
    fun preserveOutputConfiguration() {
        val writer = TomlFileWriter(outputConfig = TomlOutputConfig(explicitTables = true))
        val value = PlainDocument("ktoml", Nested(2))
        val path = Files.createTempFile("ktoml-writer-config", ".toml")

        try {
            writer.encodeToFile(PlainDocument.serializer(), value, path.toString())

            assertEquals(
                writer.encodeToString(PlainDocument.serializer(), value),
                String(Files.readAllBytes(path), StandardCharsets.UTF_8),
            )
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Test
    fun preserveSerializersModule() {
        val serializersModule = SerializersModule {
            contextual(ContextValue::class, ContextValueSerializer)
        }
        val writer = TomlFileWriter(serializersModule = serializersModule)
        val value = Document(ContextValue("ktoml"), Nested(2))
        val path = Files.createTempFile("ktoml-writer-module", ".toml")

        try {
            writer.encodeToFile(Document.serializer(), value, path.toString())

            assertEquals(
                writer.encodeToString(Document.serializer(), value),
                String(Files.readAllBytes(path), StandardCharsets.UTF_8),
            )
        } finally {
            Files.deleteIfExists(path)
        }
    }

    @Serializable
    private data class Document(
        @Contextual val title: ContextValue,
        val nested: Nested,
    )

    @Serializable
    private data class PlainDocument(
        val title: String,
        val nested: Nested,
    )

    @Serializable
    private data class Nested(val count: Int)

    private data class ContextValue(val value: String)

    private object ContextValueSerializer : KSerializer<ContextValue> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContextValue", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder): ContextValue = ContextValue(decoder.decodeString())

        override fun serialize(encoder: Encoder, value: ContextValue) {
            encoder.encodeString(value.value)
        }
    }
}
