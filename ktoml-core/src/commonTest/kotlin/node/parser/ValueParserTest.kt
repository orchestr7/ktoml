package com.akuleshov7.ktoml.test.node.parser

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.node.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

enum class NodeType {
    STRING, NULL, INT, FLOAT, BOOLEAN, INCORRECT
}

class ValueParserTest {
    @Test
    fun parsingTest() {
        testTomlValue("a = \"gfhgfhfg\"", NodeType.STRING)
        testTomlValue("a = 123456", NodeType.INT)
        testTomlValue("a = 12.2345", NodeType.FLOAT)
        testTomlValue("a = true", NodeType.BOOLEAN)
        testTomlValue("a = false", NodeType.BOOLEAN)
        // regression test related to comments with an equals symbol after it
        testTomlValue("lineCaptureGroup = 1  # index `warningTextHasLine = false`\n", NodeType.INT)
    }

    @Test
    fun quotesParsingTest() {
        assertFailsWith<TomlParsingException> {
            TomlKeyValue("a = hello world", 0)
        }
        assertFailsWith<TomlParsingException> {
            TomlKeyValue("a = \"before \" string\"", 0)
        }
    }

    @Test
    fun specialSymbolsParsing() {
        assertFailsWith<TomlParsingException> { TomlKeyValue("a = \"hello\\world\"", 0) }

        var test = TomlKeyValue("a = \"hello\\tworld\"", 0)
        assertEquals("hello\tworld", test.value.content)

        test = TomlKeyValue("a = \"helloworld\\n\"", 0)
        assertEquals("helloworld\n", test.value.content)

        test = TomlKeyValue("a = \"helloworld\\\"", 0)
        assertEquals("helloworld\\", test.value.content)

        test = TomlKeyValue("a = \"hello\\nworld\"", 0)
        assertEquals("hello\nworld", test.value.content)

        test = TomlKeyValue("a = \"hello\\bworld\"", 0)
        assertEquals("hello\bworld", test.value.content)

        test = TomlKeyValue("a = \"hello\\rworld\"", 0)
        assertEquals("hello\rworld", test.value.content)

        test = TomlKeyValue("a = \"hello\\\\tworld\"", 0)
        assertEquals("hello\\tworld", test.value.content)

        test = TomlKeyValue("a = \"hello\\\\world\"", 0)
        assertEquals("hello\\world", test.value.content)

        test = TomlKeyValue("a = \"hello tworld\"", 0)
        assertEquals("hello tworld", test.value.content)

        test = TomlKeyValue("a = \"hello\t\\\\\\\\world\"", 0)
        assertEquals("hello\t\\\\world", test.value.content)
    }

    @Test
    fun parsingIssueValue() {
        assertFails { TomlKeyValue("   a  = b = c", 0) }
        assertFails { TomlKeyValue(" = false", 0) }
        assertFails { TomlKeyValue(" just false", 0) }
    }
}

fun getNodeType(v: TomlValue): NodeType = when (v) {
    is TomlString -> NodeType.STRING
    is TomlNull -> NodeType.NULL
    is TomlInt -> NodeType.INT
    is TomlFloat -> NodeType.FLOAT
    is TomlBoolean -> NodeType.BOOLEAN
    else -> NodeType.INCORRECT
}

fun testTomlValue(s: String, expectedType: NodeType) {
    assertEquals(expectedType, getNodeType(TomlKeyValue(s, 0).value))
}
