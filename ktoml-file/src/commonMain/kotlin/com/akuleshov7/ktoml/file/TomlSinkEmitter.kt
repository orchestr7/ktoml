@file:Suppress("UNUSED_IMPORT")// TomlComposer used for documentation only

package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.writers.TomlEmitter
import okio.BufferedSink
import okio.Closeable

/**
 * A [TomlEmitter] implementation that writes to a [BufferedSink].
 */
internal class TomlSinkEmitter(
    private val sink: BufferedSink,
    config: TomlConfig
) : TomlEmitter(config), Closeable {
    override fun emit(fragment: String): TomlEmitter {
        sink.writeUtf8(fragment)

        return this
    }

    override fun emit(fragment: Char): TomlEmitter {
        sink.writeUtf8CodePoint(fragment.code)

        return this
    }

    override fun close(): Unit = sink.close()
}
