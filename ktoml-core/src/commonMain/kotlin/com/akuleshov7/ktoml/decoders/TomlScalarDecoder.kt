package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.parsers.node.TomlValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
public class TomlScalarDecoder(
    val rootNode: TomlValue
) : AbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule
    override fun decodeValue() = rootNode.content
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
}
