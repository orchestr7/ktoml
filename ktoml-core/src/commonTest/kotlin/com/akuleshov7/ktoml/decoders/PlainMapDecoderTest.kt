package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlin.test.Test

class PlainMapDecoderTest {
    @Serializable
    private data class TestDataMap(
        val text: String,
        val map: Map<String, String>,
        val number: Int,
    )

    @Test
    fun testMapDecoderPositiveCase() {
        var data = """
            text = "Test"
            number = 15
            [map]
              a = 1
              b = 1
              c = 1
              number = 31
        """.trimIndent()

        Toml.decodeFromString<TestDataMap>(data)

        data = """
            map = { a = 1,  b = 2, c = 3 }
            text = "Test"
            number = 15
        """.trimIndent()

        Toml.decodeFromString<TestDataMap>(data)
    }

    @Test
    fun testMapDecoderNegativeCases() {
        var data = """
            a = 1
            b = 1 
            c = 1
            text = "Test"
            number = 15
        """.trimIndent()

        Toml.decodeFromString<TestDataMap>(data)

        data = """
            [map]
                [map.a]
                     b = 1
                [map.b]
                     c = 1
            text = "Test"
            number = 15
        """.trimIndent()

        Toml.decodeFromString<TestDataMap>(data)

        data = """
            text = "Test"
            number = 15
        """.trimIndent()

        Toml.decodeFromString<TestDataMap>(data)
    }

    @Test
    fun testSimpleMapDecoder() {
        val data = TestDataMap(text = "text value", number = 7321, map = mapOf("a" to "b", "c" to "d"))
        val encoded = Toml.encodeToString(data)
        val decoded: TestDataMap = Toml.decodeFromString(encoded) // throws MissingRequiredPropertyException
    }
}
