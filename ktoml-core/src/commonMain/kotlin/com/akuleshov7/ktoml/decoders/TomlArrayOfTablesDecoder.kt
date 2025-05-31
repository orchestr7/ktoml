package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.tree.nodes.TomlArrayOfTablesElement
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

/**
 * @param rootNode
 * @param config
 * @property serializersModule
 */
@ExperimentalSerializationApi
@Suppress("UNCHECKED_CAST")
public class TomlArrayOfTablesDecoder(
    private val rootNode: TomlTable,
    private val config: TomlInputConfig,
    override val serializersModule: SerializersModule,
) : TomlAbstractDecoder() {
    private var nextElementIndex = 0
    private val list = rootNode.children as List<TomlArrayOfTablesElement>
    private lateinit var currentElementDecoder: TomlMainDecoder

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (nextElementIndex == list.size) {
            return CompositeDecoder.DECODE_DONE
        }

        val rootNode: TomlNode = TomlFile()
        rootNode.children.addAll(list[nextElementIndex].children)
        currentElementDecoder = TomlMainDecoder(
            rootNode = rootNode,
            config = config,
            serializersModule = serializersModule,
        )

        return nextElementIndex++
    }

    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        currentElementDecoder.decodeSerializableValue(deserializer)

    // decodeKeyValue is usually used for simple plain structures, but as it is present in TomlAbstractDecoder,
    // we should implement it and have this stub
    override fun decodeKeyValue(): TomlKeyValue {
        throw NotImplementedError("Method `decodeKeyValue`" +
                " should never be called for TomlArrayOfObjectsDecoder, because it is a more complex structure")
    }
}
