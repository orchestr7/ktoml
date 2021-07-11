@file:Suppress("FILE_WILDCARD_IMPORTS")

package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.exceptions.*
import com.akuleshov7.ktoml.parsers.node.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

/**
 * @property rootNode
 * @property config
 */
@ExperimentalSerializationApi
public class TomlDecoder(
    val rootNode: TomlNode,
    val config: KtomlConf,
) : AbstractDecoder() {
    private var elementIndex = 0
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeValue(): Any {
        val node = rootNode.getNeighbourNodes().elementAt(elementIndex - 1)
        return when (node) {
            is TomlKeyValueSimple -> node.value.content
            is TomlTable, is TomlFile -> node.children
            // empty nodes will be filtered by iterateUntilWillFindAnyKnownName() method, but in case we came into this
            // branch, we should throw an exception as it is not expected at all
            is TomlStubEmptyNode, is TomlKeyValueList ->
                throw InternalDecodingException("Empty node or KeyValueList should not be processed in TomlDecoder")
        }
    }

    // the iteration will go through all elements that will be found in the input
    private fun isDecodingDone() = elementIndex == rootNode.getNeighbourNodes().size

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // ignoreUnknown is a very important flag that controls if we will fail on unknown key in the input or not
        if (isDecodingDone()) {
            return CompositeDecoder.DECODE_DONE
        }

        // FixMe: error here for missing fields that are not required
        val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex)
        val fieldWhereValueShouldBeInjected = descriptor.getElementIndex(currentNode.name)
        checkNullability(currentNode, fieldWhereValueShouldBeInjected, descriptor)

        // in case we have not found the key from the input in the list of field names in the class,
        // we need to throw exception or ignore this unknown field and find any known key to continue processing
        if (fieldWhereValueShouldBeInjected == CompositeDecoder.UNKNOWN_NAME) {
            // if we have set an option for ignoring unknown names
            // OR in the input we had a technical node for empty tables (see the description to TomlStubEmptyNode)
            // then we need to iterate until we will find something known for us
            if (config.ignoreUnknownNames || currentNode is TomlStubEmptyNode) {
                return iterateUntilWillFindAnyKnownName(descriptor)
            } else {
                throw UnknownNameDecodingException(currentNode.name, currentNode.parent?.name)
            }
        }

        // we have found known name and we can continue processing normally
        elementIndex++
        return fieldWhereValueShouldBeInjected
    }

    private fun iterateUntilWillFindAnyKnownName(descriptor: SerialDescriptor): Int {
        while (true) {
            if (isDecodingDone()) {
                return CompositeDecoder.DECODE_DONE
            }
            val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex)
            val fieldWhereValueShouldBeInjected = descriptor.getElementIndex(currentNode.name)

            elementIndex++
            if (fieldWhereValueShouldBeInjected != CompositeDecoder.UNKNOWN_NAME) {
                return fieldWhereValueShouldBeInjected
            }
        }
    }

    /**
     * straight-forward solution to check if we do not assign null to non-null fields
     *
     * @param descriptor - serial descriptor of the current node that we would like to check
     */
    private fun checkNullability(
        currentNode: TomlNode,
        fieldWhereValueShouldBeInjected: Int,
        descriptor: SerialDescriptor
    ) {
        if (currentNode is TomlKeyValue &&
                currentNode.value is TomlNull &&
                !descriptor.getElementDescriptor(fieldWhereValueShouldBeInjected).isNullable
        ) {
            throw NonNullableValueException(
                descriptor.getElementName(fieldWhereValueShouldBeInjected),
                currentNode.lineNo
            )
        }
    }

    /**
     * actually this method is not needed as serialization lib should do everything for us, but let's
     * fail-fast in the very beginning if the structure is inconsistent and required fields are missing
     */
    private fun checkMissingRequiredField(children: MutableSet<TomlNode>?, descriptor: SerialDescriptor) {
        val fieldNameProvidedInTheInput = children?.map {
            it.name
        } ?: emptyList()

        val missingKeysInInput = descriptor.elementNames.toSet() - fieldNameProvidedInTheInput.toSet()

        missingKeysInInput.forEach {
            val index = descriptor.getElementIndex(it)

            if (!descriptor.isElementOptional(index)) {
                throw MissingRequiredFieldException(
                    "Invalid number of arguments provided for deserialization. Missing required field " +
                            "<${descriptor.getElementName(index)}> from class <${descriptor.serialName}> in the input"
                )
            }
        }
    }

    /**
     * this method does all the iteration logic for processing code structures and collections
     * treat it as an !entry point! and the orchestrator of the decoding
     */
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = when (rootNode) {
        is TomlFile -> {
            checkMissingRequiredField(rootNode.children, descriptor)
            val firstFileChild = rootNode.getFirstChild() ?: throw InternalDecodingException(
                "Missing child nodes (tales, key-values) for TomlFile." +
                        " Empty toml was provided to the input?"
            )
            TomlDecoder(firstFileChild, config)
        }
        else -> {
            // this is a little bit tricky index calculation, suggest not to change
            // we are using the previous node to get all neighbour nodes:
            // | (parentNode)
            // |--- neighbourNodes: (current rootNode) (next node which we would like to process now)
            val nextProcessingNode = rootNode
                .getNeighbourNodes()
                .elementAt(elementIndex - 1)

            when (nextProcessingNode) {
                is TomlKeyValueList -> TomlListDecoder(nextProcessingNode, config)
                is TomlKeyValueSimple, is TomlStubEmptyNode -> TomlDecoder(nextProcessingNode, config)
                is TomlTable -> {
                    val firstTableChild = nextProcessingNode.getFirstChild() ?: throw InternalDecodingException(
                        "Decoding process failed due to invalid structure of parsed AST tree: missing children" +
                                " in a table <${nextProcessingNode.fullTableName}>"
                    )
                    checkMissingRequiredField(firstTableChild.getNeighbourNodes(), descriptor)
                    TomlDecoder(firstTableChild, config)
                }
                else -> throw InternalDecodingException(
                    "Incorrect decdong state in the beginStructure()" +
                            " with $nextProcessingNode (${nextProcessingNode.content})[${nextProcessingNode.name}]"
                )
            }
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

    override fun decodeNotNullMark(): Boolean = decodeValue().toString().toLowerCase() != "null"

    companion object {
        /**
         * @param deserializer - deserializer provided by Kotlin compiler
         * @param rootNode - root node for decoding (created after parsing)
         * @param config - decoding configuration for parsing and serialization
         * @return decoded (deserialized) object of type T
         */
        fun <T> decode(
            deserializer: DeserializationStrategy<T>,
            rootNode: TomlNode,
            config: KtomlConf = KtomlConf()
        ): T {
            val decoder = TomlDecoder(rootNode, config)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
