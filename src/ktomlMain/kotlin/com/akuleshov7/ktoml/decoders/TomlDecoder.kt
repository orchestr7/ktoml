package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.InternalDecodingException
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import com.akuleshov7.ktoml.exceptions.MissingRequiredFieldException
import com.akuleshov7.ktoml.exceptions.UnknownNameDecodingException
import com.akuleshov7.ktoml.parsers.node.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@OptIn(ExperimentalSerializationApi::class)
class TomlDecoder(
    val rootNode: TomlNode,
    val config: DecoderConf,
    var elementsCount: Int = 0
) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeValue(): Any {
        val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex - 1)

        return when (currentNode) {
            is TomlKeyValue -> currentNode.value.value
            is TomlTable, is TomlFile -> currentNode.children
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // the iteration will go through the all elements that will be found in the input
        if (elementIndex == rootNode.getNeighbourNodes().size) return CompositeDecoder.DECODE_DONE

        // FixMe: error here for missing fields that are not required
        val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex)

        val fieldWhereValueShouldBeInjected = descriptor.getElementIndex(currentNode.name)

        // in case we have not found the key from the input in the list of field names in the class,
        // we need to throw exception or ignore this unknown field
        if (fieldWhereValueShouldBeInjected == CompositeDecoder.UNKNOWN_NAME) {
            if (!config.ignoreUnknownNames) {
                throw UnknownNameDecodingException(currentNode.name)
            } else {
                // FixMe: unknown names are not ignored now, need to fix it
            }
        }

        elementIndex++
        return fieldWhereValueShouldBeInjected
    }

    fun checkMissingRequiredField(children: MutableSet<TomlNode>?, descriptor: SerialDescriptor) {
        val new = children?.map {
            it.name
        } ?: emptyList()

        val missingKeysInInput = descriptor.elementNames.toSet() - new.toSet()
        missingKeysInInput.forEach {
            val index = descriptor.getElementIndex(it)
            if (!descriptor.isElementOptional(index)) {
                throw MissingRequiredFieldException(
                    "Invalid number of arguments provided for deserialization. Missing required field " +
                            "<${descriptor.getElementName(index)}> in the input"
                )
            }
        }
    }

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int {
        return decodeInt().also {
            elementsCount = it
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = when (rootNode) {
        is TomlFile -> {
            TomlDecoder(rootNode.getFirstChild(), config, descriptor.elementsCount)
        }
        // need to move on here, but also need to pass children into the function
        is TomlTable, is TomlKeyValue -> {
            // this is a little bit tricky index calculation, suggest not to change
            // we are using the previous node to get all neighbour nodes:
            //                          (parentNode)
            // neighbourNodes: (current rootNode) (next node which we would like to use)
            val nextProcessingNode = rootNode
                .getNeighbourNodes()
                .elementAt(elementIndex - 1)
                .getFirstChild()
            checkMissingRequiredField(nextProcessingNode.getNeighbourNodes(), descriptor)

            TomlDecoder(
                // this is a little bit tricky index calculation, suggest not to change
                nextProcessingNode,
                config,
                descriptor.elementsCount
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
        fun <T> decode(deserializer: DeserializationStrategy<T>, rootNode: TomlNode, config: DecoderConf): T {
            val decoder = TomlDecoder(rootNode, config)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
