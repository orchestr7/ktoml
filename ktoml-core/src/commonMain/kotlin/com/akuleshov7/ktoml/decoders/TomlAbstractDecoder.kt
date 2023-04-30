package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlDouble
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLiteralString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLong
import com.akuleshov7.ktoml.utils.FloatingPointLimitsEnum
import com.akuleshov7.ktoml.utils.FloatingPointLimitsEnum.*
import com.akuleshov7.ktoml.utils.IntegerLimitsEnum
import com.akuleshov7.ktoml.utils.IntegerLimitsEnum.*
import com.akuleshov7.ktoml.utils.convertSpecialCharacters
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encoding.AbstractDecoder

/**
 * Abstract Decoder for TOML format that is inherited by each and every decoder in this project.
 * It serves one aim: to define decoders for primitive types that are allowed in TOML.
 */
@ExperimentalSerializationApi
public abstract class TomlAbstractDecoder : AbstractDecoder() {
    private val instantSerializer = Instant.serializer()
    private val localDateTimeSerializer = LocalDateTime.serializer()
    private val localDateSerializer = LocalDate.serializer()
    private val localTimeSerializer = LocalTime.serializer()

    // Invalid Toml primitive types, but we anyway support them with some limitations
    override fun decodeByte(): Byte = decodePrimitiveType()
    override fun decodeShort(): Short = decodePrimitiveType()
    override fun decodeInt(): Int = decodePrimitiveType()
    override fun decodeFloat(): Float = decodePrimitiveType()
    override fun decodeChar(): Char {
        val keyValue = decodeKeyValue()
        return when (val value = keyValue.value) {
            // converting to Char from a parsed Long number and checking bounds for the Char (MIN-MAX range)
            is TomlLong -> validateAndConvertInteger(value.content as Long, keyValue.lineNo, CHAR) { Char(it.toInt()) }
            // converting to Char from a parsed Literal String (with single quotes: '')
            is TomlLiteralString ->
                try {
                    (value.content as String).convertSpecialCharacters(keyValue.lineNo).single()
                } catch (ex: NoSuchElementException) {
                    throw IllegalTypeException(
                        "Empty value is not allowed for type [Char], " +
                                "please check the value: [${value.content}] or use [String] type for deserialization of " +
                                "[${keyValue.key}] instead", keyValue.lineNo
                    )
                } catch (ex: IllegalArgumentException) {
                    throw IllegalTypeException(
                        "[Char] type should be used for decoding of single character, but " +
                                "received multiple characters instead: [${value.content}]. " +
                                "If you really want to decode multiple chars, use [String] instead.", keyValue.lineNo
                    )
                }
            // to avoid confusion, we prohibit basic strings with double quotes for decoding to a Char type
            is TomlBasicString -> throw IllegalTypeException(
                "Double quotes were used in the input for deserialization " +
                        "of [Char]. Use [String] type or single quotes ('') instead for: [${value.content}]",
                keyValue.lineNo
            )
            // all other toml tree types are not supported
            else -> throw IllegalTypeException(
                "Cannot decode the key [${keyValue.key.last()}] with the value [${keyValue.value.content}]" +
                        " and with the provided type [Char]. Please check the type" +
                        " in your Serializable class or it's nullability",
                keyValue.lineNo
            )
        }
    }

    // Valid Toml types that should be properly decoded
    override fun decodeBoolean(): Boolean = decodePrimitiveType()
    override fun decodeLong(): Long = decodePrimitiveType()
    override fun decodeDouble(): Double = decodePrimitiveType()
    override fun decodeString(): String = decodePrimitiveType()

    protected fun DeserializationStrategy<*>.isDateTime(): Boolean =
        descriptor == instantSerializer.descriptor ||
                descriptor == localDateTimeSerializer.descriptor ||
                descriptor == localDateSerializer.descriptor ||
                descriptor == localTimeSerializer.descriptor

    // Cases for date-time types
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        when (deserializer.descriptor) {
            instantSerializer.descriptor -> decodePrimitiveType<Instant>() as T
            localDateTimeSerializer.descriptor -> decodePrimitiveType<LocalDateTime>() as T
            localDateSerializer.descriptor -> decodePrimitiveType<LocalDate>() as T
            localTimeSerializer.descriptor -> decodePrimitiveType<LocalTime>() as T
            else -> super.decodeSerializableValue(deserializer)
        }

    internal abstract fun decodeKeyValue(): TomlKeyValue

