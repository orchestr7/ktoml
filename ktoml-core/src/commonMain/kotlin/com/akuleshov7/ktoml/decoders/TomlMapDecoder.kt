package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.InternalDecodingException
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
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
 * PLEASE NOTE, THAT IT IS EXTREMELY UNSAFE TO USE THIS DECODER, AS IT WILL DECODE EVERYTHING, WITHOUT ANY TYPE-CHECKING
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
        // stubs are internal technical nodes that are not needed in this scenario
        if (rootNode.children[decodingElementIndex] is TomlStubEmptyNode) {
            skipStubs()
        } else {
            // we will iterate in the following way:
            // for [map]
            // a = 1
            // b = 2
            // kotlinxIndex will be (0, 1), (2 ,3)
            // and decodingElementIndex will be 0, 1 (as there are only two elements in the table: 'a' and 'b')
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
        // stubs are internal technical nodes that are not needed in this scenario
        skipStubs()
        return when (val processedNode = rootNode.children[decodingElementIndex]) {
            // simple decoding for key-value type
            is TomlKeyValue -> ((if (index % 2 == 0) processedNode.key.toString() else processedNode.value.content)) as T
            is TomlTable -> {
                if (index % 2 == 0) {
                    processedNode.name as T
                } else {
                    TomlMapDecoder(processedNode, config).decodeSerializableValue(deserializer)
                }
            }
            else -> {
                throw InternalDecodingException("Trying to decode ${processedNode.prettyStr()} with TomlMapDecoder, " +
                        "but faced an unknown type of Node")
            }
        }
    }

    override fun decodeKeyValue(): TomlKeyValue {
        throw IllegalTypeException("""
            You are trying to decode a nested Table ${rootNode.fullTableKey} with a <Map> type to some primitive type.
            For example: 
            [a]
              [a.b]
                  a = 2
                  
            should be decoded to Map<String, Map<String, Long>>, but not to Map<String, Long>
            """
            , rootNode.lineNo)
    }

    /**
     * TomlStubs are internal technical nodes that should be skipped during the decoding process.
     * And so we need to skip them with our iteration indices:
     * decodingElementIndex had step equals to 1
     * kotlinxIndex has step equals to 2 (because in kotlinx.serialization Maps have x2 index: one for key and one for value)
     */
    private fun skipStubs() {
        if (rootNode.children[decodingElementIndex] is TomlStubEmptyNode) {
            ++decodingElementIndex
            kotlinxIndex += 2
        }
    }
}
