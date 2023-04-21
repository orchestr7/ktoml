package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.TomlMainDecoder
import com.akuleshov7.ktoml.encoders.TomlMainEncoder
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.utils.findPrimitiveTableInAstByName
import com.akuleshov7.ktoml.writers.TomlWriter
import kotlin.native.concurrent.ThreadLocal
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Toml class - is a general entry point in the core,
 * that is used to serialize/deserialize TOML file or string
 *
 * @property inputConfig - configuration for deserialization
 * @property outputConfig - configuration for serialization
 * @property serializersModule - default overridden
 */
@OptIn(ExperimentalSerializationApi::class)
public open class Toml(
    protected val inputConfig: TomlInputConfig = TomlInputConfig(),
    protected val outputConfig: TomlOutputConfig = TomlOutputConfig(),
    override val serializersModule: SerializersModule = EmptySerializersModule(),
) : StringFormat {
    // parser and writer are created once after the creation of the class, to reduce
    // the number of created parsers and writers for each toml
    public val tomlParser: TomlParser = TomlParser(inputConfig)
    public val tomlWriter: TomlWriter = TomlWriter(outputConfig)

    // ================== basic overrides ===============

    /**
     * simple deserializer of a string in a toml format (separated by newlines)
     *
     * @param string - request-string in toml format with '\n' or '\r\n' separation
     * @return deserialized object of type T
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedToml = tomlParser.parseString(string)
        return TomlMainDecoder.decode(deserializer, parsedToml, inputConfig)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val toml = TomlMainEncoder.encode(serializer, value, outputConfig, serializersModule)
        return tomlWriter.writeToString(file = toml)
    }

    // ================== custom decoding methods ===============

    /**
     * simple deserializer of a list of strings in a toml format
     *
     * @param toml list with strings in toml format
     * @param deserializer deserialization strategy
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: List<String>,
        config: TomlInputConfig = this.inputConfig
    ): T = decodeFromString(deserializer, toml.asSequence(), config)

    /**
     * simple deserializer of a sequence of strings in a toml format
     *
     * @param toml sequence with strings in toml format
     * @param deserializer deserialization strategy
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: Sequence<String>,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val parsedToml = tomlParser.parseStringsToTomlTree(toml, config)
        return TomlMainDecoder.decode(deserializer, parsedToml, this.inputConfig)
    }

    /**
     * partial deserializer of a sequence of lines in a toml format.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param tomlLines sequence of TOML lines
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromLines(
        deserializer: DeserializationStrategy<T>,
        tomlLines: Sequence<String>,
        tomlTableName: String,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(tomlLines, tomlTableName, config, TomlParser::parseLines)
        return TomlMainDecoder.decode(deserializer, fakeFileNode, this.inputConfig)
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
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: String,
        tomlTableName: String,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(toml, tomlTableName, config, TomlParser::parseString)
        return TomlMainDecoder.decode(deserializer, fakeFileNode, config)
    }

    /**
     * partial deserializer of a sequence of lines in a toml format.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param tomlLines sequence of strings with toml input
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        tomlLines: Sequence<String>,
        tomlTableName: String,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(
            tomlLines,
            tomlTableName,
            config,
            TomlParser::parseLines,
        )
        return TomlMainDecoder.decode(deserializer, fakeFileNode, this.inputConfig)
    }

    // ================== other ===============
    @Suppress("TYPE_ALIAS")
    private fun <I> generateFakeTomlStructureForPartialParsing(
        tomlInput: I,
        tomlTableName: String,
        config: TomlInputConfig = TomlInputConfig(),
        parsingFunction: (TomlParser, I) -> TomlFile
    ): TomlFile {
        val tomlFile = parsingFunction(TomlParser(config), tomlInput)
        val parsedToml = findPrimitiveTableInAstByName(listOf(tomlFile), tomlTableName)
            ?: throw MissingRequiredPropertyException(
                "Cannot find table with name <$tomlTableName> in the toml input. " +
                        " Are you sure that this table exists in the input?" +
                        " Not able to decode this toml part."
            )

        // adding a fake file node to restore the structure and parse only the part of te toml
        val fakeFileNode = TomlFile()
        parsedToml.children.forEach {
            fakeFileNode.appendChild(it)
        }

        return fakeFileNode
    }

    /**
     * The default instance of [Toml] with the default configuration.
     * See [TomlConfig] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : Toml(
        inputConfig = TomlInputConfig(),
        outputConfig = TomlOutputConfig()
    )
}
