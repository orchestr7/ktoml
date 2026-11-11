package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReviewRegressionTest {
    private val format = Toml(TomlInputConfig.compliant())

    @Serializable
    private data class Text(val s: String)

    @Serializable
    private data class TextTable(val a: Text)

    @Serializable
    private data class Point(val x: Int)

    @Serializable
    private data class Points(val points: List<List<Point>>)

    @Test
    fun lineContinuationRejectsNonAsciiWhitespaceBeforeNewline() {
        for (space in listOf('\u00a0', '\u2003', '\u000b')) {
            assertFailsWith<ParseException> {
                format.decodeFromString(Text.serializer(), "s = \"\"\"one\\$space\ntwo\"\"\"")
            }
        }
    }

    @Test
    fun lineContinuationPreservesNonAsciiWhitespaceAfterNewline() {
        assertEquals(Text("one\u00a0two"), format.decodeFromString(Text.serializer(), "s = \"\"\"one\\\n \t\u00a0two\"\"\""))
    }

    @Test
    fun inlineTableHandlesEvenBackslashesBeforeClosingQuote() {
        for (count in listOf(2, 4, 6)) {
            val source = "a = { s = \"" + "\\".repeat(count) + "\" } # trailing comment"
            assertEquals(TextTable(Text("\\".repeat(count / 2))), format.decodeFromString(TextTable.serializer(), source))
        }
    }

    @Test
    fun inlineTableHandlesQuoteRunsAndLiteralBackslashes() {
        for (quote in listOf('"', '\'')) {
            for (extra in 0..2) {
                val source = "a = { s = " + quote.toString().repeat(6 + extra) + " }"
                assertEquals(TextTable(Text(quote.toString().repeat(extra))), format.decodeFromString(TextTable.serializer(), source))
            }
        }
        assertEquals(TextTable(Text("\\")), format.decodeFromString(TextTable.serializer(), "a = { s = '\\' }"))
    }

    @Test
    fun dottedInlineAssignmentDoesNotConflictWithUnrelatedRootKey() {
        val document = format.tomlParser.parseString("b = 1\na.b = { c = 2 }")
        assertEquals(listOf("b", "a"), document.children.map { it.name })
    }

    @Test
    fun arrayOfTablesHeaderCannotAppendToStaticInlineArray() {
        assertFailsWith<ParseException> { format.tomlParser.parseString("a = [{ x = 1 }]\n[[a]]\nx = 2") }
        format.tomlParser.parseString("[[a]]\nx = 1\n[[a]]\nx = 2")
    }

    @Test
    fun inlineTableCannotBeExtendedByAnotherInlineAssignment() {
        for (suffix in listOf("a.c = { d = 2 }", "a.b.c = { d = 2 }", "a = { c = 2 }")) {
            assertFailsWith<ParseException> { format.tomlParser.parseString("a = { b = 1 }\n$suffix") }
        }
    }

    @Test
    fun dottedParentOfInlineTableRemainsOpen() {
        format.tomlParser.parseString("a.b = { x = 1 }\na.c = 2")
        format.tomlParser.parseString("a.b = { x = 1 }\na.c = { y = 2 }")
        assertFailsWith<ParseException> { format.tomlParser.parseString("a.b = { x = 1 }\na.b.y = 2") }
    }

    @Test
    fun decodesInlineTablesInNestedArrays() {
        assertEquals(Points(listOf(listOf(Point(1)), listOf(Point(2), Point(3)))), format.decodeFromString(
            Points.serializer(), "points = [[{ x = 1 }], [{ x = 2 }, { x = 3 }]]",
        ))
    }

    @Serializable
    private data class MappedPoints(val points: List<List<Map<String, Int>>>)

    @Test
    fun decodesInlineTablesInNestedArraysAsMaps() {
        assertEquals(MappedPoints(listOf(listOf(mapOf("x" to 1)))), format.decodeFromString(
            MappedPoints.serializer(), "points = [[{ x = 1 }]]",
        ))
    }

    @Test
    fun arrayClassifierPreservesInvalidEmptyEntries() {
        for (source in listOf("items = [{s = 1},,]", "items = [{s = 1},,{s = 2}]")) {
            assertFailsWith<ParseException> { format.tomlParser.parseString(source) }
        }
    }

    @Test
    fun mixedArraysWithQuotedTablesRemainMixed() {
        for (value in listOf("\"a\\\"b\"", "\"\"\"a\"b,}\\\"c\"\"\"", "'".repeat(3) + "a\"b,}c" + "'".repeat(3), "'a\\'")) {
            format.tomlParser.parseString("items = [{s = $value}, 2]")
        }
    }
}
