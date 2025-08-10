package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.InternalEncodingException
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.exceptions.UnsupportedEncodingFeatureException
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.pairs.values.*
import com.akuleshov7.ktoml.utils.isBareKey
import com.akuleshov7.ktoml.utils.isLiteralKeyCandidate

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

/**
 * An abstract Encoder for the TOML format.
 *
 * @property elementIndex The current element index.
 * @property attributes The current attributes.
 * @property outputConfig The output config.
 * @property serializersModule
 */
@OptIn(ExperimentalSerializationApi::class)
public abstract class TomlAbstractEncoder protected constructor(
    protected var elementIndex: Int,
    protected val attributes: TomlEncoderAttributes,
    protected val outputConfig: TomlOutputConfig,
    override val serializersModule: SerializersModule,
) : AbstractEncoder() {
    private var isNextElementKey = false

    @OptIn(ExperimentalTime::class)
    private val instantDescriptor = Instant.serializer().descriptor
    private val localDateTimeDescriptor = LocalDateTime.serializer().descriptor
    private val localDateDescriptor = LocalDate.serializer().descriptor
    private val localTimeDescriptor = LocalTime.serializer().descriptor

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
                "Arbitrary map key ($key) types are not supported. Must be either a string" +
                        " or enum. Provide a custom serializer for $type to either " +
                        "of the supported key types."
            )
        }

        setKey(key)

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
            appendValue(TomlBoolean(value))
        }
    }

    override fun encodeDouble(value: Double) {
        if (!encodeAsKey(value, "Double")) {
            appendValue(TomlDouble(value))
        }
    }

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }

    override fun encodeLong(value: Long) {
        if (!encodeAsKey(value, "Long")) {
            appendValue(TomlLong(value, attributes.intRepresentation))
        }
    }

    override fun encodeNull() {
        appendValue(TomlNull())
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder {
        // Value (inline) class always has one element
        encodeElement(descriptor, 0)
        return this
    }

    override fun encodeString(value: String) {
        if (!encodeAsKey(value)) {
            appendValue(
                if (attributes.isLiteral) {
                    TomlLiteralString(value, attributes.isMultiline)
                } else {
                    TomlBasicString(value, attributes.isMultiline)
                }
            )
        }
    }

    @Suppress("NESTED_BLOCK")
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when (val desc = serializer.descriptor) {
            instantDescriptor,
            localDateTimeDescriptor,
            localDateDescriptor,
            localTimeDescriptor -> if (!encodeAsKey(value as Any, desc.serialName)) {
                appendValue(TomlDateTime(value))
            }
            else -> when (val kind = desc.kind) {
                is StructureKind,
                is PolymorphicKind -> when {
                    desc.isInline -> serializer.serialize(this, value)
                    !encodeAsKey(value as Any, desc.serialName) -> {
                        val encoder = encodeStructure(kind)
                        serializer.serialize(encoder, value)
                        elementIndex = encoder.elementIndex
                        attributes.reset()
                    }
                }
                else -> super.encodeSerializableValue(serializer, value)
            }
        }
    }

    override fun encodeByte(value: Byte): Unit = encodeLong(value.toLong())
    override fun encodeShort(value: Short): Unit = encodeLong(value.toLong())
    override fun encodeInt(value: Int): Unit = encodeLong(value.toLong())
    override fun encodeFloat(value: Float): Unit = encodeDouble(value.toDouble())

    /**
     * https://github.com/akuleshov7/ktoml/issues/260
     * As per the TOML specification, there is no predefined Char type.
     * However, we've introduced the concept in our TOML implementation to align more closely with Kotlin's syntax.
     * So we are expecting Chars to have single quote on the decoding. So encoding should be done in a similar way.
     */
    override fun encodeChar(value: Char): Unit = appendValue(TomlLiteralString(value.toString()))

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
        if (value != null || !outputConfig.ignoreNullValues) {
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

        attributes.resetComments()
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

        // Force primitive array elements to be single-line.
        if (attributes.isInline && descriptor.kind == StructureKind.LIST) {
            when (typeDescriptor.kind) {
                is PrimitiveKind,
                SerialKind.ENUM -> attributes.isMultiline = false
                else -> { }
            }
        }

        return true
    }

    protected open fun isNextElementKey(descriptor: SerialDescriptor, index: Int): Boolean {
        when (val kind = descriptor.kind) {
            StructureKind.CLASS -> {
                // We should keep previous key when we have value (inline) class
                // But if key is null, it means that value class isn't nested, and we have to use its own key
                if (!descriptor.isInline || attributes.key == null) {
                    setKey(descriptor.getElementName(index))
                }
            }
            StructureKind.MAP -> {
                // When the index is even (key) mark the next element as a key and
                // skip annotations and element index incrementing.
                if (index % 2 == 0) {
                    isNextElementKey = true

                    return true
                }
            }
            is PolymorphicKind -> {
                setKey(descriptor.getElementName(index))

                // Ignore annotations on polymorphic types.
                if (index == 0) {
                    nextElementIndex()
                    return true
                }
            }
            StructureKind.LIST -> throw TomlWritingException(
                "It looks like you're trying to encode a list of objects. " +
                        "In TOML notation, this corresponds to an [[array of tables]]. " +
                        "However, ktoml cannot serialize this structure without a proper name — it cannot infer or construct one automatically. " +
                        "Please wrap your list in a serializable class, for example: data class A(val list: List<MyObject>). " +
                        "This will produce the expected TOML representation: [[list]]."
            )
            else -> throw InternalEncodingException("Unknown parent kind: $kind.")
        }

        return false
    }

    /**
     * Set the key attribute to [key], quoting and escaping as necessary.
     *
     * @param key
     */
    protected fun setKey(key: String) {
        attributes.key = when {
            key.isBareKey() -> key
            key.isLiteralKeyCandidate() -> "'$key'"
            else -> "\"$key\""
        }
    }

    /**
     * Creates a new array encoder instance, with this encoder as its parent.
     *
     * @param rootNode The new encoder's root node.
     * @param attributes The new encoder's attributes.
     * @return The new instance.
     */
    protected open fun arrayEncoder(
        rootNode: TomlNode,
        attributes: TomlEncoderAttributes = this.attributes.child()
    ): TomlAbstractEncoder =
        TomlArrayEncoder(
            rootNode,
            parent = this,
            elementIndex,
            attributes,
            outputConfig,
            serializersModule
        )

    /**
     * Creates a new inline table encoder instance, with the encoder as its parent.
     *
     * @param rootNode The new encoder's root node.
     * @return The new instance.
     */
    protected open fun inlineTableEncoder(rootNode: TomlNode): TomlAbstractEncoder =
        TomlInlineTableEncoder(
            rootNode,
            parent = this,
            elementIndex,
            attributes.child(),
            outputConfig,
            serializersModule
        )

    /**
     * Creates a new table encoder instance.
     *
     * @param rootNode The new encoder's root node.
     * @return The new instance.
     */
    protected open fun tableEncoder(rootNode: TomlNode): TomlAbstractEncoder =
        TomlMainEncoder(
            rootNode,
            elementIndex,
            attributes.child(),
            outputConfig,
            serializersModule
        )
}
