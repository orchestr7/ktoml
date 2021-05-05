package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.DecoderConf
import com.akuleshov7.ktoml.decoders.TomlDecoder
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import okio.ExperimentalFileSystem
import okio.Path

/**
 * Ktoml class - is a general class, that should be used to parse toml
 *
 * @param config - configuration for the serialization
 * @param serializersModule - default overridden
 *
 */
@OptIn(ExperimentalSerializationApi::class, ExperimentalFileSystem::class)
public class Ktoml(
    val config: DecoderConf,
    override val serializersModule: SerializersModule = EmptySerializersModule
) : StringFormat {
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedToml = TomlParser(string).parseString()
        return TomlDecoder.decode(deserializer, parsedToml, config)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }

    fun <T> decodeFromFile(deserializer: DeserializationStrategy<T>, file: Path): T {
        TODO("Not yet implemented")
    }

    fun <T> encodeToFile(deserializer: DeserializationStrategy<T>, request: T): Path {
        TODO("Not yet implemented")
    }
}

@ExperimentalSerializationApi
inline fun <reified T : Any> deserialize(request: String, decoderConfig: DecoderConf = DecoderConf()): T {
    return Ktoml(decoderConfig).decodeFromString(serializer(), request)
}

@ExperimentalSerializationApi
inline fun <reified T : Any> serialize(request: T, encoderConfig: DecoderConf = DecoderConf()): String {
    return Ktoml(encoderConfig).encodeToString(serializer(), request)
}
