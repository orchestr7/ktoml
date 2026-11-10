package com.akuleshov7.ktoml.okio

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi

import okio.Buffer
import okio.ForwardingSource
import okio.Sink
import okio.Timeout
import okio.buffer

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalKtomlApi::class)
class OkioStreamsTest {
    @Test
    fun decodeDocument() {
        val source = Buffer().writeUtf8(DOCUMENT)

        assertEquals(Document("ktoml", Nested(2)), Toml.decodeFromBufferedSource(source))
    }

    @Test
    fun partiallyDecodeDocument() {
        val source = Buffer().writeUtf8(DOCUMENT)

        assertEquals(Nested(2), Toml.partiallyDecodeFromBufferedSource(source, "nested"))
    }

    @Test
    fun encodeWithConfiguredToml() {
        val toml = Toml(outputConfig = TomlOutputConfig(explicitTables = true))
        val value = Document("ktoml", Nested(2))
        val sink = Buffer()

        toml.encodeToBufferedSink(value, sink)

        assertEquals(toml.encodeToString(Document.serializer(), value), sink.readUtf8())
    }

    @Test
    fun leaveSourceOpen() {
        val upstream = Buffer().writeUtf8(DOCUMENT)
        var closed = false
        val source = object : ForwardingSource(upstream) {
            override fun close() {
                closed = true
                super.close()
            }
        }.buffer()

        Toml.decodeFromBufferedSource<Document>(source)

        assertFalse(closed)
        source.close()
        assertTrue(closed)
    }

    @Test
    fun leaveSinkOpen() {
        val upstream = Buffer()
        val trackingSink = TrackingSink(upstream)
        val sink = trackingSink.buffer()

        Toml.encodeToBufferedSink(Document("ktoml", Nested(2)), sink)

        assertFalse(trackingSink.closed)
        sink.close()
        assertTrue(trackingSink.closed)
    }

    @Serializable
    private data class Document(
        val title: String,
        val nested: Nested,
    )

    @Serializable
    private data class Nested(val count: Int)

    private class TrackingSink(private val delegate: Sink) : Sink {
        var closed: Boolean = false
            private set

        override fun write(source: Buffer, byteCount: Long): Unit = delegate.write(source, byteCount)

        override fun flush(): Unit = delegate.flush()

        override fun timeout(): Timeout = delegate.timeout()

        override fun close() {
            closed = true
            delegate.close()
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
