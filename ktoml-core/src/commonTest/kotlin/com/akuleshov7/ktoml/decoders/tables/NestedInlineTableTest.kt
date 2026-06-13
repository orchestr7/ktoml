package com.akuleshov7.ktoml.decoders.tables

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for crashes on deeply nested inline tables and on arrays of
 * inline tables that themselves contain nested arrays/tables.
 *
 * - https://github.com/orchestr7/ktoml/issues/360 (inline-table cases)
 * - https://github.com/gildor/ktoml/issues/29
 *
 * Before the fix, the inline-table splitters did not track bracket-nesting
 * depth, so depth 3+ inline tables computed a negative index and crashed in
 * `indexOfNextOutsideQuotes` (`drop(-1)`), and arrays of inline tables with a
 * nested array dropped a closing `]` and crashed with a
 * `StringIndexOutOfBoundsException`.
 */
class NestedInlineTableTest {
    @Serializable
    data class Depth3Inner(val a: Int)

    @Serializable
    data class Depth3Middle(val a: Depth3Inner)

    @Serializable
    data class Depth3Outer(val a: Depth3Middle)

    @Serializable
    data class Depth3Root(val x: Depth3Outer)

    @Test
    fun decodeInlineTableNestedThreeLevels() {
        // x = { a = { a = { a = 1 } } } — valid TOML 1.0, crashed before the fix
        val test = "x = { a = { a = { a = 1 } } }"

        assertEquals(
            Depth3Root(Depth3Outer(Depth3Middle(Depth3Inner(1)))),
            Toml.decodeFromString<Depth3Root>(test),
        )
    }

    @Serializable
    data class Depth4Inner(val a: Int)

    @Serializable
    data class Depth4L3(val a: Depth4Inner)

    @Serializable
    data class Depth4L2(val a: Depth4L3)

    @Serializable
    data class Depth4L1(val a: Depth4L2)

    @Serializable
    data class Depth4Root(val x: Depth4L1)

    @Test
    fun decodeInlineTableNestedFourLevels() {
        val test = "x = { a = { a = { a = { a = 1 } } } }"

        assertEquals(
            Depth4Root(Depth4L1(Depth4L2(Depth4L3(Depth4Inner(1))))),
            Toml.decodeFromString<Depth4Root>(test),
        )
    }

    @Test
    fun decodeInlineTableNestedWithSiblings() {
        // sibling keys around the deeply nested value must still split correctly
        @Serializable
        data class Inner(val a: Int)

        @Serializable
        data class Middle(val a: Inner, val b: Int)

        @Serializable
        data class Outer(val nested: Middle, val flag: Boolean)

        val test = "out = { nested = { a = { a = 1 }, b = 2 }, flag = true }"

        @Serializable
        data class Root(val out: Outer)

        assertEquals(
            Root(Outer(Middle(Inner(1), 2), true)),
            Toml.decodeFromString<Root>(test),
        )
    }

    @Serializable
    data class Child(val name: String)

    @Serializable
    data class Parent(val name: String, val children: List<Child>? = null)

    @Serializable
    data class Grandparent(val name: String, val kids: List<Parent>)

    @Test
    fun decodeArrayOfInlineTablesWithNestedArray() {
        // https://github.com/orchestr7/ktoml/issues/360 — crashed with
        // StringIndexOutOfBoundsException before the fix
        val test =
            """
            |name = "the-grandparent"
            |kids = [{ name = "number-one-parent", children = [{ name = "some-kid" }] }]
            """.trimMargin()

        assertEquals(
            Grandparent(
                name = "the-grandparent",
                kids = listOf(Parent("number-one-parent", listOf(Child("some-kid")))),
            ),
            Toml.decodeFromString<Grandparent>(test),
        )
    }

    @Test
    fun decodeArrayOfInlineTablesWithNestedArrayTwoElements() {
        // https://github.com/orchestr7/ktoml/issues/360 — second crashing case;
        // each parent must keep its own child, not have them merged together
        val test =
            """
            |name = "the-grandparent"
            |kids = [{ name = "number-one-parent", children = [{ name = "some-kid" }] }, { name = "number-two-parent", children = [{ name = "another-kid" }] }]
            """.trimMargin()

        assertEquals(
            Grandparent(
                name = "the-grandparent",
                kids = listOf(
                    Parent("number-one-parent", listOf(Child("some-kid"))),
                    Parent("number-two-parent", listOf(Child("another-kid"))),
                ),
            ),
            Toml.decodeFromString<Grandparent>(test),
        )
    }

    @Test
    fun decodeArrayOfInlineTablesMultipleChildrenPerParent() {
        val test =
            """
            |name = "the-grandparent"
            |kids = [{ name = "p1", children = [{ name = "a" }, { name = "b" }] }]
            """.trimMargin()

        assertEquals(
            Grandparent(
                name = "the-grandparent",
                kids = listOf(Parent("p1", listOf(Child("a"), Child("b")))),
            ),
            Toml.decodeFromString<Grandparent>(test),
        )
    }

    @Test
    fun decodeChildlessArrayOfInlineTablesStillWorks() {
        // these cases already worked before the fix; lock them in against regressions
        val nullChildren =
            """
            |name = "g"
            |kids = [{ name = "p1" }]
            """.trimMargin()
        val emptyChildren =
            """
            |name = "g"
            |kids = [{ name = "p1", children = [] }]
            """.trimMargin()

        assertEquals(
            Grandparent("g", listOf(Parent("p1", children = null))),
            Toml.decodeFromString<Grandparent>(nullChildren),
        )
        assertEquals(
            Grandparent("g", listOf(Parent("p1", children = emptyList()))),
            Toml.decodeFromString<Grandparent>(emptyChildren),
        )
    }

    @Serializable
    data class Lvl3(val name: String)

    @Serializable
    data class Lvl2(val name: String, val items: List<Lvl3>? = null)

    @Serializable
    data class Lvl1(val name: String, val items: List<Lvl2>? = null)

    @Serializable
    data class Lvl0(val name: String, val items: List<Lvl1>)

    @Test
    fun decodeArrayOfInlineTablesNestedThreeLevels() {
        val test =
            """
            |name = "root"
            |items = [{ name = "a", items = [{ name = "b", items = [{ name = "c" }] }] }]
            """.trimMargin()

        assertEquals(
            Lvl0("root", listOf(Lvl1("a", listOf(Lvl2("b", listOf(Lvl3("c"))))))),
            Toml.decodeFromString<Lvl0>(test),
        )
    }
}
