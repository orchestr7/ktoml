package com.akuleshov7.ktoml.decoders.structures

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Serializable
data class SimpleArray(val a: List<Long>)

@Serializable
data class SimpleArrayWithNullableValues(val a: List<Long?>)

@Serializable
data class SimpleStringArray(val a: List<String>)

@Serializable
data class NestedArray(val a: List<List<Long>>)

@Serializable
data class NestedArrayOfStrings(val a: List<List<String>>)

@Serializable
data class ArrayInInlineTable(val table: InlineTable)

@Serializable
data class InlineTable(
    val name: String,
    @SerialName("configurationList")
    val overriddenName: List<String>
)

@Serializable
data class TestArrays(
    @SerialName("configurationList1")
    val overriddenName1: List<String>,
    @SerialName("configurationList2")
    val overriddenName2: List<String>,
    val table: Table
)

@Serializable
data class Table(val name: String)

@Serializable
data class TestArraysAndSimple(
    val name1: String,
    @SerialName("configurationList1")
    val overriddenName1: List<String>,
    @SerialName("configurationList2")
    val overriddenName2: List<String>,
    val table: Table,
    val name2: String
)

@Serializable
data class ClassWithMutableList (val field: MutableList<Long>? = null)

@Serializable
data class ClassWithImmutableList(val field: List<Long?>? = null)

class SimpleArrayDecoderTest {
    @Test
    fun testRegressions() {
        // ==== #77 ====
        val testClassWithMutableList: ClassWithMutableList = Toml.decodeFromString("field = []")
        assertEquals(mutableListOf(), testClassWithMutableList.field)

        val testClassWithImmutableList: ClassWithImmutableList = Toml.decodeFromString("field = []")
        assertEquals(emptyList(), testClassWithImmutableList.field)

        val testWithNullArray1: ClassWithImmutableList = Toml.decodeFromString("field = null")
        assertEquals(null, testWithNullArray1.field)

        val testWithNullArray2: ClassWithMutableList = Toml.decodeFromString("field = null")
        assertEquals(null, testWithNullArray2.field)

        assertFailsWith<IllegalTypeException> { Toml.decodeFromString<ClassWithMutableList>("field = [null]").field }

        val testWithOnlyNullInArray: ClassWithImmutableList = Toml.decodeFromString("field = [null ]")
        assertEquals(listOf(null), testWithOnlyNullInArray.field)

        val testWithNullInArray: ClassWithImmutableList = Toml.decodeFromString("field = [null, 1 ]")
        assertEquals(listOf<Long?>(null, 1), testWithNullInArray.field)
    }

    @Test
    fun testSimpleArrayDecoder() {
        val test = "a = [1, 2,      3]"
        assertEquals(SimpleArray(listOf(1, 2, 3)), Toml.decodeFromString(test))
    }

    @Test
    fun testArrayWithTrailingComma() {
        val test = "a = [1, 2, 3,]"
        assertEquals(SimpleArrayWithNullableValues(listOf(1, 2, 3)), Toml.decodeFromString(test))
    }

    @Test
    fun testSimpleArrayDecoderInNestedTable() {
        var test = """
            [table]
                name = "my name"
                configurationList = ["a",  "b",  "c"]
            """

        assertEquals(
            ArrayInInlineTable(
                table = InlineTable(name = "my name", overriddenName = listOf("a", "b", "c"))
            ), Toml.decodeFromString(test)
        )

        test =
            """
            [table]
                configurationList = ["a",  "b",  "c"]
                name = "my name"
            """

        assertEquals(
            ArrayInInlineTable(
                table = InlineTable(name = "my name", overriddenName = listOf("a", "b", "c"))
            ), Toml.decodeFromString(test)
        )

        val testTable = """
            configurationList1 = ["a",  "b",  "c"]
            configurationList2 = ["a",  "b",  "c"]
            [table]
                name = "my name"
            """

        assertEquals(
            TestArrays(
                overriddenName1 = listOf("a", "b", "c"),
                overriddenName2 = listOf("a", "b", "c"),
                table = Table(name = "my name")
            ), Toml.decodeFromString(testTable)
        )

        val testTableAndVariables = """
            name1 = "simple"
            configurationList1 = ["a",  "b",  "c"]
            name2 = "simple"
            configurationList2 = ["a",  "b",  "c"]
            [table]
                name = "my name"
            """


        assertEquals(
            TestArraysAndSimple(
                name1 = "simple", overriddenName1 = listOf("a", "b", "c"),
                overriddenName2 = listOf("a", "b", "c"), table = Table(name = "my name"), name2 = "simple"
            ), Toml.decodeFromString(testTableAndVariables)
        )
    }

    @Test
    fun testNestedArrayDecoder() {
        val test = "a = [[1, 2],      [3,  4]]"
        assertEquals(
            NestedArray(
                listOf(
                    listOf(1, 2),
                    listOf(3, 4)
                )
            ),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun testCommasInString() {
        val test = "a = [\"yes, indeed\", \"hmm, hmm\"]"
        assertEquals(SimpleStringArray(listOf("yes, indeed", "hmm, hmm")), Toml.decodeFromString(test))
        val testSingleQuote = "a = ['yes, indeed', 'hmm, hmm']"
        assertEquals(SimpleStringArray(listOf("yes, indeed", "hmm, hmm")), Toml.decodeFromString(testSingleQuote))
        val testWithInternalQuote = "a = [\"yes, \\\"indeed\\\"\", \"hmm, 'hmm'\"]"
        assertEquals(SimpleStringArray(listOf("yes, \"indeed\"", "hmm, 'hmm'")), Toml.decodeFromString(testWithInternalQuote))
        val testSingleWithInternalQuote = "a = ['yes, \"indeed\"', 'hmm, \"hmm\"']"
        assertEquals(SimpleStringArray(listOf("yes, \"indeed\"", "hmm, \"hmm\"")), Toml.decodeFromString(testSingleWithInternalQuote))
    }

    @Test
    fun shouldThrowIllegalTypeExceptionOnWrongArrayType() {
        @Serializable
        data class ArrayWrapper(
            val a: List<Int>,
        )
        val toml = """
            a = "abc"
        """.trimIndent()

        assertFailsWith<IllegalTypeException> {
            Toml.decodeFromString<ArrayWrapper>(toml)
        }
    }

    enum class Enum {
        A,
        B,
        C
    }

    @Serializable
    data class Enums(
        val enums: List<Enum>
    )

    @Test
    fun decodeEnumList() {
        val enums = Enums(listOf(Enum.A, Enum.B, Enum.C))
        val test = """
            enums = ["A", "B", "C"]
        """.trimIndent()

        val decoded = Toml.decodeFromString<Enums>(test)
        assertEquals(enums, decoded)
    }
}
