package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlDecoder
import com.akuleshov7.ktoml.TomlElement
import com.akuleshov7.ktoml.TomlElementKind
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlInlineTable
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValue
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.modules.SerializersModule

/**
 * @param rootNode
 * @param config
 * @property serializersModule
 * @property toml active TOML format
 */
@ExperimentalSerializationApi
@Suppress("UNCHECKED_CAST")
@InternalKtomlApi
public class TomlArrayDecoder internal constructor(
    private val rootNode: TomlKeyValueArray,
    private val config: TomlInputConfig,
    override val serializersModule: SerializersModule,
    final override val toml: Toml,
) : TomlAbstractDecoder(), TomlDecoder {
    private var nextElementIndex = 0
    private val list = rootNode.value.content as List<*>
    private lateinit var currentElementDecoder: TomlAbstractDecoder
    private lateinit var currentElement: TomlElement
    private lateinit var currentPrimitiveElementOfArray: TomlValue

    public constructor(
        rootNode: TomlKeyValueArray,
        config: TomlInputConfig,
        serializersModule: SerializersModule,
    ) : this(
        rootNode,
        config,
        serializersModule,
        Toml(inputConfig = config, serializersModule = serializersModule),
    )

    private fun haveStartedReadingElements() = nextElementIndex > 0

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = list.size

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (nextElementIndex == list.size) {
            return CompositeDecoder.DECODE_DONE
        }

        when (val element = list[nextElementIndex]) {
            is TomlInlineTable -> setTableDecoder(element)
            is TomlValue -> {
                currentPrimitiveElementOfArray = element
                if (element is TomlArray) {
                    setArrayDecoder()
                } else {
                    setPrimitiveDecoder()
                }
            }
            else -> error("Unsupported TOML array element: $element")
        }
        return nextElementIndex++
    }

    private fun setTableDecoder(element: TomlInlineTable) {
        val table = element.returnTable(TomlFile(), TomlFile(), !config.allowTableRedefinition)
        val file = TomlFile().also { it.children.addAll(table.children) }
        currentElement = TomlElement(table)
        currentElementDecoder = TomlMainDecoder(
            rootNode = file,
            config = config,
            serializersModule = serializersModule,
            toml = toml,
            exposedRootNode = table,
        )
    }

    private fun setArrayDecoder() {
        val arrayNode = TomlKeyValueArray(
            rootNode.key,
            currentPrimitiveElementOfArray,
            rootNode.lineNo,
            comments = emptyList(),
            inlineComment = "",
        )
        currentElementDecoder = TomlArrayDecoder(
            arrayNode,
            config,
            serializersModule,
            toml,
        )
        currentElement = TomlElement(arrayNode)
    }

    private fun setPrimitiveDecoder() {
        val primitiveNode = TomlKeyValuePrimitive(
            rootNode.key,
            currentPrimitiveElementOfArray,
            rootNode.lineNo,
            comments = emptyList(),
            inlineComment = "",
        )
        val primitiveRoot = TomlFile().also { root ->
            root.appendChild(primitiveNode)
        }
        currentElementDecoder = TomlMainDecoder(
            rootNode = primitiveRoot,
            config = config,
            serializersModule = serializersModule,
            toml = toml,
            exposedRootNode = primitiveNode,
        )
        currentElement = TomlElement(primitiveNode)
    }

    override fun decodeTomlElement(): TomlElement =
        if (haveStartedReadingElements()) currentElement else TomlElement(rootNode)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        if (haveStartedReadingElements()) {
            if (currentElement.kind == TomlElementKind.TABLE) {
                return if (descriptor.kind == StructureKind.MAP) {
                    TomlMapDecoder(currentElement.node as TomlTable, config, serializersModule = serializersModule, toml = toml)
                } else {
                    currentElementDecoder.beginStructure(descriptor)
                }
            }
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
        super<TomlAbstractDecoder>.decodeSerializableValue(deserializer)
    }

    // The array itself is non-null; once iteration starts, inspect the selected element.
    override fun decodeNotNullMark(): Boolean =
        !haveStartedReadingElements() || currentElementDecoder.decodeNotNullMark()

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
            config: TomlInputConfig = TomlInputConfig(),
        ): T = decode(deserializer, tomlKeyValueArray, config, Toml(inputConfig = config))

        internal fun <T> decode(
            deserializer: DeserializationStrategy<T>,
            tomlKeyValueArray: TomlKeyValueArray,
            config: TomlInputConfig,
            toml: Toml,
        ): T {
            val decoder = TomlArrayDecoder(tomlKeyValueArray, config, toml.serializersModule, toml)
            return decoder.decodeSerializableValue(deserializer)
        }
    }
}