    /**
     * This is just an adapter from `kotlinx.serialization` to match the content with a type from a Toml Tree,
     * that we have parsed to a type that is described in user's code. For example:
     * >>> input: a = "5"
     * >>> stored in Toml Tree: TomlString("5")
     * >>> expected by user: data class A(val a: Int)
     * >>> TomlString cannot be cast to Int, user made a mistake -> IllegalTypeException
     */
    private inline fun <reified T> decodePrimitiveType(): T {
        val keyValue = decodeKeyValue()
        try {
            return when (val value = keyValue.value) {
                is TomlLong -> decodeInteger(value.content as Long, keyValue.lineNo)
                is TomlDouble -> decodeFloatingPoint(value.content as Double, keyValue.lineNo)
                else -> keyValue.value.content as T
            }
        } catch (e: ClassCastException) {
            throw IllegalTypeException(
                "Cannot decode the key [${keyValue.key.last()}] with the value [${keyValue.value.content}]" +
                        " and with the provided type [${T::class}]. Please check the type in your Serializable class or it's nullability",
                keyValue.lineNo
            )
        }
    }

    private inline fun <reified T> decodeFloatingPoint(content: Double, lineNo: Int): T =
        when (T::class) {
            Float::class -> validateAndConvertFloatingPoint(
                content,
                lineNo,
                FLOAT
            ) { num: Double -> num.toFloat() as T }

            Double::class -> validateAndConvertFloatingPoint(content, lineNo, DOUBLE) { num: Double -> num as T }
            else -> invalidType(T::class.toString(), "Signed Type")
        }

    /**
     * ktoml parser treats all integer literals as Long and all floating-point literals as Double,
     * so here we should be checking that there is no overflow with smaller types like Byte, Short and Int.
     */
    private inline fun <reified T> validateAndConvertFloatingPoint(
        content: Double,
        lineNo: Int,
        limits: FloatingPointLimitsEnum,
        conversion: (Double) -> T,
    ): T = when {
        content.isInfinite() || content.isNaN() -> conversion(content)
        content in limits.min..limits.max -> conversion(content)
        else -> throw IllegalTypeException(
            "The floating point literal, that you have provided is <$content>, " +
                    "but the type for deserialization is <${T::class}>. You will get an overflow, " +
                    "so we advise you to check the data or use other type for deserialization (Long, for example)",
            lineNo
        )
    }

    /**
     * After a lot of discussions (https://github.com/akuleshov7/ktoml/pull/153#discussion_r1003114861 and
     * https://github.com/akuleshov7/ktoml/issues/163), we have finally decided to allow to use Integer types and not only Long.
     * This method does simple validation of integer values to avoid overflow. For example, you really want to use byte,
     * we will check here, that your byte value does not exceed 127 and so on.
     */
    private inline fun <reified T> decodeInteger(content: Long, lineNo: Int): T =
        when (T::class) {
            Byte::class -> validateAndConvertInteger(content, lineNo, BYTE) { it.toByte() as T }
            Short::class -> validateAndConvertInteger(content, lineNo, SHORT) { it.toShort() as T }
            Int::class -> validateAndConvertInteger(content, lineNo, INT) { it.toInt() as T }
            Long::class -> validateAndConvertInteger(content, lineNo, LONG) { it as T }
            Double::class, Float::class -> throw IllegalTypeException(
                "Expected floating-point number, but received integer literal: <$content>. " +
                        "Deserialized floating-point number should have a dot: <$content.0>",
                lineNo
            )

            else -> invalidType(T::class.toString(), "Signed Type")
        }

    /**
     * ktoml parser treats all integer literals as Long and all floating-point literals as Double,
     * so here we should be checking that there is no overflow with smaller types like Byte, Short and Int.
     */
    private inline fun <reified T> validateAndConvertInteger(
        content: Long,
        lineNo: Int,
        limits: IntegerLimitsEnum,
        conversion: (Long) -> T,
    ): T = if (content in limits.min..limits.max) {
        conversion(content)
    } else {
        throw IllegalTypeException(
            "The integer literal, that you have provided is <$content>, " +
                    "but the type for deserialization is <${T::class}>. You will get an overflow, " +
                    "so we advise you to check the data or use other type for deserialization (Long, for example)",
            lineNo
        )
    }

    private fun invalidType(typeName: String, requiredType: String): Nothing {
        val keyValue = decodeKeyValue()
        throw IllegalTypeException(
            "<$typeName> type is not allowed by toml specification," +
                    " use <$requiredType> instead" +
                    " (key = ${keyValue.key.last()}; value = ${keyValue.value.content})", keyValue.lineNo
        )
    }
}
