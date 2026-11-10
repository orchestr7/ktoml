package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import com.akuleshov7.ktoml.decoders.TomlArrayDecoder
import com.akuleshov7.ktoml.decoders.TomlArrayOfTablesDecoder
import com.akuleshov7.ktoml.decoders.TomlMainDecoder
import com.akuleshov7.ktoml.decoders.TomlMapDecoder
import com.akuleshov7.ktoml.encoders.TomlMainEncoder
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import com.akuleshov7.ktoml.utils.findPrimitiveTableInAstByName
import com.akuleshov7.ktoml.writers.TomlCallbackEmitter
import com.akuleshov7.ktoml.writers.TomlWriter

import kotlin.native.concurrent.ThreadLocal
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Toml class - is a general entry point in the core,
 * that is used to serialize/deserialize TOML file or string
 *
 * @property inputConfig - configuration for deserialization
 * @property outputConfig - configuration for serialization
 * @property serializersModule - default overridden
 */
@OptIn(ExperimentalSerializationApi::class)
@SubclassOptInRequired(InternalKtomlApi::class)
public open class Toml(
    @InternalKtomlApi
    protected val inputConfig: TomlInputConfig = TomlInputConfig(),
    @InternalKtomlApi
    protected val outputConfig: TomlOutputConfig = TomlOutputConfig(),
    override val serializersModule: SerializersModule = EmptySerializersModule(),
) : StringFormat {
    // parser and writer are created once after the creation of the class, to reduce
    // the number of created parsers and writers for each toml
    @InternalKtomlApi
    public val tomlParser: TomlParser = TomlParser(inputConfig)

    @InternalKtomlApi
    public val tomlWriter: TomlWriter = TomlWriter(outputConfig)

    // ================== basic overrides ===============

    /**
     * simple deserializer of a string in a toml format (separated by newlines)
     *
     * @param string - request-string in toml format with '\n' or '\r\n' separation
     * @return deserialized object of type T
     */
    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val parsedToml = tomlParser.parseString(string)
        return decode(deserializer, parsedToml, inputConfig)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val toml = TomlMainEncoder.encode(serializer, value, outputConfig, serializersModule)
        return tomlWriter.writeToString(file = toml)
    }

    /**
     * Encodes [value] through callbacks used by optional I/O adapter modules.
     *
     * This is public only because Kotlin module boundaries prevent the adapters from using an internal
     * declaration. Applications should use a supported `encodeTo*` API instead.
     *
     * @param serializer serialization strategy
     * @param value value to encode
     * @param emitString callback for a string fragment
     * @param emitChar callback for a character fragment
     */
    @InternalKtomlApi
    public fun <T> encodeToEmitter(
        serializer: SerializationStrategy<T>,
        value: T,
        emitString: (String) -> Unit,
        emitChar: (Char) -> Unit,
    ) {
        val toml = TomlMainEncoder.encode(serializer, value, outputConfig, serializersModule)
        val emitter = TomlCallbackEmitter(outputConfig, emitString, emitChar)
        tomlWriter.write(toml, emitter)
    }

    // ================== custom decoding methods ===============

    /**
     * simple deserializer of a sequence of strings in a toml format
     *
     * @param toml sequence with strings in toml format
     * @param deserializer deserialization strategy
     * @param config
     * @return deserialized object of type T
     */
    @InternalKtomlApi
    public fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: Sequence<String>,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val parsedToml = tomlParser.parseStringsToTomlTree(toml, config)
        return decode(deserializer, parsedToml, config)
    }

    /**
     * Parses [string] once and returns an opaque element that can be inspected or decoded repeatedly.
     *
     * @param string TOML document
     * @return parsed document handle
     */
    @ExperimentalKtomlApi
    public fun parseToTomlElement(string: String): TomlElement = TomlElement(tomlParser.parseString(string))

    /**
     * partial deserializer of a sequence of lines in a toml format.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param tomlLines sequence of TOML lines
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param config
     * @return deserialized object of type T
     */
    @InternalKtomlApi
    public fun <T> partiallyDecodeFromLines(
        deserializer: DeserializationStrategy<T>,
        tomlLines: Sequence<String>,
        tomlTableName: String,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(tomlLines, tomlTableName, config, TomlParser::parseLines)
        return TomlMainDecoder.decode(deserializer, fakeFileNode, config, tomlFor(config))
    }

    /**
     * partial deserializer of a string in a toml format (separated by newlines).
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param toml request-string in toml format with '\n' or '\r\n' separation
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param config
     * @return deserialized object of type T
     */
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        toml: String,
        tomlTableName: String,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(toml, tomlTableName, config, TomlParser::parseString)
        return TomlMainDecoder.decode(deserializer, fakeFileNode, config, tomlFor(config))
    }

    /**
     * partial deserializer of a sequence of lines in a toml format.
     * Will deserialize only the part presented under the tomlTableName table.
     * If such table is missing in he input - will throw an exception
     *
     * (!) Useful when you would like to deserialize only ONE table
     * and you do not want to reproduce whole object structure in the code
     *
     * @param deserializer deserialization strategy
     * @param tomlLines sequence of strings with toml input
     * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
     * @param config
     * @return deserialized object of type T
     */
    @InternalKtomlApi
    public fun <T> partiallyDecodeFromString(
        deserializer: DeserializationStrategy<T>,
        tomlLines: Sequence<String>,
        tomlTableName: String,
        config: TomlInputConfig = this.inputConfig
    ): T {
        val fakeFileNode = generateFakeTomlStructureForPartialParsing(
            tomlLines,
            tomlTableName,
            config,
            TomlParser::parseLines,
        )
        return TomlMainDecoder.decode(deserializer, fakeFileNode, config, tomlFor(config))
    }

    /**
     * Decodes an already-parsed [element] without parsing its source again.
     *
     * @param deserializer deserialization strategy
     * @param element parsed element to decode
     * @return decoded value
     */
    @ExperimentalKtomlApi
    public fun <T> decodeFromTomlElement(
        deserializer: DeserializationStrategy<T>,
        element: TomlElement,
    ): T = if (deserializer.descriptor.kind == StructureKind.LIST && element.isArrayOfTables()) {
        TomlArrayOfTablesDecoder(
            element.node as TomlTable,
            inputConfig,
            serializersModule,
            this,
        ).decodeSerializableValue(deserializer)
    } else {
        decode(deserializer, element.asFileNode(), inputConfig, element.node)
    }

    /**
     * Internal bridge retained for tests and decoder implementation code.
     *
     * @param deserializer deserialization strategy
     * @param node internal parser node to decode
     * @return decoded value
     */
    @ExperimentalKtomlApi
    @InternalKtomlApi
    public fun <T> decodeFromTomlNode(
        deserializer: DeserializationStrategy<T>,
        node: TomlNode,
    ): T = decode(deserializer, node.wrapIntoFileNode(), inputConfig)

    /**
     * Wraps an arbitrary node into a [TomlFile] so it can be fed to the existing decoders, which
     * expect a file root. A [TomlFile] is returned as-is; any other node's children are re-hosted
     * under a fresh file node. The children are added without re-parenting, so the caller's original
     * tree (and its `parent` links) is left intact and can keep being traversed and decoded.
     */
    private fun TomlNode.wrapIntoFileNode(): TomlFile = when (this) {
        is TomlFile -> this
        else -> TomlFile().also { it.children.addAll(children) }
    }

    private fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        parsedToml: TomlFile,
        config: TomlInputConfig,
        exposedRootNode: TomlNode = parsedToml,
    ): T {
        val decodingToml = tomlFor(config)
        return when (deserializer.descriptor.kind) {
            StructureKind.LIST -> TomlArrayDecoder.decode(
                deserializer,
                parsedToml.getFirstChild() as TomlKeyValueArray,
                config,
                decodingToml,
            )
            StructureKind.MAP -> TomlMapDecoder.decode(deserializer, parsedToml, config, decodingToml)
            else -> TomlMainDecoder.decode(deserializer, parsedToml, config, decodingToml, exposedRootNode)
        }
    }

    private fun tomlFor(config: TomlInputConfig): Toml =
        if (config == inputConfig) this else Toml(config, outputConfig, serializersModule)

    // ================== other ===============
    @Suppress("TYPE_ALIAS")
    private fun <I> generateFakeTomlStructureForPartialParsing(
        tomlInput: I,
        tomlTableName: String,
        config: TomlInputConfig = TomlInputConfig(),
        parsingFunction: (TomlParser, I) -> TomlFile
    ): TomlFile {
        val tomlFile = parsingFunction(TomlParser(config), tomlInput)
        val parsedToml = findPrimitiveTableInAstByName(listOf(tomlFile), tomlTableName)
            ?: throw MissingRequiredPropertyException(
                "Cannot find table with name <$tomlTableName> in the toml input. " +
                        " Are you sure that this table exists in the input?" +
                        " Not able to decode this toml part."
            )

        // adding a fake file node to restore the structure and parse only the part of te toml
        val fakeFileNode = TomlFile()
        parsedToml.children.forEach {
            fakeFileNode.appendChild(it)
        }

        return fakeFileNode
    }

    /**
     * The default instance of [Toml] with the default configuration.
     * See [TomlConfig] for the list of the default options
     * ThreadLocal annotation is used here for caching.
     */
    @ThreadLocal
    public companion object Default : Toml(
        inputConfig = TomlInputConfig(),
        outputConfig = TomlOutputConfig()
    )
}
