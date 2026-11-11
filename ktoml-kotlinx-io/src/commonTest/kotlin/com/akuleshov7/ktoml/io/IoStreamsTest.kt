package com.akuleshov7.ktoml.io

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.buffered
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalKtomlApi::class)
class IoStreamsTest {
    @Test
    fun decodeDocument() {
        val source = Buffer().apply { writeString(DOCUMENT) }

        assertEquals(Document("ktoml", Nested(2)), Toml.decodeFromSource(source))
    }

    @Test
    fun partiallyDecodeDocument() {
        val source = Buffer().apply { writeString(DOCUMENT) }

        assertEquals(Nested(2), Toml.partiallyDecodeFromSource(source, "nested"))
    }

    @Test
    fun encodeWithConfiguredToml() {
        val toml = Toml(outputConfig = TomlOutputConfig(explicitTables = true))
        val value = Document("ktoml", Nested(2))
        val sink = Buffer()

        toml.encodeToSink(value, sink)

        assertEquals(toml.encodeToString(Document.serializer(), value), sink.readString())
    }

    @Test
    fun leaveSourceOpen() {
        val rawSource = TrackingRawSource(DOCUMENT)
        val source = rawSource.buffered()

        Toml.decodeFromSource<Document>(source)

        assertFalse(rawSource.closed)
        source.close()
        assertTrue(rawSource.closed)
    }

    @Test
    fun leaveSinkOpen() {
        val rawSink = TrackingRawSink()
        val sink = rawSink.buffered()

        Toml.encodeToSink(Document("ktoml", Nested(2)), sink)

        assertFalse(rawSink.closed)
        sink.close()
        assertTrue(rawSink.closed)
    }

    @Serializable
    private data class Document(
        val title: String,
        val nested: Nested,
    )

    @Serializable
    private data class Nested(val count: Int)

    private class TrackingRawSource(input: String) : RawSource {
        private val buffer: Buffer = Buffer().apply { writeString(input) }
        var closed: Boolean = false
            private set

        override fun readAtMostTo(sink: Buffer, byteCount: Long): Long = buffer.readAtMostTo(sink, byteCount)

        override fun close() {
            closed = true
        }
    }

    private class TrackingRawSink : RawSink {
        var closed: Boolean = false
            private set

        override fun write(source: Buffer, byteCount: Long) {
            source.skip(byteCount)
        }

        override fun flush(): Unit = Unit

        override fun close() {
            closed = true
        }
    }

    private companion object {
        private val DOCUMENT: String = """
            title = "ktoml"

            [nested]
            count = 2
        """.trimIndent()
    }
}
