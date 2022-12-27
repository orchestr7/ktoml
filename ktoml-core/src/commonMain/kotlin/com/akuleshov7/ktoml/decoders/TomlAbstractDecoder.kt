package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.CastException
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLong
import com.akuleshov7.ktoml.utils.IntegerLimitsEnum
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

    // Invalid Toml primitive types, but we anyway support them with some limitations
    override fun decodeByte(): Byte = decodePrimitiveType()
    override fun decodeShort(): Short = decodePrimitiveType()
    override fun decodeInt(): Int = decodePrimitiveType()
    override fun decodeFloat(): Float = decodePrimitiveType()
    override fun decodeChar(): Char = decodePrimitiveType()

    // Valid Toml types that should be properly decoded
    override fun decodeBoolean(): Boolean = decodePrimitiveType()
    override fun decodeLong(): Long = decodePrimitiveType()
    override fun decodeDouble(): Double = decodePrimitiveType()
    override fun decodeString(): String = decodePrimitiveType()

    protected fun DeserializationStrategy<*>.isDateTime(): Boolean =
            descriptor == instantSerializer.descriptor ||
                    descriptor == localDateTimeSerializer.descriptor ||
                    descriptor == localDateSerializer.descriptor

    // Cases for date-time types
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when (deserializer.descriptor) {
        instantSerializer.descriptor -> decodePrimitiveType<Instant>() as T
        localDateTimeSerializer.descriptor -> decodePrimitiveType<LocalDateTime>() as T
        localDateSerializer.descriptor -> decodePrimitiveType<LocalDate>() as T
        else -> super.decodeSerializableValue(deserializer)
    }

    internal abstract fun decodeKeyValue(): TomlKeyValue

    /**
     * This is just an adapter from `kotlinx.serialization` to match the content with a type from a Toml Tree,
     * that we have parsed to a type that is described in user's code. For example:
     * >>> input: a = "5"
     * >>> stored in Toml Tree: TomlString("5")
     * >>> expected by user: data class A(val a: Int)
     * >>> TomlString cannot be cast to Int, user made a mistake -> CastException
     */
    private inline fun <reified T> decodePrimitiveType(): T {
        val keyValue = decodeKeyValue()
        try {
            return when (val value = keyValue.value) {
                is TomlLong -> decodeInteger(value.content as Long, keyValue.lineNo)
                else -> keyValue.value.content as T
            }
        } catch (e: ClassCastException) {
            throw CastException(
                "Cannot decode the key [${keyValue.key.content}] with the value [${keyValue.value.content}]" +
                        " with the provided type [${T::class}]. Please check the type in your Serializable class or it's nullability",
                keyValue.lineNo
            )
        }
    }

    /**
     * After a lot of discussions, we have finally decided to allow to use Integer types and not only Long.
     * This method does simple validation of integer values to avoid overflow. For example, you really want to use byte,
     * we will check here, that your byte value does not exceed 127 and so on.
     */
    private inline fun <reified T> decodeInteger(content: Long, lineNo: Int): T {
        return when (T::class) {
            Byte::class -> validateAndConvertInt(content, lineNo, IntegerLimitsEnum.BYTE) { c: Long -> c.toByte() as T }
            Short::class -> validateAndConvertInt(content, lineNo, IntegerLimitsEnum.SHORT) { c: Long -> c.toShort() as T  }
            Int::class -> validateAndConvertInt(content, lineNo, IntegerLimitsEnum.INT) { c: Long -> c.toInt() as T  }
            Long::class -> validateAndConvertInt(content, lineNo, IntegerLimitsEnum.LONG) { c: Long -> c as T  }
            else -> invalidType(T::class.toString(), "Signed Type")
        }
    }

    private inline fun <reified T> validateAndConvertInt(
        content: Long,
        lineNo: Int,
        limits: IntegerLimitsEnum,
        conversion: (Long) -> T,
    ): T {
        return if (content in limits.min..limits.max) {
            conversion(content)
        } else {
            throw IllegalTypeException("The integer literal that you have provided is <$content>, but the type that you" +
                    "expect deserialization to be made to is <${T::class}>. You will get an overflow: check the data or " +
                    "use proper type for deserialization", lineNo)
        }
    }

    private fun invalidType(typeName: String, requiredType: String): Nothing {
        val keyValue = decodeKeyValue()
        throw IllegalTypeException(
            "<$typeName> type is not allowed by toml specification," +
                    " use <$requiredType> instead" +
                    " (key = ${keyValue.key.content}; value = ${keyValue.value.content})", keyValue.lineNo
        )
    }
}
