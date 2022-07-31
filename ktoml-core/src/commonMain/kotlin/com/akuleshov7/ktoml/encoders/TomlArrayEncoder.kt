package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeEncoder

/**
 * @property parentValues The value list of a nested array's parent.
 */
@OptIn(ExperimentalSerializationApi::class)
public class TomlArrayEncoder internal constructor(
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
    private lateinit var values: MutableList<TomlValue>
    private lateinit var tables: TomlArrayOfTables

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

    override fun nextElementIndex(): Int {
        // All key-value array elements are on the same line; only increment for
        // table arrays.
        return if (!attributes.isInline) {
            super.nextElementIndex()
        } else {
            elementIndex
        }
    }

    override fun setElementKey(descriptor: SerialDescriptor, index: Int): Boolean = false

    override fun appendValue(value: TomlValue) {
        values += value

        super.appendValue(value)
    }

    override fun <T> encodeStructure(
        kind: SerialKind,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (attributes.isInline) {
            if (kind == StructureKind.LIST) {
                // Nested primitive array
                val encoder = TomlArrayEncoder(
                    rootNode,
                    parent = this,
                    elementIndex,
                    attributes,
                    inputConfig,
                    outputConfig
                )

                serializer.serialize(encoder, value)
            } else {
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

            tables.appendChild(element)

            val encoder = TomlMainEncoder(
                element,
                nextElementIndex(),
                attributes,
                inputConfig,
                outputConfig
            )

            serializer.serialize(encoder, value)

            setElementIndex(from = encoder)
        }
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        if (attributes.isInline) {
            values = ArrayList(collectionSize)

            if (parent == null) {
                val key = attributes.keyOrThrow()

                // Create a key-array pair and add it to the parent.
                val array = TomlKeyValueArray(
                    TomlKey(key, elementIndex),
                    TomlArray(values, "", elementIndex),
                    elementIndex,
                    attributes.comments,
                    attributes.inlineComment,
                    key,
                    inputConfig
                )

                rootNode.appendChild(array)
            } else {
                // Create an array nested in the specified parent list.
                appendValueTo(TomlArray(values, "", elementIndex), parent)
            }
        } else {
            tables = TomlArrayOfTables("[[${attributes.parent!!.getFullKey()}]]", elementIndex, inputConfig)

            rootNode.insertTableToTree(tables)
        }

        return super.beginStructure(descriptor)
    }
}
