package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
import com.akuleshov7.ktoml.exceptions.UnsupportedDecoderException

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import kotlin.test.*

class PlainMapDecoderTest {
    @Serializable
    private data class TestDataMap(
        val text: String = "Test",
        val map: Map<String, Long>,
        val number: Int = 31,
    )

    @Test
    @Ignore
    fun typeErrorsInDecoding() {
        val data = """
            text = "Test"
            number = 15
            [map]
              a = "fff"
              b = 2
              c = 3
              number = 31
              e = 4
        """.trimIndent()

        assertFailsWith<TomlDecodingException> {
            Toml.decodeFromString<TestDataMap>(data)
        }
    }

    @Test
    fun testMapDecoderPositiveCase() {
        var data = """
            text = "Test"
            number = 15
            [map]
              a = 1
              b = 2
              c = 3
              number = 31
              e = 4
        """.trimIndent()

        assertEquals(
            TestDataMap("Test", mapOf("a" to 1, "b" to 2, "c" to 3, "number" to 31, "e" to 4), 15),
            Toml.decodeFromString<TestDataMap>(data)
        )

        data = """
            text = "Test"
            number = 15
            [map]
              a = 1
              b = 2
              c = 3
              number = 31
              # e = 4
        """.trimIndent()

        assertEquals(
            TestDataMap("Test", mapOf("a" to 1, "b" to 2, "c" to 3, "number" to 31), 15),
            Toml.decodeFromString<TestDataMap>(data)
        )

        data = """
            [map]
              a = 1
              b = 2
              c = 3
              number = 15
              # e = 4
        """.trimIndent()

        assertEquals(
            TestDataMap("Test", mapOf("a" to 1, "b" to 2, "c" to 3, "number" to 15), 31),
            Toml.decodeFromString<TestDataMap>(data)
        )


        data = """
            map = { a = 1,  b = 2, c = 3, number = 15 }
            text = "Test"
            number = 15
        """.trimIndent()

        assertEquals(
            TestDataMap("Test", mapOf("a" to 1, "b" to 2, "c" to 3, "number" to 15), 15),
            Toml.decodeFromString<TestDataMap>(data)
        )
    }

    @Test
    fun testRootMapDecoder() {
        val map = mapOf(
            "my_key_1" to "my_value_1",
            "my_key_2" to "my_value_2",
        )
        val encodedString = Toml.encodeToString(map)

        assertEquals(
            map,
            Toml.decodeFromString<Map<String, String>>(encodedString),
        )
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

        assertFailsWith<MissingRequiredPropertyException> {
            Toml.decodeFromString<TestDataMap>(data)
        }

        data = """
            text = "Test"
            number = 15
        """.trimIndent()

        assertFailsWith<MissingRequiredPropertyException> {
            Toml.decodeFromString<TestDataMap>(data)
        }
    }

    @Test
    fun testSimpleMapDecoder() {
        val data = TestDataMap(text = "text value", number = 7321, map = mapOf("a" to 3, "c" to 4))
        val encoded = Toml.encodeToString(data)
        val decoded: TestDataMap = Toml.decodeFromString(encoded)

        assertEquals(
            data,
            decoded
        )
    }
}
