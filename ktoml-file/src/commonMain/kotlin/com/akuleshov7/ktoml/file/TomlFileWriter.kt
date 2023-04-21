package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.encoders.TomlMainEncoder

import okio.use

import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Writes to a file in the TOML format.
 * @property serializersModule
 */
@Suppress("SINGLE_CONSTRUCTOR_SHOULD_BE_PRIMARY")
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

    public fun <T> encodeToFile(
        serializer: SerializationStrategy<T>,
        value: T,
        tomlFilePath: String
    ) {
        val fileTree = TomlMainEncoder.encode(serializer, value)

        TomlSinkEmitter(
            openFileForWrite(tomlFilePath),
            outputConfig
        ).use {
            tomlWriter.write(fileTree, it)
        }
    }
}
