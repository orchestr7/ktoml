package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.exceptions.TomlEncodingException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("DEPRECATION")
class TomlFileWriterFailureTest {
    private object ThrowingSerializer : KSerializer<Unit> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ThrowingSerializer")

        override fun deserialize(decoder: Decoder): Nothing = error("Encode-only test serializer")

        override fun serialize(encoder: Encoder, value: Unit): Nothing = throw SerializationException("Cannot encode")
    }

    @Test
    fun preservesExistingFileWhenSerializerThrows() {
        assertExistingFilePreserved { path ->
            assertFailsWith<SerializationException> {
                TomlFileWriter().encodeToFile(ThrowingSerializer, Unit, path)
            }
        }
    }

    @Test
    fun preservesExistingFileWhenValueIsUnsupported() {
        assertExistingFilePreserved { path ->
            assertFailsWith<TomlEncodingException> {
                TomlFileWriter().encodeToFile(serializer<Map<Int, String>>(), mapOf(1 to "one"), path)
            }
        }
    }

    private fun assertExistingFilePreserved(encode: (String) -> Unit) {
        val fileSystem = getOsSpecificFileSystem()
        val path = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ktoml-writer-${Random.nextLong()}.toml"
        val original = "title = \"keep this document\"\n"
        try {
            fileSystem.write(path) { writeUtf8(original) }
            encode(path.toString())
            assertEquals(original, fileSystem.read(path) { readUtf8() })
        } finally {
            fileSystem.delete(path, mustExist = false)
        }
    }
}
