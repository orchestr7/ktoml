package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.decodeFromTomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Demonstrates how to bind a document that mixes **known** tables (typed) with an arbitrary set of
 * **unknown** tables (a catch-all) — the orchestr7/ktoml#97 use case — using only the already-shipped
 * [Toml.decodeFromTomlNode] API: parse once, traverse the public AST, decode the nodes you care about.
 *
 * ktoml does NOT invent a non-standard catch-all annotation; the caller composes the split itself.
 *
 * NOTE: [Toml.decodeFromTomlNode] is marked `@ExperimentalKtomlApi`. ktoml opts into its own
 * experimental APIs internally, so these tests need no explicit opt-in; an external consumer would add
 * `@OptIn(ExperimentalKtomlApi::class)`.
 */
@ExperimentalSerializationApi
class CatchAllTablesExampleTest {
    @Serializable
    private data class Known(val foo: String)

    @Serializable
    private data class Other(val thing: String, val other: String)

    private data class Config(
        val knownA: Known,
        val knownB: Known,
        val unknowns: Map<String, Other>,
    )

    //language=toml
    private val input = """
        [knownA]
        foo = "bar"

        [knownB]
        foo = "baz"

        [unknownA]
        thing = "something"
        other = "whatever"

        [unknownB]
        thing = "it's a thing"
        other = "stuff"
    """.trimIndent()

    @Test
    fun typedKnownTablesWithTypedCatchAll() {
        val file = Toml.tomlParser.parseString(input)
        val knownNames = setOf("knownA", "knownB")
        val tables = file.getRealTomlTables()

        val config = Config(
            knownA = Toml.decodeFromTomlNode(tables.single { it.name == "knownA" }),
            knownB = Toml.decodeFromTomlNode(tables.single { it.name == "knownB" }),
            unknowns = tables.filter { it.name !in knownNames }
                .associate { it.name to Toml.decodeFromTomlNode<Other>(it) },
        )

        assertEquals(
            Config(
                knownA = Known("bar"),
                knownB = Known("baz"),
                unknowns = mapOf(
                    "unknownA" to Other("something", "whatever"),
                    "unknownB" to Other("it's a thing", "stuff"),
                ),
            ),
            config,
        )
    }

    @Test
    fun keepUnknownTablesRawAndDecodeOnDemand() {
        val file = Toml.tomlParser.parseString(input)
        val knownNames = setOf("knownA", "knownB")

        // keep unmatched tables as raw AST nodes; decode them later, only if/when needed
        val unknowns: Map<String, TomlTable> = file.getRealTomlTables()
            .filter { it.name !in knownNames }
            .associate { it.name to it }

        assertEquals(setOf("unknownA", "unknownB"), unknowns.keys)
        assertEquals(
            Other("something", "whatever"),
            Toml.decodeFromTomlNode<Other>(unknowns.getValue("unknownA")),
        )
    }
}
