package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.annotations.*
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.exceptions.IllegalEncodingTypeException
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.*
import com.akuleshov7.ktoml.writers.IntegerRepresentation
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * An abstract Encoder for the TOML format.
 *
 * @param elementIndex The current element index.
 * @property config
 * @property parentFlags The flags inherited from the parent element.
 * @property parentKey The parent element's key. Used for constructing table keys.
 */
@OptIn(ExperimentalSerializationApi::class)
@Suppress("CUSTOM_GETTERS_SETTERS")
public abstract class NewTomlAbstractEncoder
protected constructor(
    elementIndex: Int = -1,
    protected val config: TomlInputConfig,
    protected val parentFlags: Flags = Flags(),
    protected val parentKey: String? = null
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule
    private var isNextElementKey = false
    protected val elementFlags: Flags = Flags(parentFlags)
    public var elementIndex: Int = elementIndex
        protected set
    protected lateinit var elementKey: String

    protected fun getFullKey(): String =
            parentKey?.let {
                "$parentKey.$elementKey"
            } ?: elementKey

    protected open fun nextElementIndex(): Int = ++elementIndex

    protected fun encodeValue(value: TomlValue) {
        val (_, _, _, _, comments, inlineComment) = elementFlags

        elementFlags.reset(parentFlags)

        encodeValue(value, comments, inlineComment)
    }

    protected abstract fun encodeValue(
        value: TomlValue,
        comments: List<String>,
        inlineComment: String
    )

    protected abstract fun <T> encodeStructure(
        kind: SerialKind,
        serializer: SerializationStrategy<T>,
        value: T
    )

    // Encoder

    override fun encodeBoolean(value: Boolean) {
        if (isNextElementKey) {
            isNextElementKey = false

            invalidKeyType("Boolean")
        }

        encodeValue(TomlBoolean(value, elementIndex))
    }

    override fun encodeLong(value: Long) {
        if (isNextElementKey) {
            isNextElementKey = false

            invalidKeyType("Long")
        }

        if (elementFlags.intRepresentation != IntegerRepresentation.DECIMAL) {
            throw UnsupportedEncodingFeatureException(
                "Non-decimal integer representation is not yet supported."
            )
        }

        encodeValue(TomlLong(value, elementIndex))
    }

    override fun encodeDouble(value: Double) {
        if (isNextElementKey) {
            isNextElementKey = false

            invalidKeyType("Double")
        }

        encodeValue(TomlDouble(value, elementIndex))
    }

    override fun encodeString(value: String) {
        if (isNextElementKey) {
            isNextElementKey = false

            elementKey = value
        }

        if (elementFlags.isMultiline) {
            throw UnsupportedEncodingFeatureException(
                "Multiline strings are not yet supported."
            )
        }

        encodeValue(
            if (elementFlags.isLiteral) {
                TomlLiteralString(value as Any, elementIndex)
            } else {
                TomlBasicString(value as Any, elementIndex)
            }
        )
    }

    override fun encodeNull() {
        if (isNextElementKey) {
            isNextElementKey = false

            invalidKeyType("null")
        }

        encodeValue(TomlNull(elementIndex))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when (val descriptor = serializer.descriptor) {
            Instant.serializer().descriptor,
            LocalDateTime.serializer().descriptor,
            LocalDate.serializer().descriptor -> encodeValue(TomlDateTime(value as Any, elementIndex))
            else -> when (val kind = descriptor.kind) {
                is StructureKind,
                is PolymorphicKind -> {
                    encodeStructure(kind, serializer, value)

                    elementFlags.reset(parentFlags)
                }
                else -> super.encodeSerializableValue(serializer, value)
            }
        }
    }

    override fun encodeByte(value: Byte): Nothing = invalidType("Byte", "Long", value)
    override fun encodeShort(value: Short): Nothing = invalidType("Short", "Long", value)
    override fun encodeInt(value: Int): Nothing = invalidType("Int", "Long", value)
    override fun encodeFloat(value: Float): Nothing = invalidType("Float", "Double", value)
    override fun encodeChar(value: Char): Nothing = invalidType("Char", "String", value)

    // Todo: Do we really want to make these invalid?
    private fun invalidType(
        typeName: String,
        requiredType: String,
        value: Any
    ): Nothing {
        throw IllegalEncodingTypeException(
            "<$typeName> is not allowed by the TOML specification, use <$requiredType>" +
                    " instead (key = ${getFullKey()}; value = $value)",
            elementIndex
        )
    }

    private fun invalidKeyType(type: String): Nothing {
        throw UnsupportedEncodingFeatureException(
            "Arbitrary map key types are not supported. Must be either a string" +
                    " or enum. Provide a custom serializer for $type to either " +
                    "of the supported key types."
        )
    }

    // CompositeEncoder

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        parentFlags.set(descriptor.annotations)

        return this
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        nextElementIndex()

        when (val kind = descriptor.kind) {
            StructureKind.CLASS -> elementKey = descriptor.getElementName(index)
            StructureKind.MAP -> {
                // When the index is even (key) mark the next element as a key and
                // ignore annotations.
                if (index % 2 == 0) {
                    isNextElementKey = true

                    return true
                }
            }
            StructureKind.LIST -> { }
            is PolymorphicKind -> {
                elementKey = descriptor.getElementName(index)

                // Ignore annotations on polymorphic types.
                if (index == 0) {
                    return true
                }
            }
            else -> throw InternalEncodingException("Unknown parent kind $kind")
        }

        val typeAnnotations = descriptor.getElementDescriptor(index).annotations
        val elementAnnotation = descriptor.getElementAnnotations(index)

        elementFlags.set(typeAnnotations)
        elementFlags.set(elementAnnotation)

        return true
    }

    /**
     * @property isMultiline Marks subsequent key-string or array pair elements to
     * be written as multiline.
     * @property isLiteral Marks subsequent key-string pair elements to be written
     * as string literals.
     * @property intRepresentation Changes how subsequent key-integer pair elements
     * are represented.
     * @property isInline Marks subsequent table-like elements as inline. Tables
     * will be written as inline tables.
     * @property comments Comment lines to be prepended before the next element.
     * @property inlineComment A comment to be appended to the end of the next
     * element's line.
     */
    public data class Flags(
        public var isMultiline: Boolean = false,
        public var isLiteral: Boolean = false,
        public var intRepresentation: IntegerRepresentation = IntegerRepresentation.DECIMAL,
        public var isInline: Boolean = false,
        public var comments: List<String> = emptyList(),
        public var inlineComment: String = ""
    ) {
        public constructor(parent: Flags) : this() {
            inheritFrom(parent)
        }

        public fun inheritFrom(parent: Flags) {
            isMultiline = parent.isMultiline
            isLiteral = parent.isLiteral
            intRepresentation = parent.intRepresentation
            isInline = parent.isInline
        }

        public fun reset(parent: Flags) {
            inheritFrom(parent)
            comments = emptyList()
            inlineComment = ""
        }

        public fun set(annotations: Iterable<Annotation>) {
            annotations.forEach { annotation ->
                when (annotation) {
                    is TomlLiteral -> isLiteral = true
                    is TomlMultiline -> isMultiline = true
                    is TomlInteger -> intRepresentation = annotation.representation
                    is TomlComments -> {
                        comments = annotation.lines.asList()
                        inlineComment = annotation.inline
                    }
                    is TomlInlineTable -> isInline = true
                }
            }
        }
    }
}

