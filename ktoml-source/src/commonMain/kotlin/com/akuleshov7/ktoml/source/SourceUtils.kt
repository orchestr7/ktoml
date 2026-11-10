/**
 * Utility methods for the ktoml-source module
 */

package com.akuleshov7.ktoml.source

import com.akuleshov7.ktoml.annotations.InternalKtomlApi

import okio.Source
import okio.buffer
import okio.use

/**
 * Read from source one line at a time and passes the lines to the [decoder] function.
 *
 * @param decoder
 * @return decoded lines
 */
@Deprecated(
    "Use a BufferedSource with the ktoml-okio extensions. " +
            "This compatibility helper closes the source after decoding."
)
@InternalKtomlApi
public inline fun <T> Source.useLines(decoder: (Sequence<String>) -> T): T = buffer().use { source ->
    decoder(generateSequence { source.readUtf8Line() })
}
