package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test

class ArrayEncoderTest {
    @Test
    fun emptyArrayTest() {
        @Serializable
        data class EmptyArray(val a: List<String> = emptyList())

        EmptyArray().shouldEncodeInto("a = [ ]")
    }
    
    @Test
    fun simpleArrayTest() {
        @Serializable
        data class SimpleArray(val a: List<Long> = listOf(1, 2, 3))

        SimpleArray().shouldEncodeInto("a = [ 1, 2, 3 ]")
    }

    @Test
    fun primitiveArrayTest() {
        @Serializable
        data class Arrays(
            val booleans: List<Boolean> = listOf(true, false),
            val longs: List<Long> = listOf(1, 2, 3),
            val doubles: List<Double> = listOf(3.14),
            val basicStrings: List<String> = listOf("a", "b", "c"),
            @TomlLiteral
            val literalStrings: List<String> = listOf("\"string\"")
        )

        Arrays().shouldEncodeInto(
            """
                booleans = [ true, false ]
                longs = [ 1, 2, 3 ]
                doubles = [ 3.14 ]
                basicStrings = [ "a", "b", "c" ]
                literalStrings = [ '"string"' ]
            """.trimIndent()
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

        InlineTableArray().shouldEncodeInto(
            """
                inlineTables = [
                    { index = 0 },
                    { index = 1 },
                    { index = 2 }
                ]
            """.trimIndent()
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

        NestedArray().shouldEncodeInto("a = [ [ 1, 2 ], [ 3, 4 ] ]")
    }

    @Test
    fun arrayInTableTest() {
        @Serializable
        data class Table(val a: List<Long> = listOf(1, 2, 3))

        @Serializable
        data class ArrayInTable(val table: Table = Table())

        ArrayInTable().shouldEncodeInto(
            """
                [table]
                    a = [ 1, 2, 3 ]
            """.trimIndent()
        )
    }

    @Test
    fun arrayInInlineTableTest() {
        @Serializable
        @TomlInlineTable
        data class InlineTable(val b: List<Long> = listOf(1, 2, 3))

        @Serializable
        data class ArrayInInlineTable(val a: InlineTable = InlineTable())

        ArrayInInlineTable().shouldEncodeInto("a = { b = [ 1, 2, 3 ] }")
    }

    @Test
    fun emptyArrayInTableTest(){
        @Serializable
        data class EmbeddedData(val data: String = "embedded data")

        @Serializable
        data class EmptyListData(val content: List<EmbeddedData> = listOf() )
        
        EmptyListData().shouldEncodeInto("")
    }
}
