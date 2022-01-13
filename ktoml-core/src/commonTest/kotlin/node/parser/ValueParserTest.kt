package com.akuleshov7.ktoml.test.node.parser

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.exceptions.ParseException
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
        testTomlValue(Pair("a", "\'false\'"), NodeType.LITERAL_STRING)
    }

    @Test
    fun quotesParsingTest() {
        assertFailsWith<ParseException> {
            TomlKeyValuePrimitive(Pair("\"a", "123"), 0)
        }

        assertFailsWith<ParseException> {
            TomlKeyValuePrimitive(Pair("a", "hello world"), 0)
        }
        assertFailsWith<ParseException> {
            TomlKeyValuePrimitive(Pair("a", "\"before \" string\""), 0)
        }
    }


    @Test
    fun specialSymbolsParsing() {
        assertFailsWith<ParseException> { TomlKeyValuePrimitive(Pair("a", "\"hello\\world\""), 0) }

        var test = TomlKeyValuePrimitive(Pair("a", "\"hello\\tworld\""), 0)
        assertEquals("hello\tworld", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"helloworld\\n\""), 0)
        assertEquals("helloworld\n", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"helloworld\\\""), 0)
        assertEquals("helloworld\\", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello\\nworld\""), 0)
        assertEquals("hello\nworld", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello\\bworld\""), 0)
        assertEquals("hello\bworld", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello\\rworld\""), 0)
        assertEquals("hello\rworld", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello\\\\tworld\""), 0)
        assertEquals("hello\\tworld", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello\\\\world\""), 0)
        assertEquals("hello\\world", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello tworld\""), 0)
        assertEquals("hello tworld", test.value.content)

        test = TomlKeyValuePrimitive(Pair("a", "\"hello\t\\\\\\\\world\""), 0)
        assertEquals("hello\t\\\\world", test.value.content)

        // regression test related to comments with an equals symbol after it
        var pairTest =
            "lineCaptureGroup = 1  # index `warningTextHasLine = false`\n".splitKeyValue(0, config = KtomlConf())
        assertEquals(1L, TomlKeyValuePrimitive(pairTest, 0).value.content)

        pairTest = "lineCaptureGroup = \"1 = 2\"  # index = `warningTextHasLine = false`\n".splitKeyValue(0, config = KtomlConf())
        assertEquals("1 = 2", TomlKeyValuePrimitive(pairTest, 0).value.content)
    }


    @Test
    fun parsingIssueValue() {
        assertFailsWith<ParseException> { " = false".splitKeyValue(0, config = KtomlConf()) }
        assertFailsWith<ParseException> { " just false".splitKeyValue(0, config = KtomlConf()) }
        assertFailsWith<ParseException> { TomlKeyValuePrimitive(Pair("a", "\"\\hello tworld\""), 0) }
    }
}

enum class NodeType {
    STRING, NULL, INT, FLOAT, BOOLEAN, INCORRECT, LITERAL_STRING
}

fun getNodeType(v: TomlValue): NodeType = when (v) {
    is TomlBasicString -> NodeType.STRING
    is TomlNull -> NodeType.NULL
    is TomlLong -> NodeType.INT
    is TomlDouble -> NodeType.FLOAT
    is TomlBoolean -> NodeType.BOOLEAN
    is TomlLiteralString -> NodeType.LITERAL_STRING
    else -> NodeType.INCORRECT
}


fun testTomlValue(keyValuePair: Pair<String, String>, expectedType: NodeType) {
    assertEquals(expectedType, getNodeType(TomlKeyValuePrimitive(keyValuePair, 0).value))
}
