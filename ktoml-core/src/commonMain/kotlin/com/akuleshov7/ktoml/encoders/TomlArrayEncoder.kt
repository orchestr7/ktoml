package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeEncoder

@OptIn(ExperimentalSerializationApi::class)
public class NewTomlArrayEncoder(
    private val rootNode: TomlNode,
    elementIndex: Int,
    config: TomlInputConfig,
    parentFlags: Flags,
    parentKey: String
) : NewTomlAbstractEncoder(
    elementIndex,
    config,
    parentFlags,
    parentKey
) {
    private lateinit var values: MutableList<TomlValue>

    override fun nextElementIndex(): Int {
        // All key-value array elements are on the same line; only increment for
        // table arrays.
        return if (!parentFlags.isInline) {
            super.nextElementIndex()
        } else {
            elementIndex
        }
    }

    override fun encodeValue(
        value: TomlValue,
        comments: List<String>,
        inlineComment: String
    ) {
        values += value
    }

    override fun <T> encodeStructure(
        kind: SerialKind,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val (_, _, _, isInline) = elementFlags

        val encoder = when (kind) {
            StructureKind.CLASS,
            StructureKind.MAP,
            StructureKind.OBJECT,
            is PolymorphicKind -> {
                if (isInline) {
                    throw UnsupportedEncodingFeatureException(
                        "Inline tables are not yet supported as array elements."
                    )
                }

                NewTomlMainEncoder(
                    rootNode,
                    elementIndex,
                    config,
                    elementFlags.copy(),
                    parentKey
                )
            }
            StructureKind.LIST -> {
                if (!isInline) {
                    throw InternalEncodingException(
                        "Nested array elements must be inline."
                    )
                }

                NewTomlArrayEncoder(
                    rootNode,
                    elementIndex,
                    config,
                    elementFlags.copy(),
                    parentKey!!
                )
            }
            else -> {
                throw InternalEncodingException(
                    "Unknown SerialKind $kind: expected StructureKind or PolymorphicKind."
                )
            }
        }

        serializer.serialize(encoder, value)
    }

    override fun beginCollection(
        descriptor: SerialDescriptor,
        collectionSize: Int
    ): CompositeEncoder {
        parentFlags.isInline =
                if (parentFlags.isInline) {
                    true
                } else when (descriptor.getElementDescriptor(0).kind) {
                    is PrimitiveKind,
                    SerialKind.ENUM,
                    StructureKind.LIST -> true
                    else -> {
                        if (parentFlags.isInline) {
                            throw UnsupportedEncodingFeatureException(
                                "Inline tables are not yet supported as array elements."
                            )
                        }

                        false
                    }
                }

        if (parentFlags.isInline) {
            values = ArrayList(collectionSize)
        } else {
            val tableArray = TomlArrayOfTables(
                "[[${parentKey!!}]]",
                elementIndex + 1,
                config
            )

            // A hack to create a table array via the parsing constructor without
            // creating an element.
            tableArray.children.removeAll { it is TomlArrayOfTablesElement }

            rootNode.insertTableToTree(tableArray)
        }

        return super.beginCollection(descriptor, collectionSize)
    }

    @Suppress("UNCHECKED_CAST")
    override fun endStructure(descriptor: SerialDescriptor) {
        if (parentFlags.isInline) {
            val (_, _, _, _, comments, inlineComment) = parentFlags

            // Create a parent table.
            val parent = parentKey?.let {
                val table = TomlTablePrimitive(
                    "[$it]",
                    elementIndex,
                    config = config
                )

                rootNode.insertTableToTree(table)

                table
            } ?: rootNode

            val array = TomlKeyValueArray(
                TomlKey(elementKey, elementIndex),
                TomlArray(values, "", elementIndex),
                elementIndex++,
                comments,
                inlineComment,
                elementKey,
                config
            )

            parent.appendChild(array)
        }

        super.endStructure(descriptor)
    }
}

/**
 * Encodes a TOML array or table array.
 * @property currentKey The key of the current key-value array or array table.
 * @property isTableArray Whether a table array is being encoded.
 *
 * @param elementIndex The element index to start the array from.
 */
@ExperimentalSerializationApi
public class TomlArrayEncoder(
    override var currentKey: String,
    internal var isTableArray: Boolean,
    elementIndex: Int,
    config: TomlInputConfig = TomlInputConfig()
) : TomlAbstractEncoder(
    elementIndex,
    config,
    isInlineDefault = !isTableArray
) {
    private lateinit var values: MutableList<TomlValue>
    internal lateinit var tableArray: TomlArrayOfTables
    internal lateinit var valueArray: TomlArray

    init {
        prefixKey = currentKey
    }

    // Structure begin and end

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        // Confirm whether we're encoding a table array, since the main encoder
        // only checks for the inline table annotation.
        isTableArray = if (isTableArray) {
            when (descriptor.getElementDescriptor(0).kind) {
                StructureKind.CLASS, StructureKind.MAP -> isTableArray
                is PolymorphicKind -> TODO()
                else -> false
            }
        } else {
            false
        }

        isInlineDefault = !isTableArray
        isInline = isInlineDefault

        if (isTableArray) {
            tableArray = TomlArrayOfTables("[[$currentKey]]", elementIndex)

            // A hack to create a table array via the parsing constructor without
            // creating an element.
            tableArray.children.removeAll { it is TomlArrayOfTablesElement }
        } else {
            values = mutableListOf()
        }

        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (!isTableArray) {
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
        tableArray.insertTableToTree(value)
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

            val enc = TomlMainEncoder(element, elementIndex, config, prefixKey!!)

            serializer.serialize(enc, value)

            elementIndex = enc.elementIndex

            tableArray.appendChild(element)
        }
    }
}
