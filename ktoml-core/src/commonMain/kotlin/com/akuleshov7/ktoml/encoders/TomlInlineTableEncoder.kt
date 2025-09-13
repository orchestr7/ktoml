package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import com.akuleshov7.ktoml.tree.nodes.tables.InlineTableType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

// Todo: Support "flat keys", i.e. a = { b.c = "..." }

/**
 * Encodes a TOML inline table.
 *
 * @param rootNode
 * @param parent
 * @param elementIndex
 * @param attributes
 * @param outputConfig
 * @param serializersModule
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlInlineTableEncoder internal constructor(
    private val rootNode: TomlNode,
    private val parent: TomlAbstractEncoder?,
    elementIndex: Int,
    attributes: TomlEncoderAttributes,
    outputConfig: TomlOutputConfig,
    serializersModule: SerializersModule
) : TomlAbstractEncoder(
    elementIndex,
    attributes,
    outputConfig,
    serializersModule
) {
    private val pairs: MutableList<TomlNode> = mutableListOf()

    /**
     * @param rootNode The root node to add the inline table to.
     * @param elementIndex The current element index.
     * @param attributes The current attributes.
     * @param inputConfig The input config, used for constructing nodes.
     * @param outputConfig The output config.
     */
    public constructor(
        rootNode: TomlNode,
        elementIndex: Int,
        attributes: TomlEncoderAttributes,
        outputConfig: TomlOutputConfig,
        serializersModule: SerializersModule
    ) : this(
        rootNode,
        parent = null,
        elementIndex,
        attributes,
        outputConfig,
        serializersModule
    )
    
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
            )
        } else {
            TomlKeyValuePrimitive(
                key,
                value,
                elementIndex,
                comments = emptyList(),
                inlineComment = ""
            )
        }
        
        super.appendValue(value)
    }
    
    override fun encodeStructure(kind: SerialKind): TomlAbstractEncoder = if (kind == StructureKind.LIST) {
        arrayEncoder(rootNode)
    } else {
        inlineTableEncoder(rootNode)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val (_, _, _, _, _, _, comments, inlineComment) = attributes

        val tomlKey = if (parent !is TomlArrayEncoder) {
            TomlKey(attributes.keyOrThrow(), elementIndex)
        } else {
            null
        }

        val inlineTable = TomlInlineTable(
            key = tomlKey,
            tomlKeyValues = pairs,
            inlineTableType = InlineTableType.PRIMITIVE,
            lineNo = elementIndex,
            comments = comments,
            inlineComment = inlineComment,
        )

        when (parent) {
            is TomlInlineTableEncoder -> parent.pairs += inlineTable
            is TomlArrayEncoder -> rootNode.appendChild(inlineTable)
            else -> rootNode.appendChild(inlineTable)
        }

        return super.beginStructure(descriptor)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (!outputConfig.explicitTables && parent is TomlInlineTableEncoder) {
            pairs.singleOrNull()?.let { pair ->
                parent.collapseLast(pair)
            }
        }

        super.endStructure(descriptor)
    }

    private fun collapseLast(child: TomlNode) {
        val target = pairs.removeLast()
        val name = "${target.name}.${child.name}"

        pairs += when (child) {
            is TomlKeyValuePrimitive -> TomlKeyValuePrimitive(
                TomlKey(name, child.lineNo),
                child.value,
                child.lineNo,
                child.comments,
                child.inlineComment,
            )
            is TomlKeyValueArray -> TomlKeyValueArray(
                TomlKey(name, child.lineNo),
                child.value,
                child.lineNo,
                child.comments,
                child.inlineComment,
            )
            is TomlInlineTable -> TomlInlineTable(
                key = TomlKey(name, elementIndex),
                tomlKeyValues = child.tomlKeyValues,
                inlineTableType = InlineTableType.PRIMITIVE,
                lineNo = child.lineNo,
                comments = child.comments,
                inlineComment = child.inlineComment,
                multiline = child.multiline,
            )
            else -> throw InternalEncodingException("Tried to encode $child as a pair, but it has unknown type.")
        }
    }
}
