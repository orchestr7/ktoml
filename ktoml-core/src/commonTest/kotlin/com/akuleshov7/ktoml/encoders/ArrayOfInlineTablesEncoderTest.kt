package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.TomlInlineTable
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.Serializable
import kotlin.test.Test

class ArrayOfInlineTablesEncoderTest {

    @Test
    fun inlineTableArrayTest() {
        @Serializable
        @TomlInlineTable
        data class InlineTable(val index1: Long, val index2: Long)

        @Serializable
        data class InlineTableArray(
            val inlineTables: List<InlineTable> =
                (0L..3L).map { it -> InlineTable(it, it)}
        )

        assertEncodedEquals(
            value = InlineTableArray(),
            expectedToml = """
                inlineTables = [ { index1 = 0, index2 = 0 }, { index1 = 1, index2 = 1 }, { index1 = 2, index2 = 2 }, { index1 = 3, index2 = 3 } ]
            """.trimIndent()
        )
    }

    @Serializable
    @TomlInlineTable
    data class InlineTable(val index1: Long, val index2: Long)

    @Serializable
    data class InlineTableArray(
        @TomlMultiline
        val inlineTables: List<InlineTable> =
            (0L..3L).map { it -> InlineTable(it, it)}
    )

    @Test
    fun multilineInlineTableArrayTest() {
        assertEncodedEquals(
            value = InlineTableArray(),
            expectedToml = """
                inlineTables = [
                    { index1 = 0, index2 = 0 },
                    { index1 = 1, index2 = 1 },
                    { index1 = 2, index2 = 2 },
                    { index1 = 3, index2 = 3 }
                ]
            """.trimIndent()
        )
    }

    @Test
    fun inlineTableArrayInsideTable() {
        @Serializable
        data class Container(
            val table: InlineTableArray = InlineTableArray(),
            val int: Int = 2,
        )

        assertEncodedEquals(
            value = Container(),
            expectedToml = """
                int = 2
                
                [table]
                    inlineTables = [
                        { index1 = 0, index2 = 0 },
                        { index1 = 1, index2 = 1 },
                        { index1 = 2, index2 = 2 },
                        { index1 = 3, index2 = 3 }
                    ]
            """.trimIndent()
        )
    }

    @Test
    fun inlineTableArrayInsideTableArrayTest() {
        @Serializable
        data class Container(val tables: List<InlineTableArray> = listOf(InlineTableArray(), InlineTableArray()))

        assertEncodedEquals(
            value = Container(),
            expectedToml = """
                [[tables]]
                    inlineTables = [
                        { index1 = 0, index2 = 0 },
                        { index1 = 1, index2 = 1 },
                        { index1 = 2, index2 = 2 },
                        { index1 = 3, index2 = 3 }
                    ]
                
                [[tables]]
                    inlineTables = [
                        { index1 = 0, index2 = 0 },
                        { index1 = 1, index2 = 1 },
                        { index1 = 2, index2 = 2 },
                        { index1 = 3, index2 = 3 }
                    ]
            """.trimIndent()
        )
    }

    @Test
    fun withAnnotationTargetOnProperty() {
        @Serializable
        data class InlineTable(val value: Long)

        @Serializable
        data class File(
            @TomlInlineTable
            val inlineTablesA: List<InlineTable> =
                (0L..2L).map(::InlineTable),
        )

        assertEncodedEquals(
            value = File(),
            expectedToml = """
                inlineTablesA = [ { value = 0 }, { value = 1 }, { value = 2 } ]
            """.trimIndent()
        )
    }

    @Test
    fun emptyInlineTableArrayTest() {
        assertEncodedEquals(
            value = InlineTableArray(inlineTables = emptyList()),
            expectedToml = "",
        )
    }
}
