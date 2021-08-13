package com.akuleshov7.ktoml.test.node.parser

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.exceptions.TomlParsingException
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
        var pairTest =
            "lineCaptureGroup = 1  # index `warningTextHasLine = false`\n".splitKeyValue(0, ktomlConf = KtomlConf())
        assertEquals(1L, TomlKeyValueSimple(pairTest, 0).value.content)

        pairTest = "lineCaptureGroup = \"1 = 2\"  # index = `warningTextHasLine = false`\n".splitKeyValue(0, ktomlConf = KtomlConf())
        assertEquals("1 = 2", TomlKeyValueSimple(pairTest, 0).value.content)
    }


    @Test
    fun parsingIssueValue() {
        assertFailsWith<TomlParsingException> { " = false".splitKeyValue(0, ktomlConf = KtomlConf()) }
        assertFailsWith<TomlParsingException> { " just false".splitKeyValue(0, ktomlConf = KtomlConf()) }
        assertFailsWith<TomlParsingException> { TomlKeyValueSimple(Pair("a", "\"\\hello tworld\""), 0) }
    }
}

enum class NodeType {
    STRING, NULL, INT, FLOAT, BOOLEAN, INCORRECT
}

fun getNodeType(v: TomlValue): NodeType = when (v) {
    is TomlBasicString -> NodeType.STRING
    is TomlNull -> NodeType.NULL
    is TomlLong -> NodeType.INT
    is TomlDouble -> NodeType.FLOAT
    is TomlBoolean -> NodeType.BOOLEAN
    else -> NodeType.INCORRECT
}


fun testTomlValue(keyValuePair: Pair<String, String>, expectedType: NodeType) {
    assertEquals(expectedType, getNodeType(TomlKeyValueSimple(keyValuePair, 0).value))
}
