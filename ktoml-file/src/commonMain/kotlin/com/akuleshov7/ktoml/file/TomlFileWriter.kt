package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.TomlFile

import okio.use

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Writes to a file in the TOML format.
 * @property serializersModule
 */
@OptIn(ExperimentalSerializationApi::class)
public open class TomlFileWriter : Toml {
    public constructor(
        inputConfig: TomlInputConfig = TomlInputConfig(),
        outputConfig: TomlOutputConfig = TomlOutputConfig(),
        serializersModule: SerializersModule = EmptySerializersModule(),
    ) : super(
        inputConfig,
        outputConfig,
        serializersModule
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlOutputConfig instead. Will be removed in next releases."
    )
    public constructor(
        config: TomlConfig = TomlConfig(),
        serializersModule: SerializersModule = EmptySerializersModule(),
    ) : super(
        config,
        serializersModule
    )

    public fun <T> encodeToFile(
        serializer: SerializationStrategy<T>,
        value: T,
        tomlFilePath: String
    ) {
        val fileTree = TomlFile(inputConfig)

        // Todo: Write an encoder implementation.

        TomlSinkEmitter(
            openFileForWrite(tomlFilePath),
            outputConfig
        ).use {
            tomlWriter.write(fileTree, it)
        }
    }
}
