package com.akuleshov7.decoders

import com.akuleshov7.error
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
public abstract class TomlInput(
    public val content: String,
    override var serializersModule: SerializersModule,
) : AbstractDecoder() {
    internal companion object {
        internal fun createFor(content: String, serializersModule: SerializersModule): TomlInput =
            TomlScalarInput(content, serializersModule)
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
        try {
            return super.decodeSerializableValue(deserializer)
        } catch (e: SerializationException) {
            e.message?.error()
            throw e
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class TomlInputNull(content: String,serializersModule: SerializersModule,) : TomlInput(content, serializersModule) {
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0

    override fun decodeString(): String {
        println("Decoding String")
        return super.decodeString()
    }
}
