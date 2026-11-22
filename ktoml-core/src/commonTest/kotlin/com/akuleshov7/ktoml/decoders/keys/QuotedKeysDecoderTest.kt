package com.akuleshov7.ktoml.decoders.keys

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class QuotedKeysDecoderTest {

    @Test
    fun equalsAndBracesInQuotedKeysAreNotValues() {
        @Serializable
        data class QuotedKey(@SerialName("a={") val value: Int)

        for (quote in listOf('"', '\'')) {
            val input = "${quote}a={$quote = 1"
            assertEquals(QuotedKey(1), Toml.decodeFromString<QuotedKey>(input))
        }
    }

    @Test
    fun equalsAndBracesInQuotedTableHeadersAreNotValues() {
        for (quote in listOf('"', '\'')) {
            val input = "[${quote}a={$quote]\nx = 1"
            assertEquals(mapOf("a={" to mapOf("x" to 1)), Toml.decodeFromString<Map<String, Map<String, Int>>>(input))
        }
    }

    @Test
    fun equalsAndBracesInQuotedArrayTableHeadersAreNotValues() {
        @Serializable
        data class Row(val x: Int)

        @Serializable
        data class Document(@SerialName("a={") val rows: List<Row>)

        for (quote in listOf('"', '\'')) {
            val input = "[[${quote}a={$quote]]\nx = 1"
            assertEquals(Document(listOf(Row(1))), Toml.decodeFromString<Document>(input))
        }
    }

    @Test
    fun escapedQuoteBeforeEqualsIsPartOfTheKey() {
        @Serializable
        data class QuotedKey(@SerialName("a\"={") val value: Int)

        val input = "\"a\\\"={\" = 1"
        assertEquals(QuotedKey(1), Toml.decodeFromString<QuotedKey>(input))
    }

    @Test
    fun quotedKeyCanHaveAMultilineArrayValue() {
        @Serializable
        data class QuotedKey(@SerialName("a=[") val values: List<Int>)

        for (quote in listOf('"', '\'')) {
            val input = "${quote}a=[$quote = [\n1,\n2\n]"
            assertEquals(QuotedKey(listOf(1, 2)), Toml.decodeFromString<QuotedKey>(input))
        }
    }

    @Test
    fun quotedKeyCanHaveAMultilineInlineTableValue() {
        for (quote in listOf('"', '\'')) {
            val input = "${quote}a={$quote = {\nx = 1\n}"
            assertEquals(mapOf("a={" to mapOf("x" to 1)), Toml.decodeFromString<Map<String, Map<String, Int>>>(input))
        }
    }

    @Test
    fun testSpecialSymbolsInQuotedKey() {
        @Serializable
        data class SpecialSymbolClass(
            @SerialName("androidx.activity:activity-compose")
            val value: Boolean,
        )

        val toml1 = """
            "androidx.activity:activity-compose" = true
        """.trimIndent()
        val toml2 = """
            'androidx.activity:activity-compose' = true
        """.trimIndent()
        val expected = SpecialSymbolClass(true)

        assertEquals(expected, Toml.decodeFromString(toml1))
        assertEquals(expected, Toml.decodeFromString(toml2))
    }

    @Test
    fun testEqualSignInQuotedKey() {
        @Serializable
        data class EqualSignClass(
            @SerialName("qwe=123")
            val value: String,
        )

        val toml1 = """
            "qwe=123" = 'bar'
        """.trimIndent()
        val toml2 = """
            'qwe=123' = 'bar'
        """.trimIndent()
        val expected = EqualSignClass("bar")

        assertEquals(expected, Toml.decodeFromString(toml1))
        assertEquals(expected, Toml.decodeFromString(toml2))
    }
}
