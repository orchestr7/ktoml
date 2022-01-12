@file:Suppress("UNUSED_IMPORT")// TomlComposer used for documentation only

package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.writers.TomlEmitter
import okio.BufferedSink
import okio.Closeable

/**
 * A [TomlEmitter] implementation that writes to a [BufferedSink].
 */
internal class TomlSinkEmitter(
    private val sink: BufferedSink,
    ktomlConf: KtomlConf
) : TomlEmitter(ktomlConf), Closeable {
    override fun emit(fragment: String) {
        sink.writeUtf8(fragment)
    }

    override fun emit(fragment: Char) {
        sink.writeUtf8CodePoint(fragment.code)
    }

    override fun close(): Unit = sink.close()
}
