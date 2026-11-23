@file:OptIn(com.akuleshov7.ktoml.annotations.InternalKtomlApi::class)

package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.IllegalTypeException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.parseValue
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLiteralString
import com.akuleshov7.ktoml.utils.validateIntegerSyntax
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ParserEdgeCaseRegressionTest {
    private val config = TomlInputConfig.compliant()
    private val format = Toml(config)

    @Test
    fun longIntegerPartOfFloatDoesNotOverflowStack() {
        // A normal parse error for an out-of-range number would be acceptable; a VM error is not.
        try {
            format.tomlParser.parseString("x = 1" + "0".repeat(10_000) + ".0")
        } catch (expected: ParseException) {
            // Numeric range policy is separate from this stack-safety check.
        }
    }

    @Test
    fun oversizedIntegerIsRejectedWithoutStackOverflow() {
        assertFailsWith<IllegalTypeException> {
            format.tomlParser.parseString("x = 1" + "0".repeat(10_000))
        }
    }

    @Test
    fun longIntegerSyntaxIsStackSafeForEveryRadix() {
        for (prefix in listOf("", "0b", "0o", "0x")) {
            (prefix + "1" + "0".repeat(10_000)).validateIntegerSyntax(1)
            (prefix + "1_".repeat(5_000) + "0").validateIntegerSyntax(1)
        }
    }

    @Test
    fun longMalformedIntegerSyntaxIsRejected() {
        for (prefix in listOf("", "0b", "0o", "0x")) {
            for (suffix in listOf("_", "__1", "G")) {
                assertFailsWith<ParseException> {
                    (prefix + "1".repeat(10_000) + suffix).validateIntegerSyntax(1)
                }
            }
        }
    }

    @Test
    fun integerSyntaxAcceptsOnlyAsciiDigits() {
        for (token in listOf("\u0661", "1\u0661", "0x\u0661", "0b\uff11", "0o\uff17")) {
            assertFailsWith<ParseException> { token.validateIntegerSyntax(1) }
        }
    }

    @Test
    fun unicodeWhitespaceOutsideContinuationStringIsRejected() {
        assertFailsWith<ParseException> {
            format.tomlParser.parseString("a = [\n\u30001\n]")
        }
    }

    @Test
    fun unicodeWhitespaceInsideMultilineStringRemainsValid() {
        val input = "a = [\"\"\"first\n\u3000second\"\"\"]"
        assertEquals(mapOf("a" to listOf("first\n\u3000second")), format.decodeFromString<Map<String, List<String>>>(input))
    }

    @Test
    fun reusedImplicitDottedTargetCannotBecomeHeader() {
        assertFailsWith<ParseException> {
            format.tomlParser.parseString("[a.b.c]\n[a]\nb.x = 1\n[a.b]")
        }
    }

    @Test
    fun reusedImplicitDottedPrefixCannotBecomeHeader() {
        assertFailsWith<ParseException> {
            format.tomlParser.parseString("[a.b.c.d]\n[a]\nb.c.x = 1\n[a.b]")
        }
    }

    @Test
    fun implicitSuperTablesRemainDefinableAfterSiblingHeaders() {
        format.tomlParser.parseString("[a.b.c]\n[a.b.d]\n[a]\nx = 1\n[a.b]\ny = 2")
    }

    @Test
    fun dottedTablesRemainExtensibleWithinTheirSection() {
        format.tomlParser.parseString("[a.b.c]\n[a]\nb.x = 1\nb.y = 2\n[a.b.d]\nz = 3")
    }

    @Test
    fun continuationValidationResumesAfterMultilineStrings() {
        for (delimiter in listOf("\"\"\"", "'".repeat(3))) {
            assertFailsWith<ParseException> {
                format.tomlParser.parseString("a = [" + delimiter + "first\n\u3000second" + delimiter + ",\n\u30001\n]")
            }
        }
    }

    @Test
    fun unicodeInMultilineLiteralsAndCommentsIsPreserved() {
        val input = "a = ['''first\n\u3000second''', # \u3000 comment\n'''third\n# \u3000fourth''']"
        assertEquals(mapOf("a" to listOf("first\n\u3000second", "third\n# \u3000fourth")),
            format.decodeFromString<Map<String, List<String>>>(input))
    }

    @Test
    fun escapedCarriageReturnIsPreserved() {
        assertEquals("first\r\nsecond", "\"\"\"first\\r\\nsecond\"\"\"".parseValue(1, config).content)
    }

    @Test
    fun directBasicConstructorAcceptsCrlf() {
        val source = "\"\"\"\r\nfirst\r\nsecond\"\"\""
        assertEquals("first\nsecond", TomlBasicString(source, 1).content)
    }

    @Test
    fun directLiteralConstructorAcceptsCrlf() {
        val source = "'".repeat(3) + "\r\nfirst\r\nsecond" + "'".repeat(3)
        assertEquals("first\nsecond", TomlLiteralString(source, 1, config).content)
    }

    @Test
    fun publicValueParserAcceptsCrlf() {
        for (delimiter in listOf("\"\"\"", "'".repeat(3))) {
            val source = delimiter + "\r\nfirst\r\nsecond" + delimiter
            assertEquals("first\nsecond", source.parseValue(1, config).content)
        }
    }

    @Test
    fun loneCarriageReturnRemainsInvalid() {
        for (delimiter in listOf("\"\"\"", "'".repeat(3))) {
            assertFailsWith<ParseException> { (delimiter + "first\rsecond" + delimiter).parseValue(1, config) }
        }
    }

    @Serializable
    private data class Document(val arr: List<Element>)
    @Serializable
    private data class Element(val a: Parent)
    @Serializable
    private data class Parent(val b: List<Leaf>)
    @Serializable
    private data class Leaf(val c: Int)

    @Test
    fun dottedNestedInlineArrayDecodesIntoModel() {
        val expected = Document(listOf(Element(Parent(listOf(Leaf(1))))))
        assertEquals(expected, format.decodeFromString<Document>("arr = [{ a.b = [{ c = 1 }] }]"))
    }

    @Test
    fun dottedNestedArraysKeepArrayElementsSeparate() {
        val expected = Document(listOf(Element(Parent(listOf(Leaf(1)))), Element(Parent(listOf(Leaf(2))))))
        assertEquals(expected, format.decodeFromString<Document>("arr = [{ a.b = [{ c = 1 }] }, { a.b = [{ c = 2 }] }]"))
    }

    @Test
    fun dottedNestedArraysCanBeEmpty() {
        val expected = Document(listOf(Element(Parent(emptyList()))))
        assertEquals(expected, format.decodeFromString<Document>("arr = [{ a.b = [] }]"))
    }

    @Serializable
    private data class ScalarSiblingDocument(val arr: List<ScalarSiblingElement>)
    @Serializable
    private data class ScalarSiblingElement(val a: ScalarSibling)
    @Serializable
    private data class ScalarSibling(val b: List<Leaf>, val d: Int)

    @Test
    fun dottedNestedArraysSharePrefixesWithScalarFields() {
        val expected = ScalarSiblingDocument(listOf(ScalarSiblingElement(ScalarSibling(listOf(Leaf(1)), 2))))
        for (fields in listOf("a.b = [{ c = 1 }], a.d = 2", "a.d = 2, a.b = [{ c = 1 }]")) {
            assertEquals(expected, format.decodeFromString<ScalarSiblingDocument>("arr = [{ $fields }]"))
        }
    }

    @Serializable
    private data class InlineSiblingDocument(val arr: List<InlineSiblingElement>)
    @Serializable
    private data class InlineSiblingElement(val a: InlineSibling)
    @Serializable
    private data class InlineSibling(val b: List<Leaf>, val d: Leaf)

    @Test
    fun dottedNestedArraysSharePrefixesWithInlineTables() {
        val expected = InlineSiblingDocument(listOf(InlineSiblingElement(InlineSibling(listOf(Leaf(1)), Leaf(2)))))
        assertEquals(expected, format.decodeFromString<InlineSiblingDocument>("arr = [{ a.b = [{ c = 1 }], a.d = { c = 2 } }]"))
    }

    @Serializable
    private data class SiblingDocument(val arr: List<SiblingElement>)
    @Serializable
    private data class SiblingElement(val a: Siblings)
    @Serializable
    private data class Siblings(val b: List<Leaf>, val d: List<Leaf>)

    @Test
    fun dottedNestedArraysShareOnlyTheirCommonPrefix() {
        val expected = SiblingDocument(listOf(SiblingElement(Siblings(listOf(Leaf(1)), listOf(Leaf(2))))))
        assertEquals(expected, format.decodeFromString<SiblingDocument>("arr = [{ a.b = [{ c = 1 }], a.d = [{ c = 2 }] }]"))
    }

    @Serializable
    private data class QuotedDocument(val arr: List<QuotedElement>)
    @Serializable
    private data class QuotedElement(val a: QuotedParent)
    @Serializable
    private data class QuotedParent(@SerialName("b.d") val values: List<Leaf>)

    @Test
    fun dottedNestedArraysPreserveQuotedDots() {
        val expected = QuotedDocument(listOf(QuotedElement(QuotedParent(listOf(Leaf(1))))))
        assertEquals(expected, format.decodeFromString<QuotedDocument>("arr = [{ a.\"b.d\" = [{ c = 1 }] }]"))
    }
}
