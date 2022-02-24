package com.akuleshov7.ktoml.source

import okio.Source
import okio.buffer
import okio.use

/**
 * Read from source one line at a time and passes the lines to the [decoder] function.
 */
public inline fun <T> Source.useLines(decoder: (Sequence<String>) -> T): T {
    return buffer().use { source ->
        decoder(generateSequence { source.readUtf8Line() })
    }
}