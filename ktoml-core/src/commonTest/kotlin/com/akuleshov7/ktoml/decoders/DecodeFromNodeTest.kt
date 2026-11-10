package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.decodeFromTomlNode
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

@ExperimentalSerializationApi
class DecodeFromNodeTest {
    @Serializable
    private data class Foobar(val foo: String, val bar: String)

    /**
     * The orchestr7/ktoml#280 use case, solved spec-faithfully: parse the document ONCE, then decode
     * the already-parsed tables into typed objects keyed by their full dotted path. ktoml is not asked
     * to invent a non-standard "flat path-keyed map"; the caller composes it over the public AST, with
     * no re-parsing per table.
     */
    @Test
    fun parseOnceThenDecodeEachTableByPath() {
        //language=toml
        val input = """
            [a]
            foo = "foo"
            bar = "bar"

            [b.a]
            foo = "ba-foo"
            bar = "ba-bar"

            [b.b]
            foo = "bb-foo"
            bar = "bb-bar"

            [x.y.z]
            foo = "xyz-foo"
            bar = "xyz-bar"
        """.trimIndent()

        val file = Toml.tomlParser.parseString(input)
        val byPath = file.getRealTomlTables().associate { table ->
            table.fullTableKey.toString() to Toml.decodeFromTomlNode<Foobar>(table)
        }

        assertEquals(
            mapOf(
                "a" to Foobar("foo", "bar"),
                "b.a" to Foobar("ba-foo", "ba-bar"),
                "b.b" to Foobar("bb-foo", "bb-bar"),
                "x.y.z" to Foobar("xyz-foo", "xyz-bar"),
            ),
            byPath,
        )
    }

    @Test
    fun decodeSinglePickedTable() {
        //language=toml
        val input = """
            [server]
            foo = "host"
            bar = "8080"

            [client]
            foo = "name"
            bar = "token"
        """.trimIndent()

        val file = Toml.tomlParser.parseString(input)
        val server = file.findTableInAstByName("server")!!

        // explicit deserializer form
        assertEquals(
            Foobar("host", "8080"),
            Toml.decodeFromTomlNode(serializer<Foobar>(), server),
        )
        // reified convenience form
        assertEquals(
            Foobar("host", "8080"),
            Toml.decodeFromTomlNode<Foobar>(server),
        )
    }

    @Test
    fun decodingANodeDoesNotMutateTheTreeForLaterDecodes() {
        //language=toml
        val input = """
            [t]
            foo = "f"
            bar = "b"
        """.trimIndent()

        val file = Toml.tomlParser.parseString(input)
        val table = file.findTableInAstByName("t")!!

        // decoding the same node repeatedly must keep yielding the same result (no re-parenting /
        // draining of the original children)
        val first = Toml.decodeFromTomlNode<Foobar>(table)
        val second = Toml.decodeFromTomlNode<Foobar>(table)
        assertEquals(first, second)
        assertEquals(Foobar("f", "b"), first)
    }

    @Test
    fun decodeWholeFileNode() {
        @Serializable
        data class Config(val a: Foobar, val b: Foobar)

        //language=toml
        val input = """
            [a]
            foo = "af"
            bar = "ab"

            [b]
            foo = "bf"
            bar = "bb"
        """.trimIndent()

        val file = Toml.tomlParser.parseString(input)

        assertEquals(
            Config(Foobar("af", "ab"), Foobar("bf", "bb")),
            Toml.decodeFromTomlNode<Config>(file),
        )
    }
}
