package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.TomlMainDecoder
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.TomlFile
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
 * @property config - configuration for the serialization
 * @property serializersModule - default overridden
 */
@OptIn(ExperimentalSerializationApi::class)
public open class Toml(
    private val config: TomlConfig = TomlConfig(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : StringFormat {
    // parser and writer are created once after the creation of the class, to reduce
    // the number of created parsers and writers for each toml
    public val tomlParser: TomlParser = TomlParser(config)
    public val tomlWriter: TomlWriter = TomlWriter(config)

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
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: List<String>,
        config: TomlConfig
    ): T {
        val parsedToml = tomlParser.parseStringsToTomlTree(toml, config)
        return TomlMainDecoder.decode(deserializer, parsedToml, this.config)
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
        config: TomlConfig = TomlConfig()
    ): T {
        val fakeFileNode = TomlFile()
        return TomlMainDecoder.decode(deserializer, fakeFileNode, this.config)
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
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: List<String>,
        tomlTableName: String,
        config: TomlConfig = TomlConfig()
    ): T {
        val fakeFileNode = TomlFile()
        return TomlMainDecoder.decode(deserializer, fakeFileNode, this.config)
    }

    // ================== other ===============

    /**
     * The default instance of [Toml] with the default configuration.
     * See [TomlConfig] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : Toml(TomlConfig())
}
