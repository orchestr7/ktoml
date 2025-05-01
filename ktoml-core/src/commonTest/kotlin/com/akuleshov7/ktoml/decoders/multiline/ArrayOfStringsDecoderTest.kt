package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.decoders.structures.NestedArrayOfStrings
import com.akuleshov7.ktoml.decoders.structures.SimpleStringArray
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayOfStringsDecoderTest {
    private val tripleQuotes = "\"\"\""
    private val tripleSingleQuotes = "'''"

    @Test
    fun testPositiveCase() {
        var test = """
            a = [
            '''
                hey
            ''',
                
            '''
                hi
            '''
            ]
        """.trimIndent()
        assertEquals(
            SimpleStringArray(listOf("    hey\n", "    hi\n")),
            Toml.decodeFromString(test),
        )

        test = """
            a = ['''
                hey
            ''','''
                hi
            '''
            ]
        """.trimIndent()
        assertEquals(
            SimpleStringArray(listOf("    hey\n", "    hi\n")),
            Toml.decodeFromString(test),
        )

        test = "a = [ $tripleQuotes\n    hey\n$tripleQuotes,  $tripleQuotes\n    hi\n$tripleQuotes]"
        assertEquals(
            SimpleStringArray(listOf("    hey\n", "    hi\n")),
            Toml.decodeFromString(test),
        )
    }

    @Test
    fun testIgnoreClosingSymbolInsideString() {
        var test = """
            a = ['''
                    Index: [0]
                '''
            ]
        """.trimIndent()
        assertEquals(
            SimpleStringArray(listOf("        Index: [0]\n")),
            Toml.decodeFromString(test),
        )

        test = """
            a = [
                '''
                    Index: [0]
                '''
            ]
        """.trimIndent()
        assertEquals(
            SimpleStringArray(listOf("        Index: [0]\n")),
            Toml.decodeFromString(test),
        )

        test = "a = [  $tripleQuotes\n        Index: [0] \n$tripleQuotes\n]"
        assertEquals(
            SimpleStringArray(listOf("        Index: [0] \n")),
            Toml.decodeFromString(test),
        )
    }

    @Test
    fun testMoreThanOneValuesInArray() {
        val test = """
            a = [
                ["1", "2"],
                ["3", "4"]
            ]
        """.trimIndent()

        assertEquals(
            NestedArrayOfStrings(
                listOf(
                    listOf("1", "2"),
                    listOf("3", "4")
                )
            ),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun testWithMultipleBrackets() {
        val expected = NestedArrayOfStrings(
            listOf(
                listOf("]123", "[]123"),
                listOf("[asd]", "[asd]")
            )
        )
        var test = """
            a = [["]123", "[]123"],
                ["[asd]", "[asd]"]]
        """.trimIndent()

        assertEquals(expected, Toml.decodeFromString(test))

        test = """
            a = [
                # comment
                ["]123", "[]123"],# comment
                # comment
                ["[asd]", "[asd]"# comment
                ]

            ]
        """.trimIndent()
        assertEquals(expected, Toml.decodeFromString(test))
    }

    @Test
    fun testQuotesInsideOtherQuotes() {
        var test = """
            a = [
                $tripleQuotes ''' $tripleQuotes
            ]
        """.trimIndent()

        assertEquals(
            SimpleStringArray(listOf(" ''' ")),
            Toml.decodeFromString(test),
        )

        test = """
            a = [
                $tripleSingleQuotes " $tripleSingleQuotes
            ]
        """.trimIndent()

        assertEquals(
            SimpleStringArray(listOf(" \" ")),
            Toml.decodeFromString(test),
        )
    }
}
