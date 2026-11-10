package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBoolean
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlDouble
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLong
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Mixed-type ("of Different Types") arrays — orchestr7/ktoml#317.
 *
 * A TOML array may legally hold heterogeneous values, e.g. `x = [1, "a", true]`. ktoml **parses** such
 * arrays without complaint: every element keeps its own type in the AST. The historical ❌ in the README
 * was misleading — the gap is not parsing, it is the kotlinx.serialization **type mapping**: a `List<E>`
 * has exactly one element deserializer `E`, so `[1, "a"]` cannot map onto `List<Int>` (the `"a"` has no
 * `Int` to decode into). That is a property of `List<E>`, not a ktoml bug, and ktoml deliberately ships
 * no `Any` serializer (that is kotlinx.serialization-core territory).
 *
 * There are two consumer-side ways to actually get the heterogeneous values out; this file demonstrates
 * both with runnable samples:
 *
 *  1. **Read the parsed AST directly** ([heterogeneousScalarArrayKeepsPerElementTypes],
 *     [nestedMixedArrayKeepsStructure], [convertMixedArrayToKotlinValues]). Every element is a typed
 *     [TomlValue]; you branch on its subtype. Flexible, no custom serializer, handles *arbitrary* mixes.
 *     Note: the AST types are `@InternalKtomlApi`, so an external caller must
 *     `@OptIn(InternalKtomlApi::class)` (this module opts in globally, so the test below needs no marker).
 *
 *  2. **A custom `KSerializer` that dispatches on a discriminator carried in the data**
 *     ([decodeTaggedHeterogeneousArrayWithCustomSerializer]). This is the decoder counterpart to
 *     `PolymorphicEncoderTest`: each element is a `[tag, value]` pair and the serializer reads `tag`
 *     first, then decodes `value` as the matching type. Dispatch works because the discriminator is a
 *     value *in the document*, decoded normally.
 *
 * What does NOT work today is a purely consumer-side contextual `Any` serializer that inspects the
 * *underlying TOML type* of an untagged element (`[1, "a", true]`) to dispatch: the array decoder hands a
 * custom serializer a decoder whose current element [TomlValue] is private and whose `decodeValue()` is
 * not exposed, so there is nothing to peek. That clean declarative form needs a ktoml-side hook (a
 * contextual `TomlNode` serializer / `decodeTomlElement()`, tracked with orchestr7/ktoml#97) — until
 * then, untagged mixes go through path 1 (the AST).
 */
@OptIn(ExperimentalSerializationApi::class)
class MixedTypeArrayDecodeTest {
    // ---------------------------------------------------------------------------------------------
    // Path 1: read the already-parsed AST directly.
    // ---------------------------------------------------------------------------------------------

    /** Parse [input] once and return the elements of the top-level array stored under [key]. */
    private fun topLevelArrayElements(input: String, key: String): List<TomlValue> {
        val file = Toml.tomlParser.parseString(input)
        val arrayNode = file.children.filterIsInstance<TomlKeyValueArray>().single { it.name == key }
        @Suppress("UNCHECKED_CAST")
        return (arrayNode.value as TomlArray).content as List<TomlValue>
    }

    @Test
    fun heterogeneousScalarArrayKeepsPerElementTypes() {
        val elements = topLevelArrayElements("""mixed = [1, "two", true, 3.5]""", "mixed")

        assertIs<TomlLong>(elements[0])
        assertIs<TomlBasicString>(elements[1])
        assertIs<TomlBoolean>(elements[2])
        assertIs<TomlDouble>(elements[3])

        // each TomlValue.content holds the already-typed Kotlin value
        assertEquals(listOf<Any>(1L, "two", true, 3.5), elements.map { it.content })
    }

    @Test
    fun nestedMixedArrayKeepsStructure() {
        // a = ["test", ["test"]] used to throw StackOverflowError upstream (orchestr7/ktoml#328);
        // it is fixed on toml-1.1, so a nested mix parses into a nested TomlArray here.
        val elements = topLevelArrayElements("""nested = ["a", [1, 2], true]""", "nested")

        assertIs<TomlBasicString>(elements[0])
        assertIs<TomlArray>(elements[1])
        assertIs<TomlBoolean>(elements[2])

        @Suppress("UNCHECKED_CAST")
        val inner = (elements[1] as TomlArray).content as List<TomlValue>
        assertEquals(listOf<Any>(1L, 2L), inner.map { it.content })
    }

    /**
     * The reusable shape a consumer would actually write: fold the typed AST into plain Kotlin values
     * (`Long`/`String`/`Boolean`/`Double`/`List`), recursing into nested arrays.
     */
    private fun TomlValue.toKotlin(): Any = when (this) {
        is TomlArray -> {
            @Suppress("UNCHECKED_CAST")
            (content as List<TomlValue>).map { it.toKotlin() }
        }
        else -> content
    }

    @Test
    fun convertMixedArrayToKotlinValues() {
        val elements = topLevelArrayElements("""mixed = [1, "two", true, [3.5, false]]""", "mixed")

        assertEquals(
            listOf(1L, "two", true, listOf(3.5, false)),
            elements.map { it.toKotlin() },
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Path 2: a custom KSerializer that dispatches on a `[tag, value]` discriminator in the data.
    // ---------------------------------------------------------------------------------------------

    @Serializable(with = TaggedValue.Serializer::class)
    private sealed interface TaggedValue {
        data class IntValue(val value: Int) : TaggedValue
        data class StringValue(val value: String) : TaggedValue
        data class BoolValue(val value: Boolean) : TaggedValue
        data class IntListValue(val value: List<Int>) : TaggedValue

        /**
         * Reads an element shaped as `[tag, value]`, e.g. `["int", 42]`. The first element is the
         * discriminator; once decoded we know which concrete type the second element is — including a
         * nested array payload (`["ints", [1, 2, 3]]`).
         */
        object Serializer : KSerializer<TaggedValue> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("TaggedValue") {
                element<String>("tag")
                element<String>("value")
            }

            override fun deserialize(decoder: Decoder): TaggedValue = decoder.decodeStructure(descriptor) {
                var tag: String? = null
                var result: TaggedValue? = null
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> tag = decodeStringElement(descriptor, 0)
                        1 -> result = when (tag) {
                            "int" -> IntValue(decodeIntElement(descriptor, 1))
                            "str" -> StringValue(decodeStringElement(descriptor, 1))
                            "bool" -> BoolValue(decodeBooleanElement(descriptor, 1))
                            "ints" -> IntListValue(
                                decodeSerializableElement(descriptor, 1, ListSerializer(Int.serializer())),
                            )
                            else -> error("unknown tag: $tag")
                        }
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("unexpected index: $index")
                    }
                }
                result ?: error("missing value for tag: $tag")
            }

            override fun serialize(encoder: Encoder, value: TaggedValue): Unit =
                throw NotImplementedError("encoding is out of scope for this decode example")
        }
    }

    @Test
    fun decodeTaggedHeterogeneousArrayWithCustomSerializer() {
        @Serializable
        data class Holder(val items: List<TaggedValue>)

        //language=toml
        val input = """
            items = [ ["int", 42], ["str", "hello"], ["bool", true] ]
        """.trimIndent()

        assertEquals(
            Holder(
                listOf(
                    TaggedValue.IntValue(42),
                    TaggedValue.StringValue("hello"),
                    TaggedValue.BoolValue(true),
                ),
            ),
            Toml.decodeFromString<Holder>(input),
        )
    }

    @Test
    fun decodeNestedMixedArrayWithCustomSerializer() {
        @Serializable
        data class Holder(val items: List<TaggedValue>)

        //language=toml
        val input = """
            items = [ ["int", 1], ["ints", [2, 3, 4]], ["str", "x"] ]
        """.trimIndent()

        assertEquals(
            Holder(
                listOf(
                    TaggedValue.IntValue(1),
                    TaggedValue.IntListValue(listOf(2, 3, 4)),
                    TaggedValue.StringValue("x"),
                ),
            ),
            Toml.decodeFromString<Holder>(input),
        )
    }
}
