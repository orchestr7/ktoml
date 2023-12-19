package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * @param rootNode
 */
@ExperimentalSerializationApi
public class TomlPrimitiveDecoder(
    private val rootNode: TomlKeyValuePrimitive,
) : TomlAbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
    override fun decodeValue(): Any = rootNode.value.content
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 0
    override fun decodeKeyValue(): TomlKeyValue = rootNode
}
