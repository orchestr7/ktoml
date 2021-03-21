package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.error
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.modules.SerializersModule
import okio.ExperimentalFileSystem

@OptIn(ExperimentalSerializationApi::class)
public abstract class TomlInput(
    public val content: String,
    override var serializersModule: SerializersModule,
) : AbstractDecoder() {
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        try {
            return super.decodeSerializableValue(deserializer)
        } catch (e: SerializationException) {
            e.message?.error()
            throw e
        }
    }

    internal companion object {
        internal fun createFor(content: String, serializersModule: SerializersModule): TomlInput {
            val tomlNode = TomlParser().readFile("src/ktomlTest/resources/simple_example.toml")
            return TomlScalarInput(content, serializersModule)
        }
    }
}

private class TomlInputImplementation(content: String, serializersModule: SerializersModule,) : TomlInput(content, serializersModule) {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun decodeString(): String {
        println("Decoding String")
        return super.decodeString()
    }
}
