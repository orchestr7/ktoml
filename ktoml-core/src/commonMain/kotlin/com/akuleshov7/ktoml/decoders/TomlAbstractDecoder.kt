package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
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

    override fun decodeByte(): Byte = decodePrimitiveIntegerType { it.toByte() }

    override fun decodeShort(): Short = decodePrimitiveIntegerType { it.toShort() }

    override fun decodeInt(): Int = decodePrimitiveIntegerType { it.toInt() }

    override fun decodeFloat(): Float {
        val keyValue = decodeKeyValue()

        val doubleValue = keyValue.castOrThrow<Double>()

        val floatValue = doubleValue.toFloat()

        if (floatValue.isInfinite()) {
            // maybe make this exception configurable? That's what KxS JSON does
            throw ParseException(
                "unexpected number - expected ${Float::class}, but actual value '$doubleValue' was not valid",
                keyValue.lineNo
            )
        } else {
            return floatValue
        }
    }

    override fun decodeChar(): Char {
        val keyValue = decodeKeyValue()

        val stringValue = keyValue.castOrThrow<Char>()

//        if (stringValue.length != 1) {
//            throw ParseException(
//                "Expected single Char, but actual value was a String size:${stringValue.length}",
//                keyValue.lineNo
//            )
//        } else {
//            return stringValue.first()
//        }

        return stringValue
    }

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
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T =
        when (deserializer.descriptor) {
            instantSerializer.descriptor -> decodePrimitiveType<Instant>() as T
            localDateTimeSerializer.descriptor -> decodePrimitiveType<LocalDateTime>() as T
            localDateSerializer.descriptor -> decodePrimitiveType<LocalDate>() as T
            else -> super.decodeSerializableValue(deserializer)
        }

    internal abstract fun decodeKeyValue(): TomlKeyValue

    private inline fun <reified T> decodePrimitiveType(): T = decodeKeyValue().castOrThrow()

    /** convert from a big ol' [Long] to a little tiny integer type, throwing an exception if this is not possible */
    private inline fun <reified T> decodePrimitiveIntegerType(
        converter: (Long) -> T
    ): T where T : Number, T : Comparable<T> {
        val keyValue = decodeKeyValue()

        val longValue = keyValue.castOrThrow<Long>()

        val convertedValue = converter(longValue)

        println("converted $longValue to $convertedValue")

        if (longValue != convertedValue.toLong()) {
            throw ParseException(
                "unexpected number - expected ${T::class}, but actual value '$longValue' was out-of-bounds",
                keyValue.lineNo
            )
        } else {
            return convertedValue
        }
    }

    private inline fun <reified T> TomlKeyValue.castOrThrow(): T {
        return try {
            value.content as T
        } catch (e: ClassCastException) {
            throw ParseException(
                "Cannot decode the key [${key.content}] with the value [${value.content}]" +
                        " with the provided type [${T::class}]. Please check the type in your Serializable class or it's nullability",
                lineNo,
            )
        }
    }
}
