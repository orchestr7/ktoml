package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

@OptIn(ExperimentalSerializationApi::class)
public class TomlMainEncoder(
    private val rootNode: TomlNode,
    elementIndex: Int = -1,
    attributes: Attributes = Attributes(),
    inputConfig: TomlInputConfig = TomlInputConfig(),
    outputConfig: TomlOutputConfig = TomlOutputConfig()
) : TomlAbstractEncoder(
    elementIndex,
    attributes,
    inputConfig,
    outputConfig
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
                    inlineComment,
                    name,
                    inputConfig
                )
            } else {
                TomlKeyValuePrimitive(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment,
                    name,
                    inputConfig
                )
            }
        )

        super.appendValue(value)
    }

    override fun <T> encodeStructure(
        kind: SerialKind,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val encoder = when {
            kind == StructureKind.LIST -> {
                TomlArrayEncoder(
                    rootNode,
                    elementIndex,
                    attributes.child(),
                    inputConfig,
                    outputConfig
                )
            }
            attributes.isInline -> {
                TomlInlineTableEncoder(
                    rootNode,
                    elementIndex,
                    attributes.child(),
                    inputConfig,
                    outputConfig
                )
            }
            else -> {
                val table = TomlTablePrimitive(
                    "[${attributes.getFullKey()}]",
                    elementIndex,
                    attributes.comments,
                    attributes.inlineComment,
                    inputConfig
                )

                rootNode.insertTableToTree(table)

                TomlMainEncoder(
                    table,
                    elementIndex,
                    attributes.child(),
                    inputConfig,
                    outputConfig
                )
            }
        }

        serializer.serialize(encoder, value)

        setElementIndex(from = encoder)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        rootNode.children.sortBy { it is TomlTable }

        super.endStructure(descriptor)
    }

    public companion object {
        /**
         * Encodes the specified [value] into a [TomlFile].
         *
         * @param serializer The user-defined or compiler-generated serializer for
         * type [T].
         * @param value The value to serialize.
         * @param inputConfig The input config, used for constructing nodes.
         * @param outputConfig The output config.
         * @return The encoded [TomlFile] node.
         */
        public fun <T> encode(
            serializer: SerializationStrategy<T>,
            value: T,
            inputConfig: TomlInputConfig = TomlInputConfig(),
            outputConfig: TomlOutputConfig = TomlOutputConfig()
        ): TomlFile {
            val root = TomlFile(inputConfig)

            val encoder = TomlMainEncoder(
                root,
                inputConfig = inputConfig,
                outputConfig = outputConfig
            )

            serializer.serialize(encoder, value)

            return root
        }
    }
}
