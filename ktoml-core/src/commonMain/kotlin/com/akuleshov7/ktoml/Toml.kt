package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.TomlMainDecoder
import com.akuleshov7.ktoml.exceptions.MissingRequiredFieldException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlFile

import kotlin.native.concurrent.ThreadLocal
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Toml class - is a general entry point in the core,
 * that is used to serialize/deserialize TOML file or string
 *
 * @property config - configuration for the serialization
 * @property serializersModule - default overridden
 */
@OptIn(ExperimentalSerializationApi::class)
public open class Toml(
    private val config: KtomlConf = KtomlConf(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : StringFormat {
    // parser is created once after the creation of the class, to reduce the number of created parsers for each toml
    public val tomlParser: TomlParser = TomlParser(config)

    // ================== basic overrides ===============

    /**
     * simple deserializer of a string in a toml format (separated by newlines)
     *
     * @param string - request-string in toml format with '\n' or '\r\n' separation
     * @return deserialized object of type T
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedToml = tomlParser.parseString(string)
        return TomlMainDecoder.decode(deserializer, parsedToml, config)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }

    // ================== custom decoding methods ===============

    /**
     * simple deserializer of a list of strings in a toml format
     *
     * @param toml list with strings in toml format
     * @param deserializer deserialization strategy
     * @param ktomlConf
     * @return deserialized object of type T
     */
    public fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: List<String>,
        ktomlConf: KtomlConf): T {
        val parsedToml = tomlParser.parseStringsToTomlTree(toml, ktomlConf)
        return TomlMainDecoder.decode(deserializer, parsedToml, config)
    }

    /**
     * partial deserializer of a string in a toml format (separated by newlines).
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param toml request-string in toml format with '\n' or '\r\n' separation
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param ktomlConf
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: String,
        tomlTableName: String,
        ktomlConf: KtomlConf = KtomlConf()
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(toml, tomlTableName, ktomlConf, TomlParser::parseString)
        return TomlMainDecoder.decode(deserializer, fakeFileNode, config)
    }

    /**
     * partial deserializer of a string in a toml format (separated by newlines).
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param toml list of strings with toml input
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param ktomlConf
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: List<String>,
        tomlTableName: String,
        ktomlConf: KtomlConf = KtomlConf()
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(
            toml.joinToString("\n"),
            tomlTableName,
            ktomlConf,
            TomlParser::parseString,
        )
        return TomlMainDecoder.decode(deserializer, fakeFileNode, config)
    }

    // ================== other ===============
    @Suppress("TYPE_ALIAS")
    private fun generateFakeTomlStructureForPartialParsing(
        toml: String,
        tomlTableName: String,
        ktomlConf: KtomlConf = KtomlConf(),
        parsingFunction: (TomlParser, String) -> TomlFile
    ): TomlFile {
        val parsedToml = parsingFunction(TomlParser(config), toml)
            .findTableInAstByName(tomlTableName, tomlTableName.count { it == '.' } + 1)
            ?: throw MissingRequiredFieldException(
                "Table with <$tomlTableName> name is missing in the toml input. " +
                        "Not able to decode this toml part."
            )

        // adding a fake file node to restore the structure and parse only the part of te toml
        val fakeFileNode = TomlFile(ktomlConf)
        parsedToml.children.forEach {
            fakeFileNode.appendChild(it)
        }

        return fakeFileNode
    }

    /**
     * The default instance of [Toml] with the default configuration.
     * See [KtomlConf] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : Toml(KtomlConf())
}
