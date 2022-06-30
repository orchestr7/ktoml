package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder

/**
 * Encodes a TOML array or table array.
 * @property currentKey The key of the current key-value array or array table.
 * @property isTableArray Whether a table array is being encoded.
 *
 * @param elementIndex The element index to start the array from.
 */
public class TomlArrayEncoder(
    override val currentKey: String,
    internal val isTableArray: Boolean,
    elementIndex: Int,
    config: TomlInputConfig = TomlInputConfig()
) : TomlAbstractEncoder(
    elementIndex,
    config,
    isInlineDefault = true
) {
    private lateinit var tables: MutableList<TomlNode>
    private lateinit var values: MutableList<TomlValue>
    internal lateinit var tableArray: TomlArrayOfTables
    internal lateinit var valueArray: TomlArray

    // Structure begin and end

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        if (isTableArray) {
            tableArray = TomlArrayOfTables("[[$currentKey]]", elementIndex)
        } else {
            values = mutableListOf()
        }

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (isTableArray) {
            tableArray = TomlArrayOfTables(
                content = "[[$currentKey]]",
                lineNo = elementIndex,
            )

            // A hack to create a table array via the parsing constructor without
            // creating an element.
            tableArray.children.removeAll { it is TomlArrayOfTablesElement }

            tables.forEach {
                tableArray.appendChild(it)
            }
        } else {
            valueArray = TomlArray(
                values,
                rawContent = "",
                lineNo = elementIndex
            )
        }
    }

    // Elements

    override fun nextElementIndex() {
        // All key-value array elements are on the same line; only increment for
        // table arrays.
        if (isTableArray) {
            super.nextElementIndex()
        }
    }

    override fun encodeValue(
        value: TomlValue,
        comments: List<String>,
        inlineComment: String
    ) {
        values += value
    }

    override fun encodeTable(value: TomlTable) {
        tables += value
    }

    override fun <T> encodeTableLike(
        serializer: SerializationStrategy<T>,
        value: T,
        isInline: Boolean,
        comments: List<String>,
        inlineComment: String
    ) {
        if (isInline) {
            throw UnsupportedEncodingFeatureException(
                "Inline tables are not yet supported as array elements."
            )
        } else {
            val element = TomlArrayOfTablesElement(elementIndex, comments, inlineComment, config)

            val enc = TomlMainEncoder(element, elementIndex, config)

            serializer.serialize(enc, value)

            tables += element

            elementIndex = enc.elementIndex
        }
    }
}
