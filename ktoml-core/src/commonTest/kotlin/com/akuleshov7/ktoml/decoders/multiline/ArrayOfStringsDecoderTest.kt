package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.decoders.structures.NestedArrayOfStrings
import com.akuleshov7.ktoml.decoders.structures.SimpleStringArray
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    @Test
    fun testMultilineLiteralInArrayWithApostropheAndHash() {
        val test = "a = ['''\nrandom quote ' is here\n# this is not a comment\n''']"

        assertEquals(
            SimpleStringArray(listOf("random quote ' is here\n# this is not a comment\n")),
            Toml.decodeFromString(test),
        )
    }

    @Test
    fun testQuoteRunsAtEndOfMultilineStrings() {
        val literal = "a = [''''That,' she said, 'is still pointless.'''', 'ok']"
        val basic = "a = [$tripleQuotes" + "a \\\"\"\" b" + tripleQuotes + ", \"ok\"]"

        assertEquals(
            SimpleStringArray(listOf("'That,' she said, 'is still pointless.'", "ok")),
            Toml.decodeFromString(literal),
        )
        assertEquals(
            SimpleStringArray(listOf("a \"\"\" b", "ok")),
            Toml.decodeFromString(basic),
        )
    }

    @Test
    fun testEscapedQuoteInLiteralArrayElement() {
        val test = "a = ['it\\'s, still one value', 'ok']"

        assertEquals(
            SimpleStringArray(listOf("it's, still one value", "ok")),
            Toml.decodeFromString(test),
        )
        assertFailsWith<ParseException> {
            Toml(
                TomlInputConfig(allowEscapedQuotesInLiteralStrings = false)
            ).decodeFromString<SimpleStringArray>(test)
        }
    }

    @Test
    fun testEscapedBackslashBeforeClosingQuote() {
        // https://github.com/orchestr7/ktoml/issues/380#issuecomment-4835523816
        val test = """key = ["\\"]"""

        listOf(TomlInputConfig(), TomlInputConfig.compliant()).forEach { config ->
            assertEquals(
                mapOf("key" to listOf("\\")),
                Toml(config).decodeFromString<Map<String, List<String>>>(test),
            )
        }
    }

    @Test
    fun testEvenBackslashRunsBeforeClosingQuote() {
        listOf(2, 4, 6, 8).forEach { backslashCount ->
            val test = """a = ["${"\\".repeat(backslashCount)}", "next"]"""

            listOf(TomlInputConfig(), TomlInputConfig.compliant()).forEach { config ->
                assertEquals(
                    SimpleStringArray(listOf("\\".repeat(backslashCount / 2), "next")),
                    Toml(config).decodeFromString<SimpleStringArray>(test),
                    "Backslash count: $backslashCount",
                )
            }
        }
    }

    @Test
    fun testOddBackslashRunsBeforeEscapedQuote() {
        listOf(1, 3, 5, 7).forEach { backslashCount ->
            val test = """a = ["${"\\".repeat(backslashCount)}"still, one string", "next"]"""

            listOf(TomlInputConfig(), TomlInputConfig.compliant()).forEach { config ->
                assertEquals(
                    SimpleStringArray(listOf("\\".repeat(backslashCount / 2) + "\"still, one string", "next")),
                    Toml(config).decodeFromString<SimpleStringArray>(test),
                    "Backslash count: $backslashCount",
                )
            }
        }
    }

    @Test
    fun testOddBackslashRunsDoNotCloseString() {
        listOf(1, 3, 5, 7).forEach { backslashCount ->
            val test = """a = ["${"\\".repeat(backslashCount)}"]"""

            listOf(TomlInputConfig(), TomlInputConfig.compliant()).forEach { config ->
                assertFailsWith<ParseException>("Backslash count: $backslashCount") {
                    Toml(config).decodeFromString<SimpleStringArray>(test)
                }
            }
        }
    }
}
