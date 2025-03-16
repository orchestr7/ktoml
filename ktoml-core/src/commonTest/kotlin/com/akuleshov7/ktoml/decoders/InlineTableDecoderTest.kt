package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.*

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
    fun decodeSimpleInlineTable() {
        @Serializable
        data class NestedTable(
            val name: String,
            @SerialName("configurationList")
            val overriddenName: List<String?> = listOf()
        )

        @Serializable
        data class Table2(
            @SerialName("akuleshov7.com")
            val inlineTable: NestedTable,
        )

        @Serializable
        data class MyClass(
            val table2: Table2,
        )

        val test2 =
            """
            |table2 = { table2."akuleshov7.com" = { name = 'this is a "literal" string', configurationList = ["a",  "b",  "c", null] }}
            |
            """.trimMargin()

        assertEquals(
            MyClass(
                Table2(
                    NestedTable("this is a \"literal\" string", listOf("a", "b", "c", null))
                )
            ),
            Toml.decodeFromString<MyClass>(test2),
        )
    }

    @Test
    fun decodeInlineTable() {
        val test =
            """
            |someBooleanProperty = true
            |
            |table1 = { property1 = null, property2 = 6 }
            |table2 = { someNumber = 5, table2."akuleshov7.com" = { name = 'this is a "literal" string', configurationList = ["a",  "b",  "c", null] }}
            |table2 = { otherNumber = 5.56, charFromString = 'a', charFromInteger = 123 }
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
            |table = { arr = [1, 2, 3] }
            |
            """.trimMargin()

        val result = Toml.decodeFromString<TableWithArrayWrapper>(test)
        println(result)
    }

    @Test
    fun trailingCommaIsNotPermitted() {
        val test =
            """
            |inlineTable = { inlineValStr = "inline", inlineValInt = -1, }
            |       
            """.trimMargin()

        assertFailsWith<ParseException> { Toml.decodeFromString<ReadMeExampleTest.MyClass>(test) }
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
            GradleExample(ListOfInlines(
                Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                Plugin("org.jetbrains.kotlin.jvm", Version("kotlin")),
                Plugin("org.jetbrains.kotlin.plugin.serialization", Version("kotlin")))
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
}