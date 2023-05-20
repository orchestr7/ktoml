package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Encodes a TOML file or table.
 *
 * @param elementIndex The current element index.
 * @param attributes The current attributes.
 * @param inputConfig The input config, used for constructing nodes.
 * @param outputConfig The output config.
 * @property rootNode The root node to add elements to.
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlMainEncoder(
    private val rootNode: TomlNode,
    elementIndex: Int = -1,
    attributes: TomlEncoderAttributes = TomlEncoderAttributes(),
    outputConfig: TomlOutputConfig = TomlOutputConfig(),
    serializersModule: SerializersModule = EmptySerializersModule()
) : TomlAbstractEncoder(
    elementIndex,
    attributes,
    outputConfig,
    serializersModule
) {
    override fun appendValue(value: TomlValue) {
        val (_, _, _, _, _, _, comments, inlineComment) = attributes

        val name = attributes.keyOrThrow()
        val key = TomlKey(name, elementIndex)

        rootNode.appendChild(
            if (value is TomlArray) {
                TomlKeyValueArray(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment
                )
            } else {
                TomlKeyValuePrimitive(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment
                )
            }
        )

        super.appendValue(value)
    }

    override fun encodeStructure(kind: SerialKind): TomlAbstractEncoder = when {
        kind == StructureKind.LIST -> TomlArrayEncoder(
            rootNode,
            elementIndex,
            attributes.child(),
            outputConfig,
            serializersModule
        )
        attributes.isInline -> TomlInlineTableEncoder(
            rootNode,
            elementIndex,
            attributes.child(),
            outputConfig,
            serializersModule
        )
        else -> {
            val table = TomlTable(
                TomlKey(attributes.getFullKey(), elementIndex),
                elementIndex,
                type = TableType.PRIMITIVE,
                attributes.comments,
                attributes.inlineComment
            )

            rootNode.appendChild(table)

            tableEncoder(table)
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (rootNode is TomlTable && rootNode.type == TableType.PRIMITIVE && rootNode.hasNoChildren()) {
            rootNode.appendChild(TomlStubEmptyNode(elementIndex))
        }

        // Put table children last to avoid the need for table redeclaration.
        rootNode.children.sortBy { it is TomlTable }

        // Mark primitive tables as synthetic if their only children are nested
        // tables, to avoid extraneous definition.
        // Todo: Find a more elegant solution that doesn't make isSynthetic mutable.
        if (!outputConfig.explicitTables &&
                rootNode is TomlTable &&
                rootNode.type == TableType.PRIMITIVE &&
                rootNode.children.all { it is TomlTable }) {
            rootNode.isSynthetic = true
        }

        super.endStructure(descriptor)
    }

    public companion object {
        /**
         * Encodes the specified [value] into a [TomlFile].
         *
         * @param serializer The user-defined or compiler-generated serializer for
         * type [T].
         * @param value The value to serialize.
         * @param outputConfig The output config.
         * @param serializersModule
         * @return The encoded [TomlFile] node.
         */
        public fun <T> encode(
            serializer: SerializationStrategy<T>,
            value: T,
            outputConfig: TomlOutputConfig = TomlOutputConfig(),
            serializersModule: SerializersModule = EmptySerializersModule()
        ): TomlFile {
            val root = TomlFile()

            val encoder = TomlMainEncoder(
                root,
                outputConfig = outputConfig,
                serializersModule = serializersModule
            )

            serializer.serialize(encoder, value)

            return root
        }
    }
}
