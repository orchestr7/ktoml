package com.akuleshov7.ktoml.decoders.tables

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.decoders.ReadMeExampleTest
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InlineTableDecoderTest {
    @Serializable
    data class GradleExample(val plugins: ListOfInlines)

    @Serializable
    data class ListOfInlines(
        @SerialName("kotlin-jvm")
        val kotlinJvm: Plugin,

        @SerialName("kotlin-multiplatform")
        val kotlinMultiplatform: Plugin,

        @SerialName("kotlin-plugin-serialization")
        val kotlinPLuginSerialization: Plugin,
    )

    @Serializable
    data class Plugin(val id: String, val version: Version)

    @Serializable
    data class Version(val ref: String)

    @Test
    fun decodeDottedNestedInlineTable() {
        @Serializable
        data class NestedTableWithContent(
            val name: String,
            @SerialName("configurationList")
            val overriddenName: List<String?> = listOf(),
        )

        @Serializable
        data class MyTable(
            @SerialName("akuleshov7.com")
            val inlineTable: NestedTableWithContent,
        )

        @Serializable
        data class NestedTable(
            val table: MyTable,
            val i: Int,
        )

        @Serializable
        data class MyClass(
            val table: NestedTable,
        )

        val toml1 =
            """
            |table = { i = 1, table."akuleshov7.com" = { name = 'this is a "literal" string', configurationList = ["a",  "b",  "c", null   ]}}
            |
            """.trimMargin()
        val toml2 = """
            [table]
                i = 1
            [table.table."akuleshov7.com"]
                name = 'this is a "literal" string'
                configurationList = ["a",  "b",  "c", null   ]
        """.trimIndent()

        assertEquals(
            MyClass(
                table = NestedTable(
                    table = MyTable(
                        NestedTableWithContent("this is a \"literal\" string", listOf("a", "b", "c", null))
                    ),
                    i = 1,
                ),
            ),
            Toml.decodeFromString<MyClass>(toml1),
        )

        assertEquals(
            Toml.decodeFromString<MyClass>(toml1),
            Toml.decodeFromString<MyClass>(toml2),
        )
    }

    @Test
    fun decodeInlineTable() {
        val test =
            """
            |someBooleanProperty = true
            |
            |table1 = { property1 = null, property2 = 6 }
            |table2 = { someNumber = 5, "akuleshov7.com" = { name = 'this is a "literal" string', configurationList = ["a",  "b",  "c", null]   }   , charFromInteger = 123  }
            |table2 = { otherNumber = 5.56, charFromString = 'a' }
            |gradle-libs-like-property = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            |
            |[myMap]
            |   a = "b"
            |   c = "d"
            """.trimMargin()

        Toml.decodeFromString<ReadMeExampleTest.MyClass>(test)
    }

    @Test
    fun arrayInInlineTable() {
        @Serializable
        data class TableWithArray(val arr: List<Int>)

        @Serializable
        data class TableWithArrayWrapper(val table: TableWithArray)

        val test =
            """
            |table = { arr = [1, 2, 3,  ]   }
            |
            """.trimMargin()

        val result = Toml.decodeFromString<TableWithArrayWrapper>(test)
        assertEquals(
            TableWithArrayWrapper(
                table = TableWithArray(arr = listOf(1, 2, 3))
            ),
            result
        )
    }

    @Test
    fun trailingCommaIsPermitted() {
        // TOML 1.1 permits a single trailing comma after the last key/value pair
        val withComma = "point = { x = 1, y = 2, }"
        val withoutComma = "point = { x = 1, y = 2 }"

        assertEquals(Position(Point(1, 2)), Toml.decodeFromString<Position>(withComma))
        assertEquals(
            Toml.decodeFromString<Position>(withoutComma),
            Toml.decodeFromString<Position>(withComma),
        )
    }

    @Test
    fun loneCommaIsStillRejected() {
        assertFailsWith<ParseException> { Toml.decodeFromString<Position>("point = {,}") }
    }

    @Test
    fun gradleLibsToml() {
        val test =
            """
                |[plugins]
                |kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                |kotlin-multiplatform = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
                |kotlin-plugin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
            """.trimMargin()

        val decoded = Toml.decodeFromString<GradleExample>(test)

        assertEquals(
            GradleExample(
                ListOfInlines(
                    Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                    Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                    Plugin("org.jetbrains.kotlin.plugin.serialization", Version("kotlin"))
                )
            ),
            decoded
        )
    }

    @Serializable
    data class Point(val x: Int? = null, val y: Int? = null)

    @Serializable
    data class Position(val point: Point)

    @Serializable
    data class PositionWrapper(
        val id: Int,
        val position: Position,
        val description: String
    )

    @Test
    fun testEmptyInlineTable() {
        val test1 = """
            point = {  }
        """.trimIndent()
        val test2 = """
            [point] 
        """.trimIndent()

        val result1 = Toml.decodeFromString<Position>(test1)
        val result2 = Toml.decodeFromString<Position>(test2)
        assertEquals(result2, result1)
    }

    @Test
    fun testNestedEmptyInlineTable() {
        val test = """
            id = 15
            description = "abc"

            [position]
                point = {}
        """.trimIndent()

        Toml.decodeFromString<PositionWrapper>(test)
    }

    @Test
    fun decodeMultilineInlineTable() {
        // TOML 1.1: newlines between pairs plus a trailing comma (the multi-line config use case)
        val multiline = """
            |point = {
            |  x = 1,
            |  y = 2,
            |}
            |
        """.trimMargin()
        val singleLine = "point = { x = 1, y = 2 }"

        assertEquals(Position(Point(1, 2)), Toml.decodeFromString<Position>(multiline))
        assertEquals(
            Toml.decodeFromString<Position>(singleLine),
            Toml.decodeFromString<Position>(multiline),
        )
    }

    @Test
    fun decodeMultilineInlineTableWithComments() {
        val test = """
            |point = { # opening
            |  x = 1, # the x value
            |  y = 2, # the y value
            |} # closing
        """.trimMargin()

        assertEquals(Position(Point(1, 2)), Toml.decodeFromString<Position>(test))
    }

    @Test
    fun decodeEmptyMultilineInlineTable() {
        val test = """
            |point = {
            |}
        """.trimMargin()

        assertEquals(Position(Point()), Toml.decodeFromString<Position>(test))
    }

    @Test
    fun decodeMultilineInlineTableWithMultilineArray() {
        @Serializable
        data class TableWithArray(val arr: List<Int>)

        @Serializable
        data class TableWithArrayWrapper(val table: TableWithArray)

        val test = """
            |table = {
            |  arr = [
            |    1,
            |    2,
            |    3,
            |  ],
            |}
        """.trimMargin()

        assertEquals(
            TableWithArrayWrapper(TableWithArray(listOf(1, 2, 3))),
            Toml.decodeFromString<TableWithArrayWrapper>(test),
        )
    }

    @Test
    fun decodeNestedMultilineInlineTable() {
        @Serializable
        data class Inner(val k: Int)

        @Serializable
        data class Outer(val tbl: Inner)

        @Serializable
        data class Root(val outer: Outer)

        val test = """
            |outer = {
            |  tbl = {
            |    k = 1,
            |  },
            |}
        """.trimMargin()

        assertEquals(Root(Outer(Inner(1))), Toml.decodeFromString<Root>(test))
    }
}