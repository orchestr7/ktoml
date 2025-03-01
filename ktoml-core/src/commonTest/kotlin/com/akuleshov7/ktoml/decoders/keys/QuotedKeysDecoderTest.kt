package com.akuleshov7.ktoml.decoders.keys

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

class QuotedKeysDecoderTest {

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
