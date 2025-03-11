@file:Suppress("FILE_WILDCARD_IMPORTS")

package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.*
import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlNull

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Main entry point into the decoding process. It can create less common decoders inside, for example:
 * TomlListDecoder, TomlPrimitiveDecoder, etc.
 *
 * @param rootNode
 * @param config
 * @param elementIndex
 */
@ExperimentalSerializationApi
public class TomlMainDecoder(
    private var rootNode: TomlNode,
    private val config: TomlInputConfig,
    private var elementIndex: Int = 0
) : TomlAbstractDecoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        rootNode: TomlNode,
        config: TomlConfig,
        elementIndex: Int = 0
    ) : this(
        rootNode,
        config.input,
        elementIndex
    )

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
        val node = decodeKeyValue()
        val value = node.value.content.toString()
        val index = enumDescriptor.getElementIndex(value)

        if (index == CompositeDecoder.UNKNOWN_NAME) {
            throw InvalidEnumValueException(value, enumDescriptor, node.lineNo)
        }

        return index
    }

    // the iteration will go through all elements that will be found in the input
    private fun isDecodingDone() =
        if (rootNode is TomlFile) true else elementIndex == rootNode.getNeighbourNodes().size

    /**
     * Getting the node with the value
     * | rootNode
     * |--- child1, child2, ... , childN
     * ------------elementIndex------->
     */
    private fun getCurrentNode() = rootNode.getNeighbourNodes().elementAt(elementIndex - 1)

    /**
     * Trying to decode the value using elementIndex
     * |--- child1, child2, ... , childN
     * ------------elementIndex------->
     *
     * This method should process only leaf elements that implement TomlKeyValue, because this node should contain the
     * real value for decoding. Other types of nodes are more technical
     *
     */
    override fun decodeKeyValue(): TomlKeyValue {
        // this is a very important workaround for people who plan to write their own CUSTOM serializers
        if (rootNode is TomlFile) {
            rootNode = getFirstChild(rootNode)
            elementIndex = 1
        }

        return when (val node = getCurrentNode()) {
            is TomlKeyValuePrimitive -> node
            is TomlKeyValueArray -> node
            // empty nodes will be filtered by iterateUntilWillFindAnyKnownName() method, but in case we came into this
            // branch, we should throw an exception as it is not expected at all, and we should catch this in tests
            else ->
                throw InternalDecodingException(
                    "Node of type [${node::class}] should not be processed in TomlDecoder.decodeValue(): <$node>."
                )
        }
    }

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (isDecodingDone()) {
            return CompositeDecoder.DECODE_DONE
        }

        val currentNode = rootNode.getNeighbourNodes().elementAt(elementIndex)
        val currentProperty = descriptor.getElementIndex(currentNode.name)
        checkNullability(currentNode, currentProperty, descriptor)

        // in case we have not found the key from the input in the list of property names in the class,
        // we need to throw exception or ignore this unknown property and find any known key to continue processing
        if (currentProperty == CompositeDecoder.UNKNOWN_NAME) {
            // ignoreUnknown is a very important flag that controls if we will fail on unknown key in the input or not

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
     * fail-fast in the very beginning if the structure is inconsistent and required properties are missing.
     * Also we will throw much more clear ktoml-like exception MissingRequiredPropertyException
     */
    private fun checkMissingRequiredProperties(children: MutableList<TomlNode>?, descriptor: SerialDescriptor) {
        // the only case when we are not able to check required properties is when our descriptor type is a Map with unnamed properties:
        // in this case we will just ignore this check and will put all values that we have in the table to the map
        if (descriptor.kind == StructureKind.MAP) {
            return
        }

        val propertyNames = children?.map {
            it.name
        } ?: emptyList()

        val missingPropertyNames = descriptor.elementNames.toSet() - propertyNames.toSet()

        missingPropertyNames.forEach {
            val index = descriptor.getElementIndex(it)

            if (!descriptor.isElementOptional(index) || config.ignoreDefaultValues) {
                throw MissingRequiredPropertyException(
                    "Invalid number of key-value arguments provided in the input for deserialization. Missing required property " +
                            "<${descriptor.getElementName(index)}> from class <${descriptor.serialName}> in the input. " +
                            "(In your deserialization class you have declared this field, but it is missing in the input)"
                )
            }
        }
    }

    /**
     * A hack that comes from a compiler plugin to process Inline (value) classes
     */
    override fun decodeInline(descriptor: SerialDescriptor): Decoder =
        iterateOverTomlStructure(descriptor, true)

    /**
     * this method does all the iteration logic for processing code structures and collections
     * treat it as an !entry point! and the orchestrator of the decoding
     */
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        iterateOverTomlStructure(descriptor, false)

    /**
     * Entry Point into the logic, core logic of the structure traversal and linking the data from TOML AST
     * to the descriptor and vica-versa. Basically this logic is used to iterate through data structures and do processing.
     */
    private fun iterateOverTomlStructure(descriptor: SerialDescriptor, inlineFunc: Boolean): TomlAbstractDecoder =
        if (rootNode is TomlFile) {
            checkMissingRequiredProperties(rootNode.children, descriptor)
            val firstFileChild = getFirstChild(rootNode)

            // inline structures has a very specific logic for decoding. Kotlinx.serialization plugin generates specific code:
            // 'decoder.decodeInline(this.getDescriptor()).decodeLong())'. So we need simply to increment
            // our element index by 1 (0 is the default value), because value/inline classes are always a wrapper over some SINGLE value.
            if (inlineFunc) TomlMainDecoder(firstFileChild, config, 1) else TomlMainDecoder(firstFileChild, config, 0)
        } else {
            // this is a tricky index calculation, suggest not to change. We are using the previous node to get all neighbour nodes:
            // | (parentNode)
            // |--- neighbourNodes: (current rootNode) (next node which we would like to process now)
            val nextProcessingNode = rootNode
                .getNeighbourNodes()
                .elementAt(elementIndex - 1)

            when (nextProcessingNode) {
                is TomlKeyValueArray -> TomlArrayDecoder(nextProcessingNode, config)
                is TomlKeyValuePrimitive, is TomlStubEmptyNode -> TomlMainDecoder(nextProcessingNode, config)
                is TomlTable -> when (descriptor.kind) {
                    // This logic is a special case when user would like to parse key-values from a table to a map.
                    // It can be useful, when the user does not know key names of TOML key-value pairs, for example:
                    // if parsing
                    StructureKind.MAP -> TomlMapDecoder(nextProcessingNode, config)

                    else -> {
                        val firstTableChild = nextProcessingNode.getFirstChild() ?: throw InternalDecodingException(
                            "Decoding process has failed due to invalid structure of parsed AST tree: missing children" +
                                    " in a table <${nextProcessingNode.fullTableKey}>"
                        )

                        checkMissingRequiredProperties(firstTableChild.getNeighbourNodes(), descriptor)
                        TomlMainDecoder(firstTableChild, config)
                    }
                }

                else -> throw InternalDecodingException(
                    "Incorrect decoding state in the beginStructure()" +
                            " with $nextProcessingNode ($nextProcessingNode)[${nextProcessingNode.name}]"
                )
            }
        }

    private fun getFirstChild(node: TomlNode) =
        node.getFirstChild() ?: if (!config.allowEmptyToml) {
            throw InternalDecodingException(
                "Missing child nodes (tables, key-values) for TomlFile." +
                        "May be an empty toml was provided to the input?"
            )
        } else {
            node
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
            config: TomlInputConfig = TomlInputConfig()
        ): T {
            val decoder = TomlMainDecoder(rootNode, config)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
