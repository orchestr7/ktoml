package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.InternalDecodingException
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.UnknownNameDecodingException
import com.akuleshov7.ktoml.parsers.node.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@OptIn(ExperimentalSerializationApi::class)
class TomlDecoder(
    val rootNode: TomlNode,
    val config: SerializationConf,
    var elementsCount: Int = 0
) : AbstractDecoder() {
    private var elementIndex = 0
    val neighbourNodes = rootNode.parent?.children

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeValue(): Any {
        val currentNode = neighbourNodes?.elementAt(elementIndex - 1)

        return when (currentNode) {
            is TomlKeyValue -> currentNode.value.value
            is TomlTable -> currentNode.children
            is TomlFile -> currentNode.children
            else -> throw InternalDecodingException(
                "Internal error (decodeValue stage) -" +
                        " unexpected type of a node: <$currentNode> was found during the decoding process."
            )
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        val currentNode = neighbourNodes?.elementAt(elementIndex)

        val keyField = when (currentNode) {
            is TomlKeyValue -> currentNode.key.content
            is TomlTable -> currentNode.tableName
            is TomlFile -> currentNode.content
            else -> throw InternalDecodingException(
                "Internal error (decodeElementIndex stage) -" +
                        " unexpected type of a node: <$currentNode> was found during the decoding process."
            )
        }

        val fieldWhereValueShouldBeInjected = descriptor.getElementIndex(keyField)
        println("Field name: $keyField, index: $fieldWhereValueShouldBeInjected")

        if (fieldWhereValueShouldBeInjected == CompositeDecoder.UNKNOWN_NAME && !config.ignoreUnknownNames) {
            throw UnknownNameDecodingException(keyField)
        }

        println("Indexes: $elementIndex, ${descriptor.elementsCount}")
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
        println("Came to beginStructure with ${rootNode.content}")
        println("Structure index: $elementIndex")

        return when (rootNode) {
            is TomlFile -> TomlDecoder(rootNode.children.elementAt(0), config, descriptor.elementsCount)
            // need to move on here, but also need to pass children into the function
            is TomlTable -> TomlDecoder(
                rootNode.parent!!.children.elementAt(elementIndex - 1).children.elementAt(0),
                config,
                descriptor.elementsCount
            )
            else -> throw InternalDecodingException(
                "Internal error (beginStructure stage) - unexpected type <${rootNode}> of" +
                        " a node <${rootNode.content}> was found during the decoding process."
            )
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val value = decodeValue().toString()
        val index = enumDescriptor.getElementIndex(value)

        if (index == CompositeDecoder.UNKNOWN_NAME) {
            val choices = (0 until enumDescriptor.elementsCount)
                .map { enumDescriptor.getElementName(it) }
                .sorted()
                .joinToString(", ")

            throw InvalidEnumValueException(value, choices)
        }

        return index
    }

    override fun decodeNotNullMark(): Boolean = decodeString().toLowerCase() != "null"

    companion object {
        fun <T> decode(deserializer: DeserializationStrategy<T>, rootNode: TomlNode, config: SerializationConf): T {
            val decoder = TomlDecoder(rootNode, config)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
