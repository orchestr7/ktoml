package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.TomlDecoder
import com.akuleshov7.ktoml.exceptions.MissingRequiredFieldException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlFile

import okio.ExperimentalFileSystem

import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * KtomlSerializer class - is a general class in the core, that is used to serialize/deserialize TOML file or string
 *
 * @property config - configuration for the serialization
 * @property serializersModule - default overridden
 */
@ExperimentalSerializationApi
class Toml(
    private val config: KtomlConf = KtomlConf(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : StringFormat {
    // this is moved to properties to reduce the number of created classes for each toml
    val tomlParser = TomlParser(config)

    // FixMe: need to fix code duplication here
    // ================== basic overrides ===============
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedToml = tomlParser.parseString(string)
        return TomlDecoder.decode(deserializer, parsedToml, config)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }

    // ================== custom decoding methods ===============

    /**
     * @param deserializer
     * @param toml
     * @param tomlTableName
     * @return decoded object of type T
     */
    fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: String,
        tomlTableName: String
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(toml, tomlTableName, TomlParser::parseString)
        return TomlDecoder.decode(deserializer, fakeFileNode, config)
    }

    /**
     * @param deserializer
     * @param tomlFilePath
     * @return decoded object of type T
     */
    @ExperimentalFileSystem
    fun <T> decodeFromFile(deserializer: DeserializationStrategy<T>, tomlFilePath: String): T {
        val parsedToml = TomlParser(config).readAndParseFile(tomlFilePath)
        return TomlDecoder.decode(deserializer, parsedToml, config)
    }

    /**
     * @param deserializer
     * @param tomlFilePath
     * @param tomlTableName
     * @return decoded object of type T
     */
    @ExperimentalFileSystem
    fun <T> partiallyDecodeFromFile(
        deserializer: DeserializationStrategy<T>,
        tomlFilePath: String,
        tomlTableName: String
    ): T {
        val fakeFileNode =
                generateFakeTomlStructureForPartialParsing(tomlFilePath, tomlTableName, TomlParser::readAndParseFile)
        return TomlDecoder.decode(deserializer, fakeFileNode, config)
    }

    @Suppress("TYPE_ALIAS")
    private fun generateFakeTomlStructureForPartialParsing(
        toml: String,
        tomlTableName: String,
        parsingFunction: (TomlParser, String) -> TomlFile
    ): TomlFile {
        val parsedToml = parsingFunction(TomlParser(config), toml)
            .findTableInAstByName(tomlTableName, tomlTableName.count { it == '.' } + 1)
            ?: throw MissingRequiredFieldException(
                "Table with <$tomlTableName> name is missing in the toml input. " +
                        "Not able to decode this toml part."
            )

        // adding a fake file node to restore the structure and parse only the part of te toml
        val fakeFileNode = TomlFile()
        parsedToml.children.forEach {
            fakeFileNode.appendChild(it)
        }

        return fakeFileNode
    }
}
