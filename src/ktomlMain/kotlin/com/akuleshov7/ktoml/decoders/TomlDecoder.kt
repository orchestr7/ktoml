package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.parsers.node.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@OptIn(ExperimentalSerializationApi::class)
class TomlDecoder(val rootNode: TomlNode, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    init {
        println("creating for ${rootNode.content}")
    }

    override fun decodeValue(): Any {
        return when(rootNode) {
            is TomlKeyValue -> rootNode.value.value
            is TomlTable -> rootNode.children
            is TomlFile -> rootNode.children
            else -> throw Exception("")
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        val neighbourNodes = rootNode.parent?.children
        val currentNode = neighbourNodes?.elementAt(elementIndex)

        val keyField = when (currentNode) {
            is TomlKeyValue -> currentNode.key.content
            is TomlTable -> currentNode.tableName
            is TomlFile -> currentNode.content
            else -> throw Exception("")
        }

        val fieldWhereValueShouldBeInjected = descriptor.getElementIndex(keyField)
        println("Field name: $keyField, index: $fieldWhereValueShouldBeInjected")

        if (fieldWhereValueShouldBeInjected == CompositeDecoder.UNKNOWN_NAME) {
            // FixMe: throw exception or handle this case
        }
        elementIndex++
        return fieldWhereValueShouldBeInjected
    }


    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return decodeInt().also {
            elementsCount = it
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        println("====== new structure with size ${descriptor.elementsCount}~~~~")
        rootNode.prettyPrint()
        return when(rootNode) {
            is TomlFile -> TomlDecoder(rootNode.children.elementAt(0), descriptor.elementsCount)
            is TomlTable -> TomlDecoder(rootNode.children.elementAt(elementIndex - 1), descriptor.elementsCount)
            else -> throw Exception("")
        }
    }

    override fun decodeNotNullMark(): Boolean = decodeString().toLowerCase() != "null"

    companion object {
        fun <T> decode(deserializer: DeserializationStrategy<T>, rootNode: TomlNode): T {
            val decoder = TomlDecoder(rootNode)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}



