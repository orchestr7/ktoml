package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.annotations.*
import com.akuleshov7.ktoml.writers.IntegerRepresentation.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class EncodingAnnotationTest {
    @Test
    fun commentedPairTest() {
        @Serializable
        data class File(
            @TomlComments("Single comment", inline = "")
            val a: Long = 3,
            @TomlComments("Comment 1", "Comment 2", inline = "")
            val b: String = "test",
            @TomlComments(inline = "Inline comment")
            val c: Boolean = true,
            @TomlComments(
                "Comment 1",
                "Comment 2",
                inline = "Inline comment"
            )
            val d: Double = Double.NaN
        )

        assertEquals(
            """
                # Single comment
                a = 3
                # Comment 1
                # Comment 2
                b = "test"
                c = true # Inline comment
                # Comment 1
                # Comment 2
                d = nan # Inline comment
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    fun commentedTableTest() {
        @Serializable
        data class TableA(val a: String = "")

        @Serializable
        data class TableB(val b: Long = 7)

        @Serializable
        data class File(
            @TomlComments("Comment 1", "Comment 2", inline = "")
            val tableA: TableA = TableA(),
            @TomlComments(inline = "Inline comment")
            val tableB: TableB = TableB()
        )

        assertEquals(
            """
                # Comment 1
                # Comment 2
                [tableA]
                    a = ""
                
                [tableB] # Inline comment
                    b = 7
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    fun basicInlineTableTest()
    {
        @Serializable
        @TomlInlineTable
        data class InlineTableA(
            val a1: String = "test",
            val a2: String = "test"
        )

        @Serializable
        data class InlineTableB(val b: Boolean = false)

        @Serializable
        data class File(
            val a: InlineTableA = InlineTableA(),
            @TomlInlineTable
            val b1: InlineTableB = InlineTableB(),
            val b2: @TomlInlineTable InlineTableB = InlineTableB()
        )

        assertEquals(
            """
                a = { a1 = "test", a2 = "test" }
                b1 = { b = false }
                b2 = { b = false }
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    fun nestedInlineTableTest() {
        @Serializable
        data class InlineTable(val message: String)

        @Serializable
        @TomlInlineTable
        data class NestedInlineTable(
            val inner1: InlineTable = InlineTable("a"),
            val inner2: InlineTable = InlineTable("b"),
        )

        @Serializable
        data class File(
            val nested: NestedInlineTable = NestedInlineTable()
        )

        assertEquals(
            """
                nested = { inner1 = { message = "a" }, inner2 = { message = "b" } }
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    @Ignore
    fun arrayOfInlineTablesText() {
        @Serializable
        data class InlineTable(val value: Long)

        @Serializable
        data class File(
            @TomlInlineTable
            val inlineTablesA: List<InlineTable> =
                    (0L..2L).map(::InlineTable),
            val inlineTablesB: @TomlInlineTable List<InlineTable> =
                    (3L..5L).map(::InlineTable),
            val inlineTablesC: List<@TomlInlineTable InlineTable> =
                    (6L..8L).map(::InlineTable)
        )

        assertEquals(
            """
                inlineTablesA = [ { value = 0 }, { value = 1 }, { value = 2 } ]
                inlineTablesB = [ { value = 3 }, { value = 4 }, { value = 5 } ]
                inlineTablesC = [ { value = 6 }, { value = 7 }, { value = 8 } ]
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    @Ignore
    fun multilineArrayTest() {
        @Serializable
        data class File(
            @TomlMultiline
            val words: List<String> =
                    listOf(
                        "the", "quick", "brown",
                        "fox", "jumps", "over",
                        "the", "lazy", "dog"
                    ),
            val fib: @TomlMultiline List<Long> =
                    listOf(1, 1, 2, 3, 5, 8, 13)
        )

        assertEquals(
            """
                words = [
                    "the",
                    "quick",
                    "brown",
                    "fox",
                    "jumps",
                    "over",
                    "the",
                    "lazy",
                    "dog"
                ]
                fib = [
                    1,
                    1,
                    2,
                    3,
                    5,
                    8,
                    13
                ]
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    @Ignore
    fun integerRepresentationTest() {
        @Serializable
        data class File(
            @TomlInteger(DECIMAL)
            val decA: Long = 0,
            val decB: @TomlInteger(DECIMAL) Long = 1,
            @TomlInteger(BINARY)
            val binA: Long = 2,
            val binB: @TomlInteger(BINARY) Long = 3,
            @TomlInteger(GROUPED)
            val groA: Long = 1_000_000,
            val groB: @TomlInteger(GROUPED) Long = 1_000,
            @TomlInteger(HEX)
            val hexA: Long = 4,
            val hexB: @TomlInteger(HEX) Long = 5,
            @TomlInteger(OCTAL)
            val octA: Long = 6,
            val octB: @TomlInteger(OCTAL) Long = 7
        )

        assertEquals(
            """
                decA = 0
                decB = 1
                binA = 0b10
                binB = 0b11
                groA = 1_000_000
                groB = 1_000
                hexA = 0x4
                hexB = 0x5
                octA = 0o6
                octB = 0o7
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    fun literalStringTest() {
        @Serializable
        data class File(
            @TomlLiteral
            val regex: String = """/[a-z-_]+|"[^"]+"|'[^']+'/""",
            val quote: @TomlLiteral String = "\"hello!\""
        )

        assertEquals(
            """
                regex = '/[a-z-_]+|"[^"]+"|'[^']+'/'
                quote = '"hello!"'
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }

    @Test
    @Ignore
    fun multilineStringTest() {
        @Serializable
        data class File(
            @TomlMultiline
            val mlTextA: String = "\n\\tMultiline\ntext!\n",
            val mlTextB: @[TomlMultiline TomlLiteral] String = "\n\"Multiline\n\"text!\n"
        )

        val tripleQuotes = "\"\"\""

        assertEquals(
            """
                mlTextA = $tripleQuotes
                \tMultiline
                text!
                $tripleQuotes
                mlTextB = '''
                "Multiline
                text!"
                '''
            """.trimIndent(),
            Toml.encodeToString(File())
        )
    }
}
