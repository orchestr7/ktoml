package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.SerializersModule

// Todo: Support "flat keys", i.e. a = { b.c = "..." }

/**
 * Encodes a TOML inline table.
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlInlineTableEncoder internal constructor(
    private val rootNode: TomlNode,
    private val parent: TomlAbstractEncoder?,
    elementIndex: Int,
    attributes: Attributes,
    inputConfig: TomlInputConfig,
    outputConfig: TomlOutputConfig,
    serializersModule: SerializersModule
) : TomlAbstractEncoder(
    elementIndex,
    attributes,
    inputConfig,
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
        attributes: Attributes,
        inputConfig: TomlInputConfig,
        outputConfig: TomlOutputConfig,
        serializersModule: SerializersModule
    ) : this(
        rootNode,
        parent = null,
        elementIndex,
        attributes,
        inputConfig,
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
    
    override fun encodeStructure(kind: SerialKind): TomlAbstractEncoder = if (kind == StructureKind.LIST) {
        arrayEncoder(rootNode)
    } else {
        inlineTableEncoder(rootNode)
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
                name,
                inputConfig
            )
            is TomlKeyValueArray -> TomlKeyValueArray(
                TomlKey(name, child.lineNo),
                child.value,
                child.lineNo,
                child.comments,
                child.inlineComment,
                name,
                inputConfig
            )
            is TomlInlineTable -> TomlInlineTable(
                "",
                child.lineNo,
                name,
                child.tomlKeyValues,
                child.comments,
                child.inlineComment,
                inputConfig
            )
            else -> throw InternalEncodingException("Not a pair")
        }
    }
}
