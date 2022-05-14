
package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.native.concurrent.ThreadLocal

/**
 * TomlFile class can be used for reading files in TOML format
 * that is used to serialize/deserialize TOML file or string
 * @property serializersModule
 */
@OptIn(ExperimentalSerializationApi::class)
public open class TomlFileReader : Toml {
    public constructor(
        inputConfig: TomlInputConfig = TomlInputConfig(),
        outputConfig: TomlOutputConfig = TomlOutputConfig(),
        serializersModule: SerializersModule = EmptySerializersModule
    ) : super(
        inputConfig,
        outputConfig,
        serializersModule
    )

    @Deprecated(
        message = "config parameter split into inputConfig and outputConfig."
    )
    public constructor(
        config: TomlConfig,
        serializersModule: SerializersModule = EmptySerializersModule
    ) : super(
        config,
        serializersModule
    )

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
        val parsedToml = readAndParseFile(tomlFilePath)
        return decodeFromString(deserializer, parsedToml, inputConfig)
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
        val parsedToml = readAndParseFile(tomlFilePath)
        return partiallyDecodeFromString(deserializer, parsedToml, tomlTableName, inputConfig)
    }

    /**
     * The default instance of [TomlFileReader] with the default configuration.
     * See [TomlConfig] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : TomlFileReader(
        inputConfig = TomlInputConfig(),
        outputConfig = TomlOutputConfig()
    )
}
