package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlStubEmptyNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Sometimes, when you do not know the names of the TOML keys and cannot create a proper class with field names for parsing,
 * it can be useful to read and parse TOML tables to a map. This is exactly what this TomlMapDecoder is used for.
 *
 * @param rootNode toml table that we are trying to decode
 * @param decodingElementIndex for iterating over the TOML table we are currently reading
 * @param kotlinxIndex for iteration inside the kotlinX loop: [decodeElementIndex -> decodeSerializableElement]
 */
@ExperimentalSerializationApi
public class TomlMapDecoder(
    private val rootNode: TomlTable,
    private val config: TomlInputConfig,
    private var decodingElementIndex: Int = 0,
    private var kotlinxIndex: Int = 0,
) : TomlAbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // we will iterate in the following way:
        // for [map]
        // a = 1
        // b = 2
        // kotlinxIndex will be (0, 1), (2 ,3)
        // and decodingElementIndex will be 0, 1 (as there are only two elements in the table: 'a' and 'b')
        if (rootNode.children[decodingElementIndex] is TomlStubEmptyNode) {
            skipStubs()
        } else {
            decodingElementIndex = kotlinxIndex / 2
        }

        if (decodingElementIndex == rootNode.children.size) {
            return CompositeDecoder.DECODE_DONE
        }

        return kotlinxIndex++
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T {
        skipStubs()

        return when (val processedNode = rootNode.children[decodingElementIndex]) {
            // simple decoding for key-value type
            is TomlKeyValue -> ((if (index % 2 == 0) processedNode.key.toString() else processedNode.value.content)) as T
            is TomlTable -> {
                if (index % 2 == 0) processedNode.name as T else {
                    val newDecoder = TomlMapDecoder(processedNode, config)
                    newDecoder.decodeSerializableValue(deserializer)
                }
            }
            else -> {
                throw Exception()
            }
        }
    }

    override fun decodeKeyValue(): TomlKeyValue {
        TODO("No need to implement decodeKeyValue for TomlMapDecoder as it is not needed for such primitive decoders")
    }

    private fun skipStubs() {
        if (rootNode.children[decodingElementIndex] is TomlStubEmptyNode) {
            ++decodingElementIndex
        }
    }
}
