package com.akuleshov7.ktoml.test.node.parser

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.ParserConf
import com.akuleshov7.ktoml.parsers.node.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValueParserTest {
    @Test
    fun parsingTest() {
        testTomlValue(Pair("a", "\"gfhgfhfg\""), NodeType.STRING)
        testTomlValue(Pair("a", "123456"), NodeType.INT)
        testTomlValue(Pair("a", "12.2345"), NodeType.FLOAT)
        testTomlValue(Pair("a", "true"), NodeType.BOOLEAN)
        testTomlValue(Pair("a", "false"), NodeType.BOOLEAN)
    }

    @Test
    fun quotesParsingTest() {
        assertFailsWith<TomlParsingException> {
            TomlKeyValueSimple(Pair("\"a", "123"), 0)
        }

        assertFailsWith<TomlParsingException> {
            TomlKeyValueSimple(Pair("a", "hello world"), 0)
        }
        assertFailsWith<TomlParsingException> {
            TomlKeyValueSimple(Pair("a", "\"before \" string\""), 0)
        }
    }


    @Test
    fun specialSymbolsParsing() {
        assertFailsWith<TomlParsingException> { TomlKeyValueSimple(Pair("a", "\"hello\\world\""), 0) }

        var test = TomlKeyValueSimple(Pair("a", "\"hello\\tworld\""), 0)
        assertEquals("hello\tworld", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"helloworld\\n\""), 0)
        assertEquals("helloworld\n", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"helloworld\\\""), 0)
        assertEquals("helloworld\\", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello\\nworld\""), 0)
        assertEquals("hello\nworld", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello\\bworld\""), 0)
        assertEquals("hello\bworld", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello\\rworld\""), 0)
        assertEquals("hello\rworld", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello\\\\tworld\""), 0)
        assertEquals("hello\\tworld", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello\\\\world\""), 0)
        assertEquals("hello\\world", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello tworld\""), 0)
        assertEquals("hello tworld", test.value.content)

        test = TomlKeyValueSimple(Pair("a", "\"hello\t\\\\\\\\world\""), 0)
        assertEquals("hello\t\\\\world", test.value.content)

        // regression test related to comments with an equals symbol after it
        val pairTest =
            "lineCaptureGroup = 1  # index `warningTextHasLine = false`\n".splitKeyValue(0, parserConf = ParserConf())
        assertEquals(1, TomlKeyValueSimple(pairTest, 0).value.content)

    }


    @Test
    fun parsingIssueValue() {
        assertFailsWith<TomlParsingException> { "   a  = b = c".splitKeyValue(0, parserConf = ParserConf()) }
        assertFailsWith<TomlParsingException> { " = false".splitKeyValue(0, parserConf = ParserConf()) }
        assertFailsWith<TomlParsingException> { " just false".splitKeyValue(0, parserConf = ParserConf()) }
        assertFailsWith<TomlParsingException> { TomlKeyValueSimple(Pair("a", "\"\\hello tworld\""), 0) }
    }
}

enum class NodeType {
    STRING, NULL, INT, FLOAT, BOOLEAN, INCORRECT
}

fun getNodeType(v: TomlValue): NodeType = when (v) {
    is TomlBasicString -> NodeType.STRING
    is TomlNull -> NodeType.NULL
    is TomlInt -> NodeType.INT
    is TomlFloat -> NodeType.FLOAT
    is TomlBoolean -> NodeType.BOOLEAN
    else -> NodeType.INCORRECT
}


fun testTomlValue(keyValuePair: Pair<String, String>, expectedType: NodeType) {
    assertEquals(expectedType, getNodeType(TomlKeyValueSimple(keyValuePair, 0).value))
}
