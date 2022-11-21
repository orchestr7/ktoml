package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.CastException
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
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

    // Invalid Toml primitive types, we will simply throw an error for them
    override fun decodeByte(): Byte   {
        val result = decodeLong()
        if (result !in (Byte.MIN_VALUE.toLong()..Byte.MAX_VALUE.toLong()))
            throw ParseException("$result is not a Byte", 0)
        return result.toByte()
    }

    override fun decodeShort(): Short  {
        val result = decodeLong()
        if (result !in (Short.MIN_VALUE.toLong()..Short.MAX_VALUE.toLong()))
            throw ParseException("$result is not a Short", 0)
        return result.toShort()
    }
    override fun decodeInt(): Int    {
        val result = decodeLong()
        if (result !in (Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()))
            throw ParseException("not an Int", 0)
        return result.toInt()
    }
    override fun decodeFloat(): Float   {
        val result = decodeDouble()
        if (result !in (Float.MIN_VALUE.toDouble()..Float.MAX_VALUE.toDouble()))
            throw ParseException("$result is not a Float", 0)
        return result.toFloat()
    }
    override fun decodeChar(): Char  {
        val result = decodeLong()
        if (result !in (Char.MIN_VALUE.toLong()..Char.MAX_VALUE.toLong()))
            throw ParseException("$result is not a Char", 0)
        return result.toChar()
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
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when (deserializer.descriptor) {
        instantSerializer.descriptor -> decodePrimitiveType<Instant>() as T
        localDateTimeSerializer.descriptor -> decodePrimitiveType<LocalDateTime>() as T
        localDateSerializer.descriptor -> decodePrimitiveType<LocalDate>() as T
        else -> super.decodeSerializableValue(deserializer)
    }

    internal abstract fun decodeKeyValue(): TomlKeyValue

    private fun invalidType(typeName: String, requiredType: String): Nothing {
        val keyValue = decodeKeyValue()
        throw IllegalTypeException(
            "<$typeName> type is not allowed by toml specification," +
                    " use <$requiredType> instead" +
                    " (key = ${keyValue.key.content}; value = ${keyValue.value.content})", keyValue.lineNo
        )
    }

    private inline fun <reified T> decodePrimitiveType(
        convertValue: (Any) -> T =  { it as T }
    ): T {
        val keyValue = decodeKeyValue()
        try {
            return convertValue(keyValue.value.content)
        } catch (e: ClassCastException) {
            throw CastException(
                "Cannot decode the key [${keyValue.key.content}] with the value [${keyValue.value.content}]" +
                        " with the provided type [${T::class}]. Please check the type in your Serializable class or it's nullability",
                keyValue.lineNo
            )
        }
    }
}
