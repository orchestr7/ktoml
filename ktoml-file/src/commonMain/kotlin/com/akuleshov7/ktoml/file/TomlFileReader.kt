package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.source.TomlSourceReader
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.native.concurrent.ThreadLocal

/**
 * TomlFile class can be used for reading files in TOML format
 * that is used to serialize/deserialize TOML file or string
 * @property serializersModule
 */
@OptIn(ExperimentalSerializationApi::class)
public open class TomlFileReader(
    config: TomlConfig = TomlConfig(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : TomlSourceReader(config, serializersModule) {

    /**
     * Simple deserializer of a file that contains toml. Reading file with okio native library
     *
     * @param deserializer deserialization strategy
     * @param tomlFilePath path to the file where toml is stored
     * @return deserialized object of type T
     */
    public fun <T> decodeFromFile(
        deserializer: DeserializationStrategy<T>,
        tomlFilePath: String,
    ): T {
        return decodeFromSource(deserializer, getFileSource(tomlFilePath))
    }

    /**
     * Simple deserializer of a file that contains toml. Reading file with okio native library
     *
     * @param tomlFilePath path to the file where toml is stored
     * @return deserialized object of type T
     */
    public inline fun <reified T> decodeFromFile(
        tomlFilePath: String,
    ): T {
        return decodeFromFile(serializersModule.serializer(), tomlFilePath)
    }

    /**
     * Partial deserializer of a file that contains toml. Reading file with okio native library.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception.
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param tomlFilePath path to the file where toml is stored
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromFile(
        deserializer: DeserializationStrategy<T>,
        tomlFilePath: String,
        tomlTableName: String,
    ): T {
        return partiallyDecodeFromSource(deserializer, getFileSource(tomlFilePath), tomlTableName)
    }

    /**
     * Partial deserializer of a file that contains toml. Reading file with okio native library.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception.
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param tomlFilePath path to the file where toml is stored
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @return deserialized object of type T
     */
    public inline fun <reified T> partiallyDecodeFromFile(
        tomlFilePath: String,
        tomlTableName: String,
    ): T {
        return partiallyDecodeFromFile(serializersModule.serializer(), tomlFilePath, tomlTableName)
    }

    /**
     * The default instance of [TomlFileReader] with the default configuration.
     * See [TomlConfig] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : TomlFileReader(TomlConfig())
}
