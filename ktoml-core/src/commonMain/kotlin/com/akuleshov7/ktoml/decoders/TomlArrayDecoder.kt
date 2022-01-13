package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.parsers.node.TomlKeyValue
import com.akuleshov7.ktoml.parsers.node.TomlKeyValueArray
import com.akuleshov7.ktoml.parsers.node.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.parsers.node.TomlNull
import com.akuleshov7.ktoml.parsers.node.TomlValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * @property rootNode
 * @property config
 */
@ExperimentalSerializationApi
@Suppress("UNCHECKED_CAST")
public class TomlArrayDecoder(
    private val rootNode: TomlKeyValueArray,
    private val config: TomlConfig,
) : TomlAbstractDecoder() {
    private var nextElementIndex = 0
    private val list = rootNode.value.content as List<TomlValue>
    override val serializersModule: SerializersModule = EmptySerializersModule
    private lateinit var currentElementDecoder: TomlPrimitiveDecoder
    private lateinit var currentPrimitiveElementOfArray: TomlValue

    private fun haveStartedReadingElements() = nextElementIndex > 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = list.size

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (nextElementIndex == list.size) {
            return CompositeDecoder.DECODE_DONE
        }

        currentPrimitiveElementOfArray = list[nextElementIndex]

        currentElementDecoder = TomlPrimitiveDecoder(
            // a small hack that creates a PrimitiveKeyValue node that is used in the decoder
            TomlKeyValuePrimitive(
                rootNode.key,
                currentPrimitiveElementOfArray,
                rootNode.lineNo,
                rootNode.key.content,
                config
            )
        )
        return nextElementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (haveStartedReadingElements()) {
            return currentElementDecoder
        }
        return super.beginStructure(descriptor)
    }

    override fun decodeKeyValue(): TomlKeyValue = throw NotImplementedError("Method `decodeKeyValue`" +
            " should never be called for TomlListDecoder, it should use ")

    override fun decodeString(): String = currentElementDecoder.decodeString()
    override fun decodeInt(): Int = currentElementDecoder.decodeInt()
    override fun decodeLong(): Long = currentElementDecoder.decodeLong()
    override fun decodeShort(): Short = currentElementDecoder.decodeShort()
    override fun decodeByte(): Byte = currentElementDecoder.decodeByte()
    override fun decodeDouble(): Double = currentElementDecoder.decodeDouble()
    override fun decodeFloat(): Float = currentElementDecoder.decodeFloat()
    override fun decodeBoolean(): Boolean = currentElementDecoder.decodeBoolean()
    override fun decodeChar(): Char = currentElementDecoder.decodeChar()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = currentElementDecoder.decodeEnum(enumDescriptor)

    // this should be applied to [currentPrimitiveElementOfArray] and not to the [rootNode], because
    override fun decodeNotNullMark(): Boolean = currentPrimitiveElementOfArray !is TomlNull
}
