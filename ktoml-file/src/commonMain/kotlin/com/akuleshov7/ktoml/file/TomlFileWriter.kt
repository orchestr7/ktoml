package com.akuleshov7.ktoml.file

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.encoders.TomlMainEncoder
import com.akuleshov7.ktoml.parsers.node.TomlFile
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import okio.use

@OptIn(ExperimentalSerializationApi::class)
public open class TomlFileWriter(
    private val config: KtomlConf = KtomlConf(),
    override val serializersModule: SerializersModule = EmptySerializersModule
) : Toml(config, serializersModule) {
    public fun <T> encodeToFile(
        serializer: SerializationStrategy<T>,
        value: T,
        tomlFilePath: String
    )
    {
        val fileTree = TomlFile(config)
        val encoder = TomlMainEncoder(fileTree, config)

        serializer.serialize(encoder, value)

        TomlSinkComposer(
            openFileForWrite(tomlFilePath),
            config
        ).use {
            tomlWriter.write(fileTree, it)
        }
    }
}