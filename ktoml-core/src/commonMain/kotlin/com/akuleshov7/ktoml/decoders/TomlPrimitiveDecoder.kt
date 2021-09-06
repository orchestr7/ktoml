package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.parsers.node.TomlKeyValue
import com.akuleshov7.ktoml.parsers.node.TomlKeyValuePrimitive
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * @property rootNode
 */
@ExperimentalSerializationApi
public class TomlPrimitiveDecoder(
    private val rootNode: TomlKeyValuePrimitive,
) : TomlAbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule
    override fun decodeValue(): Any = rootNode.value.content
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
    override fun decodeKeyValue(): TomlKeyValue = rootNode
}
