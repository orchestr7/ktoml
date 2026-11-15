package com.akuleshov7.ktoml.decoders.tables

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression guard for https://github.com/gildor/ktoml/issues/31.
 *
 * When repeated `[[a]]` array-of-tables sections each contain an inline array of tables
 * (`children = [{ ... }]`), the children of the second and later `[[a]]` elements must stay
 * under their own element rather than being attached to the first element.
 *
 * On `main` this required threading `latestCreatedBucket` through the inline-table insertion
 * (orchestr7/ktoml#386). On `toml-1.1` the rewritten tree builder already handles it: an inline
 * array of tables expands to an `[[a.children]]` fragment, and `insertTableToTree` resolves the
 * `[[a]]` parent to its **last** element (`resolveInsertionParent`) before searching for the
 * `children` fragment, so a fresh one is created under the current element. These tests lock that
 * behaviour in so a future refactor cannot silently reintroduce the bug.
 */
class ArrayOfTablesWrongParentTest {
    @Serializable
    data class Kid(val name: String)

    @Serializable
    data class Parent(val name: String, val children: List<Kid>? = null)

    @Serializable
    data class Root(val name: String, val kids: List<Parent>)

    @Test
    fun eachArrayOfTablesElementKeepsItsOwnInlineChildren() {
        val toml = """
            name = "g"
            [[kids]]
            name = "p1"
            children = [{ name = "a" }]
            [[kids]]
            name = "p2"
            children = [{ name = "b" }]
        """.trimIndent()

        assertEquals(
            Root(
                name = "g",
                kids = listOf(
                    Parent("p1", listOf(Kid("a"))),
                    Parent("p2", listOf(Kid("b"))),
                ),
            ),
            Toml.decodeFromString<Root>(toml),
        )
    }

    @Test
    fun multipleChildrenAcrossMultipleParents() {
        val toml = """
            name = "g"
            [[kids]]
            name = "p1"
            children = [{ name = "a" }, { name = "b" }]
            [[kids]]
            name = "p2"
            children = [{ name = "c" }, { name = "d" }]
            [[kids]]
            name = "p3"
            children = [{ name = "e" }]
        """.trimIndent()

        assertEquals(
            Root(
                name = "g",
                kids = listOf(
                    Parent("p1", listOf(Kid("a"), Kid("b"))),
                    Parent("p2", listOf(Kid("c"), Kid("d"))),
                    Parent("p3", listOf(Kid("e"))),
                ),
            ),
            Toml.decodeFromString<Root>(toml),
        )
    }

    @Test
    fun inlineChildrenMatchPureNestedArrayOfTables() {
        val inlineForm = """
            name = "g"
            [[kids]]
            name = "p1"
            children = [{ name = "a" }]
            [[kids]]
            name = "p2"
            children = [{ name = "b" }]
        """.trimIndent()

        val pureForm = """
            name = "g"
            [[kids]]
            name = "p1"
            [[kids.children]]
            name = "a"
            [[kids]]
            name = "p2"
            [[kids.children]]
            name = "b"
        """.trimIndent()

        assertEquals(
            Toml.decodeFromString<Root>(pureForm),
            Toml.decodeFromString<Root>(inlineForm),
        )
    }
}
