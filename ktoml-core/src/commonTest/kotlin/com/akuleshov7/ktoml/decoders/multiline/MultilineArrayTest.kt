package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.decoders.shouldDecodeInto
import com.akuleshov7.ktoml.decoders.shouldFailAtLine
import com.akuleshov7.ktoml.decoders.shouldThrowExceptionWhileDecoding
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlin.test.Test

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
        """
            a = [
                "hey",
                "hi"
            ]
        """.shouldDecodeInto(expectedResult)

        """
            a = ["hey",
                "hi"]
        """.shouldDecodeInto(expectedResult)

        """
            a = [
                        "hey",
                "hi"            ]
        """.shouldDecodeInto(expectedResult)

        """
            a = ["hey",
                "hi"]
        """.shouldDecodeInto(expectedResult)

        """
            a = ["hey", "hi"
            ]
        """.shouldDecodeInto(expectedResult)

        """
            a = ["hey", "hi", "hello"
            ]
        """.shouldDecodeInto(SimpleStringArray(listOf("hey", "hi", "hello")))

        """
            a = ["hey", "hi"
            ,"hello"
            ]
        """.shouldDecodeInto(SimpleStringArray(listOf("hey", "hi", "hello")))

        """
            a = [
            ]
        """.shouldDecodeInto(SimpleStringArray(listOf()))

        """
            a = [
                "hey=hey",
                "hi=]"
            ]
        """.shouldDecodeInto(SimpleStringArray(listOf("hey=hey", "hi=]")))

        """
            a = [
                "hey=hey",
                "hi=]"
            ]
        """.shouldDecodeInto(SimpleStringArray(listOf("hey=hey", "hi=]")))
    }

    @Test
    fun testMultilineLongArrays() {
        """
            field = [
                1,
                2,
                3
            ]
        """.shouldDecodeInto(ClassWithImmutableList(listOf(1, 2, 3)))

        """
            field = [
                1,
                null,
                3
            ]
        """.shouldDecodeInto(ClassWithImmutableList(listOf(1, null, 3)))
    }

    @Test
    fun testMultilineArraysWithComments() {
        """
            a = [
                "hey",
                "hi" #123
            ]
        """.shouldDecodeInto(SimpleStringArray(listOf("hey", "hi")))

        """
            #123
            #123
            a = [#123
                "hey",#123
                "hi" # 123
            ]#123
            #123
        """.shouldDecodeInto(SimpleStringArray(listOf("hey", "hi")))

        """
            a = [#123
                "hey#abc",
                "hi#def" # 123
            ]#123
            #123
        """.shouldDecodeInto(SimpleStringArray(listOf("hey#abc", "hi#def")))
    }

    @Test
    fun testIncorrectMultilineArray() {
        """
            field = [
                1,
                2,
                3
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
            a = 123
            b = "abc"
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
            a = 123
            ]
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
            [
            a = 123
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
            a = [
                1, 2
            ]
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
            [foo]
            a = 123
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)

        """
            field = [
                1,
                2,
                3
                
            a = ']'
        """.shouldThrowExceptionWhileDecoding<SimpleStringArray, ParseException>()
            .shouldFailAtLine(2)
    }

    @Test
    fun testArrayWithTrailingComma() {
        """
            a = [
                1,
                2,
                3,
            ]
        """.shouldDecodeInto(SimpleArrayWithNullableValues(listOf(1, 2, 3)))
    }
}