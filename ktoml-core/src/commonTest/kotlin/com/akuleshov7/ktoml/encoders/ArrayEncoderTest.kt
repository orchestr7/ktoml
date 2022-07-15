package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class ArrayEncoderTest {
    @Test
    fun emptyArrayTest() {
        @Serializable
        data class EmptyArray(val a: List<String> = emptyList())

        assertEquals(
            "a = [ ]",
            Toml.encodeToString(EmptyArray())
        )
    }

    @Test
    fun simpleArrayTest() {
        @Serializable
        data class SimpleArray(val a: List<Long> = listOf(1, 2, 3))

        assertEquals(
            "a = [ 1, 2, 3 ]",
            Toml.encodeToString(SimpleArray())
        )
    }

    @Test
    fun primitiveArrayTest() {
        @Serializable
        data class Array(
            val booleans: List<Boolean> = listOf(true, false),
            val longs: List<Long> = listOf(1, 2, 3),
            val doubles: List<Double> = listOf(3.14),
            val basicStrings: List<String> = listOf("a", "b", "c"),
            val literalStrings: List<@TomlLiteral String> = listOf("\"string\"")
        )

        assertEquals(
            """
            booleans = [ true, false ]
            longs = [ 1, 2, 3 ]
            doubles = [ 3.14 ]
            basicStrings = [ "a", "b", "c" ]
            literalStrings = [ '"string"' ]
            """.trimIndent(),
            Toml.encodeToString(Array())
        )
    }

    @Test
    @Ignore
    fun inlineTableArrayTest() {
        @Serializable
        @TomlInlineTable
        data class InlineTable(val index: Long)

        @Serializable
        data class InlineTableArray(
            @TomlMultiline
            val inlineTables: List<InlineTable> =
                    (0L..2L).map(::InlineTable)
        )

        assertEquals(
            """
            inlineTables = [
                { index = 0 },
                { index = 1 },
                { index = 2 }
            ]
            """.trimIndent(),
            Toml.encodeToString(InlineTableArray())
        )
    }

    @Test
    fun nestedArrayTest() {
        @Serializable
        data class NestedArray(
            val a: List<List<Long>> =
                    listOf(
                        listOf(1, 2),
                        listOf(3, 4)
                    )
        )

        assertEquals(
            """
            a = [ [ 1, 2 ], [ 3, 4 ] ]
            """.trimIndent(),
            Toml.encodeToString(NestedArray())
        )
    }

    @Test
    fun arrayInTableTest() {
        @Serializable
        data class Table(val a: List<Long> = listOf(1, 2, 3))

        @Serializable
        data class ArrayInTable(val table: Table = Table())

        assertEquals(
            """
            [table]
                a = [ 1, 2, 3 ]
            """.trimIndent(),
            Toml.encodeToString(ArrayInTable())
        )
    }
}
