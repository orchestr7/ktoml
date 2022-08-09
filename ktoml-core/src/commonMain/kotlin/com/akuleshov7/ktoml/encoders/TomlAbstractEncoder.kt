package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.*
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.exceptions.IllegalEncodingTypeException
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.*
import com.akuleshov7.ktoml.writers.IntegerRepresentation
import com.akuleshov7.ktoml.writers.IntegerRepresentation.DECIMAL
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.SerializersModule

/**
 * An abstract Encoder for the TOML format.
 *
 * @property elementIndex The current element index.
 * @property attributes The current attributes.
 * @property inputConfig The input config, used for constructing nodes.
 * @property outputConfig The output config.
 */
@OptIn(ExperimentalSerializationApi::class)
public abstract class TomlAbstractEncoder protected constructor(
    protected var elementIndex: Int,
    protected val attributes: Attributes,
    protected val inputConfig: TomlInputConfig,
    protected val outputConfig: TomlOutputConfig,
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = outputConfig.serializersModule
    private var isNextElementKey = false
    private val instantDescriptor = Instant.serializer().descriptor
    private val localDateTimeDescriptor = LocalDateTime.serializer().descriptor
    private val localDateDescriptor = LocalDate.serializer().descriptor

    protected open fun nextElementIndex(): Int = ++elementIndex

    // Values

    @Suppress("FUNCTION_BOOLEAN_PREFIX")
    private fun encodeAsKey(key: Any, type: String? = null): Boolean {
        if (!isNextElementKey) {
            return false
        }

        isNextElementKey = false

        if (key !is String) {
            throw UnsupportedEncodingFeatureException(
                "Arbitrary map key types are not supported. Must be either a string" +
                        " or enum. Provide a custom serializer for $type to either " +
                        "of the supported key types."
            )
        }

        attributes.key = key

        return true
    }

    protected open fun appendValue(value: TomlValue) {
        attributes.reset()
    }

    /**
     * Allows [TomlInlineTableEncoder] and [TomlArrayEncoder] to access another
     * encoder's [appendValue] function.
     *
     * @param value
     * @param parent
     */
    internal fun appendValueTo(value: TomlValue, parent: TomlAbstractEncoder) {
        parent.appendValue(value)
    }

    override fun encodeBoolean(value: Boolean) {
        if (!encodeAsKey(value, "Boolean")) {
            appendValue(TomlBoolean(value, elementIndex))
        }
    }

    override fun encodeDouble(value: Double) {
        if (!encodeAsKey(value, "Double")) {
            appendValue(TomlDouble(value, elementIndex))
        }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun encodeLong(value: Long) {
        if (attributes.intRepresentation != DECIMAL) {
            throw UnsupportedEncodingFeatureException(
                "Non-decimal integer representation is not yet supported."
            )
        }

        if (!encodeAsKey(value, "Long")) {
            appendValue(TomlLong(value, elementIndex))
        }
    }

    override fun encodeNull() {
        appendValue(TomlNull(elementIndex))
    }

    override fun encodeString(value: String) {
        if (attributes.isMultiline) {
            throw UnsupportedEncodingFeatureException(
                "Multiline strings are not yet supported."
            )
        }

        if (!encodeAsKey(value, "String")) {
            appendValue(
                if (attributes.isLiteral) {
                    TomlLiteralString(value as Any, elementIndex)
                } else {
                    TomlBasicString(value as Any, elementIndex)
                }
            )
        }
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when (val desc = serializer.descriptor) {
            instantDescriptor,
            localDateTimeDescriptor,
            localDateDescriptor -> if (!encodeAsKey(value as Any, desc.serialName)) {
                appendValue(TomlDateTime(value as Any, elementIndex))
            }
            else -> when (val kind = desc.kind) {
                is StructureKind,
                is PolymorphicKind -> if (!encodeAsKey(value as Any, desc.serialName)) {
                    val encoder = encodeStructure(kind)

                    serializer.serialize(encoder, value)

                    elementIndex = encoder.elementIndex

                    attributes.reset()
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
                    " instead (key = ${attributes.getFullKey()}; value = $value)",
            elementIndex
        )
    }

    // Structure

    protected abstract fun encodeStructure(kind: SerialKind): TomlAbstractEncoder

    override fun shouldEncodeElementDefault(
        descriptor: SerialDescriptor,
        index: Int
    ): Boolean = !outputConfig.ignoreDefaultValues

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (value != null || outputConfig.ignoreNullValues) {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        if (isNextElementKey(descriptor, index)) {
            return true
        }

        nextElementIndex()

        val typeDescriptor = descriptor.getElementDescriptor(index)
        val typeAnnotations = typeDescriptor.annotations
        val elementAnnotations = descriptor.getElementAnnotations(index)

        attributes.set(typeAnnotations)
        attributes.set(elementAnnotations)

        // Force primitive array elements to be inline.
        if (!attributes.isInline && typeDescriptor.kind == StructureKind.LIST) {
            when (typeDescriptor.getElementDescriptor(0).kind) {
                is PrimitiveKind,
                SerialKind.ENUM,
                StructureKind.LIST -> attributes.isInline = true
                else -> { }
            }
        }

        return true
    }

    protected open fun isNextElementKey(descriptor: SerialDescriptor, index: Int): Boolean {
        when (val kind = descriptor.kind) {
            StructureKind.CLASS -> attributes.key = descriptor.getElementName(index)
            StructureKind.MAP -> {
                // When the index is even (key) mark the next element as a key and
                // skip annotations and element index incrementing.
                if (index % 2 == 0) {
                    isNextElementKey = true

                    return true
                }
            }
            is PolymorphicKind -> {
                attributes.key = descriptor.getElementName(index)

                // Ignore annotations on polymorphic types.
                if (index == 0) {
                    nextElementIndex()
                    return true
                }
            }
            else -> throw InternalEncodingException("Unknown parent kind: $kind.")
        }

        return false
    }

    /**
     * @property parent The parent to inherit default values from.
     * @property key The current element's key.
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
    public data class Attributes(
        public val parent: Attributes? = null,
        public var key: String? = null,
        public var isMultiline: Boolean = false,
        public var isLiteral: Boolean = false,
        public var intRepresentation: IntegerRepresentation = DECIMAL,
        public var isInline: Boolean = false,
        public var comments: List<String> = emptyList(),
        public var inlineComment: String = ""
    ) {
        public fun keyOrThrow(): String = key ?: throw InternalEncodingException("Key not set")

        public fun child(): Attributes = copy(parent = copy())

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

        public fun reset() {
            key = null

            val parent = parent ?: Attributes()

            isMultiline = parent.isMultiline
            isLiteral = parent.isLiteral
            intRepresentation = parent.intRepresentation
            isInline = parent.isInline
            comments = parent.comments
            inlineComment = parent.inlineComment
        }

        public fun getFullKey(): String {
            val elementKey = keyOrThrow()

            return parent?.let {
                "${it.keyOrThrow()}.$elementKey"
            } ?: elementKey
        }
    }
}
