package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml.Default.serializersModule
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlNull
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

/**
 * @param rootNode
 * @param config
 * @property serializersModule
 */
@ExperimentalSerializationApi
@Suppress("UNCHECKED_CAST")
public class TomlArrayDecoder(
    private val rootNode: TomlKeyValueArray,
    private val config: TomlInputConfig,
    override val serializersModule: SerializersModule,
) : TomlAbstractDecoder() {
    private var nextElementIndex = 0
    private val list = rootNode.value.content as List<TomlValue>
    private lateinit var currentElementDecoder: TomlAbstractDecoder
    private lateinit var currentPrimitiveElementOfArray: TomlValue

    private fun haveStartedReadingElements() = nextElementIndex > 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = list.size

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (nextElementIndex == list.size) {
            return CompositeDecoder.DECODE_DONE
        }

        currentPrimitiveElementOfArray = list[nextElementIndex]

        if (currentPrimitiveElementOfArray is TomlArray) {
            setArrayDecoder()
        } else {
            setPrimitiveDecoder()
        }
        return nextElementIndex++
    }

    private fun setArrayDecoder() {
        currentElementDecoder = TomlArrayDecoder(
            TomlKeyValueArray(
                rootNode.key,
                currentPrimitiveElementOfArray,
                rootNode.lineNo,
                comments = emptyList(),
                inlineComment = "",
            ),
            config,
            serializersModule,
        )
    }

    private fun setPrimitiveDecoder() {
        val primitiveRoot = TomlFile().also { root ->
            root.appendChild(
                TomlKeyValuePrimitive(
                    rootNode.key,
                    currentPrimitiveElementOfArray,
                    rootNode.lineNo,
                    comments = emptyList(),
                    inlineComment = "",
                )
            )
        }
        currentElementDecoder = TomlMainDecoder(
            rootNode = primitiveRoot,
            config = config,
            serializersModule = serializersModule,
        )
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (haveStartedReadingElements()) {
            return currentElementDecoder
        }
        return super.beginStructure(descriptor)
    }

    // decodeKeyValue is usually used for simple plain structures, but as it is present in TomlAbstractDecoder,
    // we should implement it and have this stub
    override fun decodeKeyValue(): TomlKeyValue = throw NotImplementedError("Method `decodeKeyValue`" +
            " should never be called for TomlArrayDecoder, because it is a more complex structure")

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
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = if (
        deserializer.isDateTime() || deserializer.isUnsigned()
    ) {
        currentElementDecoder.decodeSerializableValue(deserializer)
    } else {
        super.decodeSerializableValue(deserializer)
    }

    // this should be applied to [currentPrimitiveElementOfArray] and not to the [rootNode]
    override fun decodeNotNullMark(): Boolean = currentPrimitiveElementOfArray !is TomlNull

    public companion object {
        /**
         * @param deserializer - deserializer provided by Kotlin compiler
         * @param tomlKeyValueArray - TomlKeyValueArray node for decoding
         * @param config - decoding configuration for parsing and serialization
         * @return decoded (deserialized) object of type T
         */
        public fun <T> decode(
            deserializer: DeserializationStrategy<T>,
            tomlKeyValueArray: TomlKeyValueArray,
            config: TomlInputConfig = TomlInputConfig()
        ): T {
            val decoder = TomlArrayDecoder(tomlKeyValueArray, config, serializersModule)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
