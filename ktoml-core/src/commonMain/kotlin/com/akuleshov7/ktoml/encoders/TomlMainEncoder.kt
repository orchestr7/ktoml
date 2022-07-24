package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.tree.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeEncoder

/**
 * Encodes a TOML file or table.
 * @property rootNode The root node to add elements to.
 *
 * @param elementIndex The current element index. passed to nodes as the `lineNo`
 * parameter.
 * @param config A [TomlInputConfig] instance, passed to nodes.
 * @param parentFlags The flags inherited from the parent element.
 * @param parentKey The parent element's key. Used for constructing table keys.
 */
@OptIn(ExperimentalSerializationApi::class)
public class NewTomlMainEncoder(
    private val rootNode: TomlNode,
    elementIndex: Int = -1,
    config: TomlInputConfig = TomlInputConfig(),
    parentFlags: Flags = Flags(),
    parentKey: String? = null
) : NewTomlAbstractEncoder(
    elementIndex,
    config,
    parentFlags,
    parentKey
) {
    private val children = mutableListOf<TomlNode>()
    private var startElementIndex = -1

    override fun encodeValue(
        value: TomlValue,
        comments: List<String>,
        inlineComment: String
    ) {
        val key = TomlKey(elementKey, elementIndex)

        val pair =
            if (value is TomlArray) {
                TomlKeyValueArray(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment,
                    elementKey,
                    config
                )
            } else {
                TomlKeyValuePrimitive(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment,
                    elementKey,
                    config
                )
            }

        if (parentKey == null) {
            rootNode.appendChild(pair)
        } else {
            children += pair
        }
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
                    TODO()
                } else {
                    NewTomlMainEncoder(
                        rootNode,
                        elementIndex,
                        config,
                        elementFlags,
                        getFullKey()
                    ).also {
                        elementIndex = it.elementIndex
                    }
                }
            }
            StructureKind.LIST -> {
                NewTomlArrayEncoder(
                    rootNode,
                    elementIndex,
                    config,
                    elementFlags,
                    getFullKey()
                ).also {
                    elementIndex = it.elementIndex
                }
            }
            else ->
                throw InternalEncodingException(
                    "Unknown SerialKind $kind: expected StructureKind or PolymorphicKind."
                )
        }

        serializer.serialize(encoder, value)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        return super.beginStructure(descriptor).also {
            startElementIndex = elementIndex
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        parentKey?.let {
            val (_, _, _, _, comments, inlineComment) = parentFlags

            val table = TomlTablePrimitive(
                "[$it]",
                startElementIndex,
                comments,
                inlineComment,
                config
            )

            children.forEach(table::appendChild)

            rootNode.insertTableToTree(table)
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
         * @param config The input config, used for constructing nodes.
         * @return The encoded [TomlFile] node.
         */
        public fun <T> encode(
            serializer: SerializationStrategy<T>,
            value: T,
            config: TomlInputConfig = TomlInputConfig()
        ): TomlFile {
            val root = TomlFile(config)

            val encoder = NewTomlMainEncoder(root, config = config)

            serializer.serialize(encoder, value)

            return root
        }
    }
}

/**
 * Encodes a TOML file or table.
 * @property root The root node to add elements to.
 */
public class TomlMainEncoder(
    private val root: TomlNode,
    startElementIndex: Int = -1,
    config: TomlInputConfig = TomlInputConfig()
) : TomlAbstractEncoder(startElementIndex, config) {
    override lateinit var currentKey: String

    internal constructor(
        root: TomlNode,
        startElementIndex: Int,
        config: TomlInputConfig,
        prefixKey: String
    ) : this(
        root,
        startElementIndex,
        config
    ) {
        this.prefixKey = prefixKey
    }

    override fun encodeValue(
        value: TomlValue,
        comments: List<String>,
        inlineComment: String
    ) {
        val key = TomlKey(currentKey, elementIndex)

        root.appendChild(
            if (value is TomlArray) {
                TomlKeyValueArray(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment,
                    currentKey,
                    config
                )
            } else {
                TomlKeyValuePrimitive(
                    key,
                    value,
                    elementIndex,
                    comments,
                    inlineComment,
                    currentKey,
                    config
                )
            }
        )
    }

    override fun encodeTable(value: TomlTable) {
        root.insertTableToTree(value)
    }

    override fun <T> encodeTableLike(
        serializer: SerializationStrategy<T>,
        value: T,
        isInline: Boolean,
        comments: List<String>,
        inlineComment: String
    ) {
        if (isInline) {
            val enc = TomlInlineTableEncoder(currentKey, elementIndex, config)

            serializer.serialize(enc, value)

            root.appendChild(
                TomlInlineTable(
                    "",
                    elementIndex,
                    currentKey,
                    enc.keyValues,
                    comments,
                    inlineComment,
                    config
                )
            )
        } else {
            val root = TomlTablePrimitive("[$currentKey]", elementIndex, comments, inlineComment, config)

            val enc = TomlMainEncoder(root, elementIndex, config)

            serializer.serialize(enc, value)

            encodeTable(root)

            elementIndex = enc.elementIndex
        }
    }

    public companion object {
        /**
         * Encodes the specified [value] into a [TomlFile].
         *
         * @param serializer The user-defined or compiler-generated serializer for
         * type [T].
         * @param value The value to serialize.
         * @param config The input config, used for constructing nodes.
         * @return The encoded [TomlFile] node.
         */
        public fun <T> encode(
            serializer: SerializationStrategy<T>,
            value: T,
            config: TomlInputConfig = TomlInputConfig()
        ): TomlFile {
            val root = TomlFile(config)

            val encoder = TomlMainEncoder(root, config = config)

            encoder.encodeSerializableValue(serializer, value)

            return root
        }
    }
}
