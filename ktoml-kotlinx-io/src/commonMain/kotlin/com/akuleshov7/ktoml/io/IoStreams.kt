/**
 * TOML adapters for kotlinx-io sources and sinks.
 */

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.akuleshov7.ktoml.io

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import com.akuleshov7.ktoml.annotations.InternalKtomlApi

import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readLine
import kotlinx.io.writeString
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.serializer

/**
 * Decodes one TOML document from [source] using UTF-8.
 *
 * This function consumes the document but does not close [source].
 *
 * @param deserializer deserialization strategy
 * @param source source containing the TOML document
 * @return deserialized value
 */
@ExperimentalKtomlApi
public fun <T> Toml.decodeFromSource(
    deserializer: DeserializationStrategy<T>,
    source: Source,
): T = decodeFromString(deserializer, generateSequence(source::readLine))

/**
 * Decodes one TOML document from [source] using UTF-8 and the serializer for [T].
 *
 * This function consumes the document but does not close [source].
 *
 * @param source source containing the TOML document
 * @return deserialized value
 */
@ExperimentalKtomlApi
public inline fun <reified T> Toml.decodeFromSource(source: Source): T =
    decodeFromSource(serializersModule.serializer(), source)

/**
 * Decodes one table from a TOML document in [source] using UTF-8.
 *
 * This function consumes the document but does not close [source].
 *
 * @param deserializer deserialization strategy
 * @param source source containing the TOML document
 * @param tomlTableName fully qualified table name
 * @return deserialized table value
 */
@ExperimentalKtomlApi
public fun <T> Toml.partiallyDecodeFromSource(
    deserializer: DeserializationStrategy<T>,
    source: Source,
    tomlTableName: String,
): T = partiallyDecodeFromString(
    deserializer,
    generateSequence(source::readLine),
    tomlTableName,
)

/**
 * Decodes one table from a TOML document in [source] using UTF-8 and the serializer for [T].
 *
 * This function consumes the document but does not close [source].
 *
 * @param source source containing the TOML document
 * @param tomlTableName fully qualified table name
 * @return deserialized table value
 */
@ExperimentalKtomlApi
public inline fun <reified T> Toml.partiallyDecodeFromSource(
    source: Source,
    tomlTableName: String,
): T = partiallyDecodeFromSource(serializersModule.serializer(), source, tomlTableName)

/**
 * Encodes [value] as one UTF-8 TOML document into [sink].
 *
 * This function does not flush or close [sink].
 *
 * @param serializer serialization strategy
 * @param value value to encode
 * @param sink destination for the TOML document
 */
@ExperimentalKtomlApi
@OptIn(InternalKtomlApi::class)
public fun <T> Toml.encodeToSink(
    serializer: SerializationStrategy<T>,
    value: T,
    sink: Sink,
) {
    encodeToEmitter(
        serializer,
        value,
        emitString = { sink.writeString(it) },
        emitChar = { sink.writeString(it.toString()) },
    )
}

/**
 * Encodes [value] as one UTF-8 TOML document into [sink] using the serializer for [T].
 *
 * This function does not flush or close [sink].
 *
 * @param value value to encode
 * @param sink destination for the TOML document
 */
@ExperimentalKtomlApi
public inline fun <reified T> Toml.encodeToSink(value: T, sink: Sink): Unit =
    encodeToSink(serializersModule.serializer(), value, sink)
