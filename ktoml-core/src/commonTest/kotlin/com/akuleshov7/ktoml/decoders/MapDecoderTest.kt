package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.exceptions.TomlDecodingException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MapDecoderTest {
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

    @Test
    fun decodeMapOfObjects() {
        @Serializable
        data class Coin(
            val scale: Long,
            @SerialName("default_volume")
            val defaultVolume: Double,
            @SerialName("skip_in_orderbook")
            val skipInOrderbook: Double,
        )
        @Serializable
        data class CoinsConf(
            val coins: Map<String, Coin>
        )

        //language=toml
        val toml = """
            [coins.bitcoin]
            scale = 8
            default_volume = 0.1
            skip_in_orderbook = 0.00001

            [coins.ethereum]
            scale = 8
            default_volume = 0.2
            skip_in_orderbook = 0.00001

            [coins.tether]
            scale = 2
            default_volume = 100.0
            skip_in_orderbook = 0.0001
        """.trimIndent()

        assertEquals(
            CoinsConf(
                mapOf(
                    "bitcoin" to Coin(8, 0.1, 0.00001),
                    "ethereum" to Coin(8, 0.2, 0.00001),
                    "tether" to Coin(2, 100.0, 0.0001)
                )
            ),
            Toml().decodeFromString(CoinsConf.serializer(), toml),
        )
    }

    @Test
    fun decodeMapWithQuotedNames() {
        @Serializable
        data class Animal(
            val name: String,
            val vocal: String
        )
        @Serializable
        data class Animals(
            val animals: Map<String, Animal>
        )

        //language=toml
        val tomlString = """
            [animals."my cat"]
            name = "maunz"
            vocal = "miau"
            
            [animals."my dog"]
            name = "bello"
            vocal = "wuff"
        """.trimIndent()
        assertEquals(
            Animals(
                mapOf(
                    "my cat" to Animal("maunz", "miau"),
                    "my dog" to Animal("bello", "wuff")
                )
            ),
            Toml.decodeFromString<Animals>(tomlString),
        )
    }

    @Test
    fun decodeMapWithInlineTable() {
        @Serializable
        data class Table(
            val item1: String,
            val item2: Map<String, String>,
        )
        @Serializable
        data class Wrapper(
            val table: Table,
        )

        //language=toml
        val toml = """
            [table]
            "item1" = "val1"
            "item2" = { key1 = "val2", key2 = "val3" }
        """.trimIndent()
        assertEquals(
            Wrapper(
                Table(
                    item1 = "val1",
                    item2 = mapOf("key1" to "val2", "key2" to "val3")
                )
            ),
            Toml.decodeFromString<Wrapper>(toml),
        )
    }

    @Test
    fun shouldThrowIllegalTypeExceptionOnWrongMapType() {
        @Serializable
        data class MapWrapper(
            val a: Map<Int, Int>,
        )
        val toml = """
            a = "abc"
        """.trimIndent()

        assertFailsWith<IllegalTypeException> {
            Toml.decodeFromString<MapWrapper>(toml)
        }
    }
}
