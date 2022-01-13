@file:Suppress("FILE_WILDCARD_IMPORTS")

package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.*
import com.akuleshov7.ktoml.parsers.node.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

/**
 * Main entry point into the decoding process. It can create less common decoders inside, for example:
 * TomlListDecoder, TomlPrimitiveDecoder, etc.
 *
 * @property rootNode
 * @property config
 */
@ExperimentalSerializationApi
public class TomlMainDecoder(
    private val rootNode: TomlNode,
    private val config: TomlConfig,
) : TomlAbstractDecoder() {
    private var elementIndex = 0
    override val serializersModule: SerializersModule = EmptySerializersModule

    override fun decodeValue(): Any = decodeKeyValue().value.content

    override fun decodeNotNullMark(): Boolean {
        // we have a special node type in the tree to check nullability (TomlNull). It is one of the implementations of TomlValue
        return when (val node = getCurrentNode()) {
            is TomlKeyValuePrimitive -> node.value !is TomlNull
            is TomlKeyValueArray -> node.value !is TomlNull
            else -> true
        }
    }

    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int {
        val value = decodeValue().toString()
        val index = enumDescriptor.getElementIndex(value)

        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw InvalidEnumValueException(value, enumDescriptor)
        }

        return index
    }

    // the iteration will go through all elements that will be found in the input
    private fun isDecodingDone() = elementIndex == rootNode.getNeighbourNodes().size

    /**
     * Getting the node with the value
     * | rootNode
     * |--- child1, child2, ... , childN
     * ------------elementIndex------->
     */
    private fun getCurrentNode() = rootNode.getNeighbourNodes().elementAt(elementIndex - 1)

    /**
     * Trying to decode the value (ite
     * |--- child1, child2, ... , childN
     * ------------elementIndex------->
     *
     * This method should process only leaf elements that implement TomlKeyValue, because this node should contain the
     * real value for decoding. Other types of nodes are more technical
     *
     */
    override fun decodeKeyValue(): TomlKeyValue = when (val node = getCurrentNode()) {
        is TomlKeyValuePrimitive -> node
        is TomlKeyValueArray -> node
        // empty nodes will be filtered by iterateUntilWillFindAnyKnownName() method, but in case we came into this
        // branch, we should throw an exception as it is not expected at all and we should catch this in tests
        is TomlStubEmptyNode, is TomlTable, is TomlFile ->
            throw InternalDecodingException(
                "This kind of node should not be processed in TomlDecoder.decodeValue(): ${node.content}"
            )
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        // ignoreUnknown is a very important flag that controls if we will fail on unknown key in the input or not
        if (isDecodingDone()) {
            return CompositeDecoder.DECODE_DONE
        }

        // FixMe: error here for missing properties that are not required
        val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex)
        val currentProperty = descriptor.getElementIndex(currentNode.name)
        checkNullability(currentNode, currentProperty, descriptor)

        // in case we have not found the key from the input in the list of property names in the class,
        // we need to throw exception or ignore this unknown property and find any known key to continue processing
        if (currentProperty == CompositeDecoder.UNKNOWN_NAME) {
            // if we have set an option for ignoring unknown names
            // OR in the input we had a technical node for empty tables (see the description to TomlStubEmptyNode)
            // then we need to iterate until we will find something known for us
            if (config.ignoreUnknownNames || currentNode is TomlStubEmptyNode) {
                return iterateUntilWillFindAnyKnownName(descriptor)
            } else {
                throw UnknownNameException(currentNode.name, currentNode.parent?.name)
            }
        }

        // we have found known name and we can continue processing normally
        elementIndex++
        return currentProperty
    }

    private fun iterateUntilWillFindAnyKnownName(descriptor: SerialDescriptor): Int {
        while (true) {
            if (isDecodingDone()) {
                return CompositeDecoder.DECODE_DONE
            }
            val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex)
            val currentProperty = descriptor.getElementIndex(currentNode.name)

            elementIndex++
            if (currentProperty != CompositeDecoder.UNKNOWN_NAME) {
                return currentProperty
            }
        }
    }

    /**
     * straight-forward solution to check if we do not assign null to non-null property
     *
     * @param descriptor - serial descriptor of the current node that we would like to check
     */
    private fun checkNullability(
        currentNode: TomlNode,
        currentProperty: Int,
        descriptor: SerialDescriptor
    ) {
        if (currentNode is TomlKeyValue &&
                currentNode.value is TomlNull &&
                !descriptor.getElementDescriptor(currentProperty).isNullable
        ) {
            throw NullValueException(
                descriptor.getElementName(currentProperty),
                currentNode.lineNo
            )
        }
    }

    /**
     * Actually this method is not needed as serialization lib should do everything for us, but let's
     * fail-fast in the very beginning if the structure is inconsistent and required properties are missing
     */
    private fun checkMissingRequiredProperties(children: MutableSet<TomlNode>?, descriptor: SerialDescriptor) {
        val propertyNames = children?.map {
            it.name
        } ?: emptyList()

        val missingPropertyNames = descriptor.elementNames.toSet() - propertyNames.toSet()

        missingPropertyNames.forEach {
            val index = descriptor.getElementIndex(it)

            if (!descriptor.isElementOptional(index)) {
                throw MissingRequiredPropertyException(
                    "Invalid number of key-value arguments provided in the input for deserialization." +
                            " Missing required property " +
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
            checkMissingRequiredProperties(rootNode.children, descriptor)
            val firstFileChild = rootNode.getFirstChild() ?: throw InternalDecodingException(
                "Missing child nodes (tables, key-values) for TomlFile." +
                        " Was empty toml provided to the input?"
            )
            TomlMainDecoder(firstFileChild, config)
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
                is TomlKeyValueArray -> TomlArrayDecoder(nextProcessingNode, config)
                is TomlKeyValuePrimitive, is TomlStubEmptyNode -> TomlMainDecoder(nextProcessingNode, config)
                is TomlTable -> {
                    val firstTableChild = nextProcessingNode.getFirstChild() ?: throw InternalDecodingException(
                        "Decoding process failed due to invalid structure of parsed AST tree: missing children" +
                                " in a table <${nextProcessingNode.fullTableName}>"
                    )
                    checkMissingRequiredProperties(firstTableChild.getNeighbourNodes(), descriptor)
                    TomlMainDecoder(firstTableChild, config)
                }
                else -> throw InternalDecodingException(
                    "Incorrect decoding state in the beginStructure()" +
                            " with $nextProcessingNode (${nextProcessingNode.content})[${nextProcessingNode.name}]"
                )
            }
        }
    }

    public companion object {
        /**
         * @param deserializer - deserializer provided by Kotlin compiler
         * @param rootNode - root node for decoding (created after parsing)
         * @param config - decoding configuration for parsing and serialization
         * @return decoded (deserialized) object of type T
         */
        public fun <T> decode(
            deserializer: DeserializationStrategy<T>,
            rootNode: TomlNode,
            config: TomlConfig = TomlConfig()
        ): T {
            val decoder = TomlMainDecoder(rootNode, config)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
