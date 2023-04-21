package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MultilineArrayTest {

    @Serializable
    data class SimpleStringArray(val a: List<String>)

    @Serializable
    data class SimpleArrayWithNullableValues(val a: List<Long?>)

    @Serializable
    data class ClassWithImmutableList(val field: List<Long?>? = null)

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
        assertTrue(exception.message!!.contains("Line 2"))

        exception = assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                """
        ) }
        assertTrue(exception.message!!.contains("Line 2"))

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
        assertTrue(exception.message!!.contains("Line 2"))

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
        assertTrue(exception.message!!.contains("Line 2"))

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
        assertTrue(exception.message!!.contains("Line 2"))

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
        assertTrue(exception.message!!.contains("Line 2"))

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
        assertTrue(exception.message!!.contains("Line 2"))

        assertFailsWith<ParseException> { Toml.decodeFromString(
            """
                field = [
                    1,
                    2,
                    3

                a = ']'
                """
        ) }
        assertTrue(exception.message!!.contains("Line 2"))
    }

    @Test
    fun testArrayWithTrailingComma() {
        val test = """
            a = [
                1,
                2,
                3,
            ]
            """.trimIndent()
        assertEquals(SimpleArrayWithNullableValues(listOf(1, 2, 3)), Toml.decodeFromString(test))
    }
}