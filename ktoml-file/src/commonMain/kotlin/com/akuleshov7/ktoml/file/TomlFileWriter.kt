package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.Toml
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
public open class TomlFileWriter(
    private val config: KtomlConf = KtomlConf(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : Toml(config, serializersModule) {
    public fun <T> encodeToFile(
        serializer: SerializationStrategy<T>,
        value: T,
        tomlFilePath: String
    ) {
        val fileTree = TomlFile(config)

        // Todo: Write an encoder implementation.

        TomlSinkEmitter(
            openFileForWrite(tomlFilePath),
            config
        ).use {
            tomlWriter.write(fileTree, it)
        }
    }
}
