package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.CastException
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Serializable
data class SimpleArray(val a: List<Long>)

@Serializable
data class SimpleStringArray(val a: List<String>)

@Serializable
data class NestedArray(val a: List<List<Long>>)

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

        assertFailsWith<CastException> { Toml.decodeFromString<ClassWithMutableList>("field = [null]").field }

        val testWithOnlyNullInArray: ClassWithImmutableList = Toml.decodeFromString("field = [null ]")
        assertEquals(listOf(null), testWithOnlyNullInArray.field)

        val testWithNullInArray: ClassWithImmutableList = Toml.decodeFromString("field = [null, 1 ]")
        assertEquals(listOf<Long?>(null, 1), testWithNullInArray.field)
    }

    @Test
    fun testMultilineStringArrays() {
        val expectedResult = SimpleStringArray(listOf("hey", "hi"))
        var test =
            """
                a = [
                    "hey",
                    "hi"
                ]
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                a = ["hey",
                    "hi"]
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                a = [
                            "hey",
                    "hi"            ]
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                a = ["hey",
                    "hi"]
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                a = ["hey", "hi"
                ]
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                a = ["hey", "hi", "hello"
                ]
            """
        assertEquals(SimpleStringArray(listOf("hey", "hi", "hello")), Toml.decodeFromString(test))

        test =
            """
                a = ["hey", "hi"
                ,"hello"
                ]
            """
        assertEquals(SimpleStringArray(listOf("hey", "hi", "hello")), Toml.decodeFromString(test))

        test =
            """
                a = [
                ]
            """
        assertEquals(SimpleStringArray(listOf()), Toml.decodeFromString(test))

        test =
            """
                a = [
                    "hey=hey",
                    "hi=]"
                ]
            """
        assertEquals(SimpleStringArray(listOf("hey=hey", "hi=]")), Toml.decodeFromString(test))

        test =
            """
                a = [
                    "hey=hey",
                    "hi=]"
                ]
            """
        assertEquals(SimpleStringArray(listOf("hey=hey", "hi=]")), Toml.decodeFromString(test))
    }

    @Test
    fun testMultilineLongArrays() {
        var testWithMultilineArray: ClassWithImmutableList = Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3
                ]
                """
        )
        assertEquals(listOf<Long?>(1, 2, 3), testWithMultilineArray.field)

        testWithMultilineArray = Toml.decodeFromString(
            """
                field = [
                    1,
                    null,
                    3
                ]
                """
        )
        assertEquals(listOf<Long?>(1, null, 3), testWithMultilineArray.field)
    }

    @Test
    fun testMultilineArraysWithComments() {
        val expectedResult = SimpleStringArray(listOf("hey", "hi"))
        var test =
            """
                a = [
                    "hey",
                    "hi" #123
                ]
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                #123
                #123
                a = [#123
                    "hey",#123
                    "hi" # 123
                ]#123
                #123
            """
        assertEquals(expectedResult, Toml.decodeFromString(test))

        test =
            """
                a = [#123
                    "hey#abc",
                    "hi#def" # 123
                ]#123
                #123
            """
        assertEquals(SimpleStringArray(listOf("hey#abc", "hi#def")), Toml.decodeFromString(test))
    }

    @Test
    fun testIncorrectMultilineArray() {
        var exception = assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3
                """
        ) }
        assertTrue(exception.message!!.contains("Line 5"))

        exception = assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                """
        ) }
        assertTrue(exception.message!!.contains("Line 5"))

        exception = assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                a = 123
                b = "abc"
                """
        ) }
        assertTrue(exception.message!!.contains("Line 7"))

        assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                a = 123
                ]
                """
        ) }
        assertTrue(exception.message!!.contains("Line 7"))

        assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                [
                a = 123
                """
        ) }
        assertTrue(exception.message!!.contains("Line 7"))

        assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                a = [
                    1, 2
                ]
                """
        ) }
        assertTrue(exception.message!!.contains("Line 7"))

        assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                [foo]
                a = 123
                """
        ) }
        assertTrue(exception.message!!.contains("Line 7"))

        assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                a = ']'
                """
        ) }
        assertTrue(exception.message!!.contains("Line 7"))
    }

    @Test
    fun testSimpleArrayDecoder() {
        val test = "a = [1, 2,      3]"
        assertEquals(SimpleArray(listOf(1, 2, 3)), Toml.decodeFromString(test))
    }

    @Test
    fun testSimpleArrayDecoderInNestedTable() {
        var test = """
            |[table]
            |name = "my name"
            |configurationList = ["a",  "b",  "c"]
            """.trimMargin()

        assertEquals(
            ArrayInInlineTable(
                table = InlineTable(name = "my name", overriddenName = listOf("a", "b", "c"))
            ), Toml.decodeFromString(test)
        )

        test =
            """
            |[table]
            |configurationList = ["a",  "b",  "c"]
            |name = "my name"
            """.trimMargin()

        assertEquals(
            ArrayInInlineTable(
                table = InlineTable(name = "my name", overriddenName = listOf("a", "b", "c"))
            ), Toml.decodeFromString(test)
        )

        val testTable = """
            |configurationList1 = ["a",  "b",  "c"]
            |configurationList2 = ["a",  "b",  "c"]
            |[table]
            |name = "my name"
            """.trimMargin()

        assertEquals(
            TestArrays(
                overriddenName1 = listOf("a", "b", "c"),
                overriddenName2 = listOf("a", "b", "c"),
                table = Table(name = "my name")
            ), Toml.decodeFromString(testTable)
        )

        val testTableAndVariables = """
            |name1 = "simple"
            |configurationList1 = ["a",  "b",  "c"]
            |name2 = "simple"
            |configurationList2 = ["a",  "b",  "c"]
            |[table]
            |name = "my name"
            """.trimMargin()


        assertEquals(
            TestArraysAndSimple(
                name1 = "simple", overriddenName1 = listOf("a", "b", "c"),
                overriddenName2 = listOf("a", "b", "c"), table = Table(name = "my name"), name2 = "simple"
            ), Toml.decodeFromString(testTableAndVariables)
        )
    }

    @Test
    @Ignore
    fun testNestedArrayDecoder() {
        // FixMe: nested array decoding causes issues and is not supported yet
        val test = "a = [[1, 2],      [3,  4]]"
        assertEquals(NestedArray(listOf(listOf(1, 2), listOf(3, 4))), Toml.decodeFromString(test))
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
}
