package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.DecoderConf
import com.akuleshov7.ktoml.decoders.TomlDecoder
import com.akuleshov7.ktoml.exceptions.MissingRequiredFieldException
import com.akuleshov7.ktoml.exceptions.UnknownNameDecodingException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlFile
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
    // FixMe: need to fix code duplication here
    // ================== decoders ===============
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedToml = TomlParser(string).parseString()
        return TomlDecoder.decode(deserializer, parsedToml, config)
    }

    fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, toml: String, tomlTableName: String): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(TomlParser::parseString, toml, tomlTableName)
        return TomlDecoder.decode(deserializer, fakeFileNode, config)
    }

    fun <T> decodeFromFile(deserializer: DeserializationStrategy<T>, tomlFilePath: String): T {
        val parsedToml = TomlParser(tomlFilePath).readAndParseFile()
        return TomlDecoder.decode(deserializer, parsedToml, config)
    }

    fun <T> decodeFromFile(deserializer: DeserializationStrategy<T>,  tomlFilePath: String, tomlTableName: String): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(TomlParser::readAndParseFile, tomlFilePath, tomlTableName)
        return TomlDecoder.decode(deserializer, fakeFileNode, config)
    }

    private fun generateFakeTomlStructureForPartialParsing(parsingFunction: (TomlParser) -> TomlFile, toml: String, tomlTableName: String): TomlFile {
        val parsedToml = parsingFunction(TomlParser(toml))
            .findTableInAstByName(tomlTableName,  tomlTableName.count { it == '.' } + 1)
            ?: throw MissingRequiredFieldException("Table with <$tomlTableName> name is missing in the toml input. " +
                    "Not able to decode this toml part.")

        // adding a fake file node to restore the structure and parse only the part of te toml
        val fakeFileNode = TomlFile()
        parsedToml.children.forEach {
            fakeFileNode.appendChild(it)
        }

        return fakeFileNode
    }

    // ================== encoders ===============
    fun <T> encodeToFile(deserializer: DeserializationStrategy<T>, request: T): Path {
        TODO("Not yet implemented")
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }
}

/**
 * simple deserializer of a string in a toml format (separated by newlines)
 *
 * @param request - string in toml format with '\n' or '\r\n' separation
 * @param decoderConfig - optional config to configure extra options (not required)
 */
@ExperimentalSerializationApi
inline fun <reified T : Any> deserialize(request: String, decoderConfig: DecoderConf = DecoderConf()): T {
    return Ktoml(decoderConfig).decodeFromString(serializer(), request)
}

/**
 * partial deserializer of a string in a toml format (separated by newlines). Will deserialize only the part presented
 * under the tomlTableName table. If such table is missing in he input - will throw an exception
 *
 * @param request - string in toml format with '\n' or '\r\n' separation
 * @param decoderConfig - optional config to configure extra options (not required)
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 */
@ExperimentalSerializationApi
inline fun <reified T : Any> deserialize(request: String, tomlTableName: String, decoderConfig: DecoderConf = DecoderConf()): T {
    return Ktoml(decoderConfig).decodeFromString(serializer(), request, tomlTableName)
}

/**
 * simple deserializer of a file that contains toml. Reading file with okio native library
 *
 * @param tomlFilePath - path to the file where toml is stored
 * @param decoderConfig - optional config to configure extra options (not required)
 */
@ExperimentalSerializationApi
inline fun <reified T : Any> deserializeFile(tomlFilePath: String, decoderConfig: DecoderConf = DecoderConf()): T {
    return Ktoml(decoderConfig).decodeFromFile(serializer(), tomlFilePath)
}

/**
 * partial deserializer of a file that contains toml. Reading file with okio native library. Will deserialize only the part presented
 * under the tomlTableName table. If such table is missing in he input - will throw an exception.
 *
 * @param tomlFilePath - path to the file where toml is stored
 * @param decoderConfig - optional config to configure extra options (not required)
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 */
@ExperimentalSerializationApi
inline fun <reified T : Any> deserializeFile(tomlFilePath: String, tomlTableName: String, decoderConfig: DecoderConf = DecoderConf()): T {
    return Ktoml(decoderConfig).decodeFromFile(serializer(), tomlFilePath, tomlTableName)
}

@ExperimentalSerializationApi
inline fun <reified T : Any> serialize(request: T, encoderConfig: DecoderConf = DecoderConf()): String {
    return Ktoml(encoderConfig).encodeToString(serializer(), request)
}
