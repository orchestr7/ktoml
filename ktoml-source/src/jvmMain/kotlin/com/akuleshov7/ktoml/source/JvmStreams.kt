/**
 * Utility methods for the decoding of JVM streams
 */

package com.akuleshov7.ktoml.source

import com.akuleshov7.ktoml.Toml

import okio.source

import java.io.InputStream

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.serializer

/**
 * Deserializes TOML from [stream] using UTF-8 encoding to a value of type [T] using [deserializer].
 *
 * @param deserializer
 * @param stream
 * @return decoded lines
 */
public fun <T> Toml.decodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream
): T = stream.source().useLines {
    decodeFromString(deserializer, it)
}

/**
 * Deserializes the contents of given [stream] to the value of type [T] using UTF-8 encoding and
 * deserializer retrieved from the reified type parameter.
 *
 * @param stream
 */
public inline fun <reified T> Toml.decodeFromStream(stream: InputStream): T = decodeFromStream(serializersModule.serializer(), stream)

/**
 * Partial deserializer of a stream that contains toml.
 * Will deserialize only the part presented under the tomlTableName table.
 * If such table is missing in he input - will throw an exception.
 *
 * (!) Useful when you would like to deserialize only ONE table
 * and you do not want to reproduce whole object structure in the code
 *
 * @param deserializer deserialization strategy
 * @param stream stream where toml is stored
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 * @return deserialized object of type T
 */
public fun <T> Toml.partiallyDecodeFromStream(
    deserializer: DeserializationStrategy<T>,
    stream: InputStream,
    tomlTableName: String
): T = stream.source().useLines {
    partiallyDecodeFromLines(deserializer, it, tomlTableName)
}

/**
 * Partial deserializer of a stream that contains toml.
 * Will deserialize only the part presented under the tomlTableName table.
 * If such table is missing in he input - will throw an exception.
 *
 * (!) Useful when you would like to deserialize only ONE table
 * and you do not want to reproduce whole object structure in the code
 *
 * @param stream stream where toml is stored
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 * @return deserialized object of type T
 */
public inline fun <reified T> Toml.partiallyDecodeFromStream(
    stream: InputStream,
    tomlTableName: String
): T = partiallyDecodeFromStream(serializersModule.serializer(), stream, tomlTableName)
