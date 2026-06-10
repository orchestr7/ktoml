package com.akuleshov7.ktoml.decoders.tables

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for https://github.com/gildor/ktoml/issues/31.
 *
 * When repeated `[[a]]` array-of-tables sections each contain an inline array of tables
 * (`children = [{ ... }]`), the children of the second and later `[[a]]` elements were wrongly
 * attached to the **first** element: an inline array of tables expands to a `[[a.children]]`
 * fragment, and the tree builder re-used the first element's `children` bucket instead of
 * creating a fresh one under the current element.
 *
 * The equivalent pure form (`[[kids]]` + `[[kids.children]]`) already decoded correctly; this
 * locks in that the inline form behaves the same.
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
