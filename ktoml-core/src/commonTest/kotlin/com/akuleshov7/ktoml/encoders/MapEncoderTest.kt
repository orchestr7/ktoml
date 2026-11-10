package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test

class MapEncoderTest {
    // language=toml
    private val simpleTable = """
        [map]
            keyA = "a"
            keyB = "b"
            keyC = "c"
    """.trimIndent()

    // language=toml
    private val inlineTable = """
        map = { keyA = "a", keyB = "b", keyC = "c" }
    """.trimIndent()

    // language=toml
    private val pairArray = """
        map = [
            [ { x = 0, y = 1 }, "north" ],
            [ { x = 1, y = 0 }, "east" ],
            [ { x = 0, y = -1 }, "south" ],
            [ { x = -1, y = 0 }, "west" ]
        ]
    """.trimIndent()

    private val stringMap = mapOf(
        "keyA" to "a",
        "keyB" to "b",
        "keyC" to "c"
    )

    private val enumMap = Key.values().associateWith(Key::value)

    @Serializable
    enum class Key(val value: String) {
        @SerialName("keyA") KeyA("a"),
        @SerialName("keyB") KeyB("b"),
        @SerialName("keyC") KeyC("c")
    }

    @Test
    fun mapAsTableTest() {
        @Serializable
        data class File(val map: Map<String, String>)

        assertEncodedEquals(value = File(stringMap), expectedToml = simpleTable)
    }

    @Test
    fun mapAsInlineTableTest() {
        @Serializable
        data class File(
            @TomlInlineTable
            val map: Map<String, String>
        )

        assertEncodedEquals(value = File(stringMap), expectedToml = inlineTable)
    }

    @Test
    fun enumMapAsTableTest() {
        @Serializable
        data class File(val map: Map<Key, String>)

        assertEncodedEquals(value = File(enumMap), expectedToml = simpleTable)
    }

    @Test
    fun enumMapAsInlineTableTest() {
        @Serializable
        data class File(
            @TomlInlineTable
            val map: Map<Key, String>
        )

        assertEncodedEquals(value = File(enumMap), expectedToml = inlineTable)
    }

    @Test
    @Ignore
    fun arbitraryKeyMapTest() {
        @Serializable
        data class Point(val x: Int, val y: Int)

        @Serializable
        data class File(
            @TomlMultiline
            val map: Map<Point, String> = mapOf(
                Point(0, 1) to "north",
                Point(1, 0) to "east",
                Point(0, -1) to "south",
                Point(-1, 0) to "west"
            )
        )

        assertEncodedEquals(value = File(), expectedToml = pairArray)
    }
}
