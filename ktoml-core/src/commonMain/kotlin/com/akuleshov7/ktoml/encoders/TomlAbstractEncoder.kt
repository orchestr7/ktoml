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
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * An abstract Encoder for the TOML format.
 * @property elementIndex The current element index. The next element index will be
 * this `+ 1`
 * @property config The input config, used for constructing nodes.
 */
@OptIn(ExperimentalSerializationApi::class)
public abstract class TomlAbstractEncoder(
    internal var elementIndex: Int = -1,
    protected val config: TomlInputConfig,
    private val isInlineDefault: Boolean = false
) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule
    protected abstract var currentKey: String

    // Flags
    private var isStringMultiline = false
    private var isStringLiteral = false
    private var intRepresentation = IntegerRepresentation.DECIMAL
    private var isInline = isInlineDefault
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
        val literal = if (isStringLiteral) {
            isStringLiteral = false

            true
        } else {
            false
        }

        if (isStringMultiline) {
            isStringMultiline = false

            throw UnsupportedEncodingFeatureException(
                "multiline strings are not yet supported."
            )
        }

        encodeValue(
            if (literal) {
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
            StructureKind.MAP -> encodeTableLike(serializer, value)
            else -> encodeSerializableValue(serializer, value)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        nextElementIndex()

        // Find annotations

        val typeAnnotations = descriptor.annotations

        val elementAnnotations = when (val kind = descriptor.kind) {
            StructureKind.CLASS -> {
                currentKey = descriptor.getElementName(index)

                descriptor.getElementAnnotations(index)
            }
            StructureKind.MAP -> {
                // Avoid interpreting annotations twice (key and value). We only
                // want annotations from the value.
                if (index % 2 == 0) {
                    return super.encodeElement(descriptor, index)
                }

                descriptor.getElementAnnotations(1)
            }
            StructureKind.LIST -> descriptor.getElementAnnotations(0)
            is PolymorphicKind -> throw UnsupportedEncodingFeatureException(
                "Polymorphic types are not yet supported"
            )
            else -> throw InternalEncodingException("Unknown parent kind $kind")
        }

        typeAnnotations.setFlags()
        elementAnnotations.setFlags()

        return super.encodeElement(descriptor, index)
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
