package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.modules.SerializersModule

/**
 * Encodes a TOML array or table array.
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlArrayEncoder internal constructor(
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
    private val values: MutableList<TomlValue> = mutableListOf()
    private val tables: MutableList<TomlNode> = mutableListOf()

    /**
     * @param rootNode The root node to add the array to.
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

    override fun nextElementIndex(): Int {
        // All key-value array elements are on the same line; only increment for
        // table arrays.
        return if (!attributes.isInline) {
            super.nextElementIndex()
        } else {
            elementIndex
        }
    }

    override fun isNextElementKey(descriptor: SerialDescriptor, index: Int): Boolean = false

    override fun appendValue(value: TomlValue) {
        values += value

        // If a primitive array somehow wasn't already marked as such, do so.
        if (!attributes.parent!!.isInline) {
            if (tables.isNotEmpty()) {
                throw InternalEncodingException("Primitive value added to table array")
            }

            attributes.parent.isInline = true
        }

        super.appendValue(value)
    }

    override fun encodeStructure(kind: SerialKind): TomlAbstractEncoder = if (attributes.isInline) {
        when (kind) {
            StructureKind.LIST,
            is PolymorphicKind ->
                // Nested primitive array
                arrayEncoder(rootNode, attributes)
            else ->
                throw UnsupportedEncodingFeatureException(
                    "Inline tables are not yet supported as array elements."
                )
        }
    } else {
        val element = TomlArrayOfTablesElement(
            elementIndex,
            attributes.comments,
            attributes.inlineComment,
            inputConfig
        )

        tables += element

        TomlMainEncoder(
            element,
            nextElementIndex(),
            attributes,
            inputConfig,
            outputConfig,
            serializersModule
        )
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (attributes.isInline) {
            val array = TomlArray(values, "", elementIndex)

            parent?.let {
                appendValueTo(array, parent)
            } ?: parent.run {
                val key = attributes.parent!!.keyOrThrow()

                // Create a key-array pair and add it to the parent.
                rootNode.appendChild(
                    TomlKeyValueArray(
                        TomlKey(key, elementIndex),
                        array,
                        elementIndex,
                        attributes.comments,
                        attributes.inlineComment,
                        key,
                        inputConfig
                    )
                )
            }
        } else {
            // If the root table array contains a single nested table array, move it
            // from its element to the root and mark the root as synthetic.
            collapseSingleChildRoot()
        }

        super.endStructure(descriptor)
    }

    private fun collapseSingleChildRoot() {
        var isSynthetic = false

        tables.singleOrNull()?.let { element ->
            if (element is TomlArrayOfTablesElement) {
                element.children.singleOrNull()?.let { nested ->
                    if (nested is TomlArrayOfTables) {
                        tables.clear()

                        tables += nested
                        isSynthetic = !outputConfig.explicitTables
                    }
                }
            }
        }

        val tableArray = TomlArrayOfTables(
            "[[${attributes.parent!!.getFullKey()}]]",
            elementIndex,
            inputConfig,
            isSynthetic
        )

        tables.forEach(tableArray::appendChild)

        rootNode.appendChild(tableArray)
    }
}
