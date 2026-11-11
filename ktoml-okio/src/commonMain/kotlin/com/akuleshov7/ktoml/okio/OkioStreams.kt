/**
 * TOML adapters for Okio buffered sources and sinks.
 */

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package com.akuleshov7.ktoml.okio

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import com.akuleshov7.ktoml.annotations.InternalKtomlApi

import okio.BufferedSink
import okio.BufferedSource

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
public fun <T> Toml.decodeFromBufferedSource(
    deserializer: DeserializationStrategy<T>,
    source: BufferedSource,
): T = decodeFromString(deserializer, generateSequence(source::readUtf8Line))

/**
 * Decodes one TOML document from [source] using UTF-8 and the serializer for [T].
 *
 * This function consumes the document but does not close [source].
 *
 * @param source source containing the TOML document
 * @return deserialized value
 */
@ExperimentalKtomlApi
public inline fun <reified T> Toml.decodeFromBufferedSource(source: BufferedSource): T =
    decodeFromBufferedSource(serializersModule.serializer(), source)

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
public fun <T> Toml.partiallyDecodeFromBufferedSource(
    deserializer: DeserializationStrategy<T>,
    source: BufferedSource,
    tomlTableName: String,
): T = partiallyDecodeFromString(
    deserializer,
    generateSequence(source::readUtf8Line),
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
public inline fun <reified T> Toml.partiallyDecodeFromBufferedSource(
    source: BufferedSource,
    tomlTableName: String,
): T = partiallyDecodeFromBufferedSource(serializersModule.serializer(), source, tomlTableName)

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
public fun <T> Toml.encodeToBufferedSink(
    serializer: SerializationStrategy<T>,
    value: T,
    sink: BufferedSink,
) {
    encodeToEmitter(
        serializer,
        value,
        emitString = { sink.writeUtf8(it) },
        emitChar = { sink.writeUtf8CodePoint(it.code) },
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
public inline fun <reified T> Toml.encodeToBufferedSink(value: T, sink: BufferedSink): Unit =
    encodeToBufferedSink(serializersModule.serializer(), value, sink)
