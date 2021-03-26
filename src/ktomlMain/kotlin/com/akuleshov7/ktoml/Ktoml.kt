package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.TomlDecoder
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Ktoml class - is a general class, that should be used to parse toml
 *
 * @param
 *
 */
@OptIn(ExperimentalSerializationApi::class)
public class Ktoml(
    override val serializersModule: SerializersModule = EmptySerializersModule,
) : StringFormat {

    fun <T> readToml(): T {
        TODO()
    }

    fun <T> writeToml(obj: T): List<String> {
        TODO()
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedTomlNode = TomlParser(string).parseString()
        return TomlDecoder.decode(deserializer, parsedTomlNode)
    }

    fun <T> decodeFromFile(deserializer: DeserializationStrategy<T>, tomlStr: String): T {
        TODO()
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }
}

