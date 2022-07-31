package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder

// Todo: Support "flat keys", i.e. a = { b.c = "..." }

/**
 * @property parentNodes The node list of a nested inline table's parent.
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlInlineTableEncoder internal constructor(
    private val rootNode: TomlNode,
    private val parent: TomlAbstractEncoder?,
    elementIndex: Int,
    attributes: Attributes,
    inputConfig: TomlInputConfig,
    outputConfig: TomlOutputConfig
) : TomlAbstractEncoder(
    elementIndex,
    attributes,
    inputConfig,
    outputConfig
) {
    public constructor(
        rootNode: TomlNode,
        elementIndex: Int,
        attributes: Attributes,
        inputConfig: TomlInputConfig,
        outputConfig: TomlOutputConfig
    ) : this(
        rootNode,
        parent = null,
        elementIndex,
        attributes,
        inputConfig,
        outputConfig
    )
    
    private val pairs = mutableListOf<TomlNode>()
    
    // Inline tables are single-line, don't increment.
    override fun nextElementIndex(): Int = elementIndex

    override fun appendValue(value: TomlValue) {
        val name = attributes.keyOrThrow()
        val key = TomlKey(name, elementIndex)

        pairs += if (value is TomlArray) {
            TomlKeyValueArray(
                key,
                value,
                elementIndex,
                comments = emptyList(),
                inlineComment = "",
                name,
                inputConfig
            )
        } else {
            TomlKeyValuePrimitive(
                key,
                value,
                elementIndex,
                comments = emptyList(),
                inlineComment = "",
                name,
                inputConfig
            )
        }
        
        super.appendValue(value)
    }
    
    override fun <T> encodeStructure(
        kind: SerialKind,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val encoder = if (kind == StructureKind.LIST) {
            TomlArrayEncoder(
                rootNode,
                parent = this,
                elementIndex,
                attributes.child(),
                inputConfig,
                outputConfig
            )
        } else {
            TomlInlineTableEncoder(
                rootNode,
                parent = this,
                elementIndex,
                attributes.child(),
                inputConfig,
                outputConfig
            )
        }

        serializer.serialize(encoder, value)

        setElementIndex(from = encoder)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val (_, _, _, _, _, _, comments, inlineComment) = attributes
        val name = attributes.keyOrThrow()

        val inlineTable = TomlInlineTable(
            "",
            elementIndex,
            name,
            pairs,
            comments,
            inlineComment,
            inputConfig
        )

        when (parent) {
            is TomlInlineTableEncoder -> parent.pairs += inlineTable
            is TomlArrayEncoder -> {
                // Todo: Implement this when inline table arrays are supported.
            }
            else -> rootNode.appendChild(inlineTable)
        }

        return super.beginStructure(descriptor)
    }
}