/**
 * An abstract Encoder for the TOML format.
 * @property elementIndex The current element index. The next element index will be
 * this `+ 1`
 * @property config The input config, used for constructing nodes.
 * @property isInlineDefault
 */
@OptIn(ExperimentalSerializationApi::class)
public abstract class TomlAbstractEncoder(
    internal var elementIndex: Int = -1,
    protected val config: TomlInputConfig,
    protected var isInlineDefault: Boolean = false
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule
    internal var prefixKey: String? = null
    protected abstract var currentKey: String

    // Flags
    private var isStringMultiline = false
    private var isStringLiteral = false
    private var intRepresentation = IntegerRepresentation.DECIMAL
    protected var isInline: Boolean = isInlineDefault
    private var comments: List<String> = emptyList()
    private var inlineComment: String = ""

    private fun clearComments() {
        comments = emptyList()
    }

    private fun clearInlineComment() {
        inlineComment = ""
    }

    protected open fun resetInlineFlag() {
        // Reset isInline to its default, which is false for the main decoder and
        // true for the array and inline table decoders.
        isInline = isInlineDefault
    }

    internal abstract fun encodeValue(
        value: TomlValue,
        comments: List<String> = this.comments.also { clearComments() },
        inlineComment: String = this.inlineComment.also { clearInlineComment() }
    )

    internal abstract fun encodeTable(value: TomlTable)

    protected abstract fun <T> encodeTableLike(
        serializer: SerializationStrategy<T>,
        value: T,
        isInline: Boolean = this.isInline.also { resetInlineFlag() },
        comments: List<String> = this.comments.also { clearComments() },
        inlineComment: String = this.inlineComment.also { clearInlineComment() }
    )

    protected open fun nextElementIndex() {
        ++elementIndex
    }

    // Single values

    override fun encodeByte(value: Byte): Nothing = invalidType("Byte", "Long", value)
    override fun encodeShort(value: Short): Nothing = invalidType("Short", "Long", value)
    override fun encodeInt(value: Int): Nothing = invalidType("Int", "Long", value)
    override fun encodeFloat(value: Float): Nothing = invalidType("Float", "Double", value)
    override fun encodeChar(value: Char): Nothing = invalidType("Char", "String", value)

    override fun encodeBoolean(value: Boolean) {
        encodeValue(TomlBoolean(value, elementIndex))
    }

    override fun encodeLong(value: Long) {
        if (intRepresentation != IntegerRepresentation.DECIMAL) {
            intRepresentation = IntegerRepresentation.DECIMAL

            throw UnsupportedEncodingFeatureException(
                "Non-decimal integer representation is not yet supported."
            )
        }

        encodeValue(TomlLong(value, elementIndex))
    }

    override fun encodeDouble(value: Double) {
        encodeValue(TomlDouble(value, elementIndex))
    }

    override fun encodeString(value: String) {
        if (isStringMultiline) {
            throw UnsupportedEncodingFeatureException(
                "multiline strings are not yet supported."
            )
        }

        encodeValue(
            if (isStringLiteral) {
                TomlLiteralString(value as Any, elementIndex)
            } else {
                TomlBasicString(value as Any, elementIndex)
            }
        )
    }

    override fun encodeNull() {
        encodeValue(TomlNull(elementIndex))
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when (serializer) {
            Instant.serializer(),
            LocalDateTime.serializer(),
            LocalDate.serializer() -> encodeValue(TomlDateTime(value as Any, elementIndex))
            else -> super.encodeSerializableValue(serializer, value)
        }
    }

    // Todo: Do we really want to make these invalid?
    private fun invalidType(
        typeName: String,
        requiredType: String,
        value: Any
    ): Nothing =
            throw IllegalEncodingTypeException(
                "<$typeName> is not allowed by the TOML specification," +
                        " use <$requiredType> instead (key = $currentKey; value = $value)",
                elementIndex
            )

    // Structure

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (!encodeElement(descriptor, index)) {
            return
        }

        when (val parentKind = descriptor.kind) {
            StructureKind.MAP -> {
                // Serialize the key and skip.
                if (index % 2 == 0) {
                    when (descriptor.getElementDescriptor(index)) {
                        String.serializer().descriptor -> currentKey = value as String
                        else -> {
                            // A hack to get the key from custom serializers
                            serializer.serialize(
                                object : AbstractEncoder() {
                                    override val serializersModule: SerializersModule = this@TomlAbstractEncoder.serializersModule

                                    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
                                        encodeString(enumDescriptor.getElementName(index))
                                    }

                                    override fun encodeString(value: String) {
                                        currentKey = value
                                    }

                                    override fun encodeValue(value: Any) {
                                        throw UnsupportedEncodingFeatureException(
                                            "Arbitrary map key types are not supported. Must be" +
                                                    " either a string or enum. Provide a custom" +
                                                    " serializer for ${value::class.simpleName}" +
                                                    "to either of the supported key types."
                                        )
                                    }
                                },
                                value
                            )
                        }
                    }

                    return
                }
            }
            StructureKind.LIST,
            StructureKind.CLASS -> { }
            else -> throw InternalEncodingException("Unknown parent kind $parentKind")
        }

        when (val kind = descriptor.getElementDescriptor(index).kind) {
            StructureKind.LIST -> {
                val enc = TomlArrayEncoder(currentKey, !isInline, elementIndex, config)

                serializer.serialize(enc, value)

                if (enc.isTableArray) {
                    encodeTable(enc.tableArray)
                } else {
                    encodeValue(enc.valueArray)
                }

                elementIndex = enc.elementIndex
            }
            StructureKind.CLASS,
            StructureKind.MAP -> {
                if (!isInline && prefixKey != null) {
                    currentKey = "$prefixKey.$currentKey"
                }

                encodeTableLike(serializer, value)
            }
            else -> encodeSerializableValue(serializer, value)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        nextElementIndex()

        // Find annotations

        val parentAnnotations = descriptor.annotations
        val typeAnnotations: List<Annotation>
        val elementAnnotations: List<Annotation>

        when (val kind = descriptor.kind) {
            StructureKind.CLASS -> {
                currentKey = descriptor.getElementName(index)

                typeAnnotations = descriptor.getElementDescriptor(index).annotations
                elementAnnotations = descriptor.getElementAnnotations(index)
            }
            StructureKind.MAP -> {
                // Avoid interpreting annotations twice (key and value). We only
                // want annotations from the value.
                if (index % 2 == 0) {
                    return super.encodeElement(descriptor, index)
                }

                typeAnnotations = descriptor.getElementDescriptor(1).annotations
                elementAnnotations = descriptor.getElementAnnotations(1)
            }
            StructureKind.LIST -> {
                typeAnnotations = descriptor.getElementDescriptor(0).annotations
                elementAnnotations = descriptor.getElementAnnotations(0)
            }
            is PolymorphicKind -> throw UnsupportedEncodingFeatureException(
                "Polymorphic types are not yet supported"
            )
            else -> throw InternalEncodingException("Unknown parent kind $kind")
        }

        parentAnnotations.setFlags()
        typeAnnotations.setFlags()
        elementAnnotations.setFlags()

        return super.encodeElement(descriptor, index)
    }

    private fun resetFlags() {
        isStringMultiline = false
        isStringLiteral = false
        intRepresentation = IntegerRepresentation.DECIMAL
        isInline = isInlineDefault
        comments = emptyList()
        inlineComment = ""
    }

    private fun Iterable<Annotation>.setFlags() {
        forEach { annotation ->
            when (annotation) {
                is TomlLiteral -> isStringLiteral = true
                is TomlMultiline -> isStringMultiline = true
                is TomlInteger -> intRepresentation = annotation.representation
                is TomlComments -> {
                    comments = annotation.lines.asList()
                    inlineComment = annotation.inline
                }
                is TomlInlineTable -> isInline = true
            }
        }
    }
}