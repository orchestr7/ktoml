package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.values.*
import com.akuleshov7.ktoml.tree.nodes.splitKeyValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
    fun dateTimeParsingTest() {
        testTomlValue("a" to "1979-05-27T07:32:00Z", NodeType.DATE_TIME)
        testTomlValue("a" to "1979-05-27T00:32:00-07:00", NodeType.DATE_TIME)
        testTomlValue("a" to "1979-05-27T00:32:00.999999-07:00", NodeType.DATE_TIME)
        testTomlValue("a" to "1979-05-27 07:32:00Z", NodeType.DATE_TIME)
        testTomlValue("a" to "1979-05-27T07:32:00", NodeType.DATE_TIME)
        testTomlValue("a" to "1979-05-27T00:32:00.999999", NodeType.DATE_TIME)
        testTomlValue("a" to "1979-05-27", NodeType.DATE_TIME)
    }

    @Test
    fun nullParsingTest() {
        testTomlValue("a" to "null", NodeType.NULL)
        testTomlValueFail("a" to "null", TomlInputConfig(allowNullValues = false))
    }

    @Test
    fun quotesParsingTest() {
        testTomlValueFail("\"a" to "123")
        testTomlValueFail("a" to  "hello world")
        testTomlValueFail("a" to "\"before \" string\"")
    }

    @Test
    fun specialSymbolsParsing() {
        testTomlValueFail("a" to "\"hello\\world\"")
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\\tworld\"",
            expectedContent = "hello\tworld"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"helloworld\\n\"",
            expectedContent = "helloworld\n"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"helloworld\\\"",
            expectedContent = "helloworld\\"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\\nworld\"",
            expectedContent = "hello\nworld"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\\bworld\"",
            expectedContent = "hello\bworld"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\\rworld\"",
            expectedContent = "hello\rworld"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\\\\tworld\"",
            expectedContent = "hello\\tworld"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\\\\world\"",
            expectedContent = "hello\\world"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello tworld\"",
            expectedContent = "hello tworld"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"hello\t\\\\\\\\world\"",
            expectedContent = "hello\t\\\\world"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"Ɣ is greek\"",
            expectedContent = "Ɣ is greek"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"\\u0194 is greek\"",
            expectedContent = "Ɣ is greek"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"\\U0001F615 is emoji\"",
            expectedContent = "\uD83D\uDE15 is emoji"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"\uD83D\uDE15 is emoji\"",
            expectedContent = "\uD83D\uDE15 is emoji"
        )
        testTomlValueContent(
            keyValuePair = "a" to "\"I'm a string. \\\"You can quote me\\\". Name\\tJos\\u00E9\\nLocation\\tSF.\"",
            expectedContent = "I'm a string. \"You can quote me\". Name\tJosé\nLocation\tSF."
        )
        // regression test related to comments with an equals symbol after it
        testTomlValueContent(
            keyValuePair = "lineCaptureGroup = 1  # index `warningTextHasLine = false`\n".splitKeyValue(0),
            expectedContent = 1L
        )
        testTomlValueContent(
            keyValuePair = "lineCaptureGroup = \"1 = 2\"  # index = `warningTextHasLine = false`\n".splitKeyValue(0),
            expectedContent = "1 = 2"
        )
    }

    @Test
    fun symbolsAfterComment() {
        testTomlValueContent(
            keyValuePair = "test_key = \"test_value\"  # \" some comment".splitKeyValue(0),
            expectedContent = "test_value"
        )
    }

    @Test
    fun parsingIssueValue() {
        shouldThrow<ParseException> { " = false".splitKeyValue(0) }
        shouldThrow<ParseException> { " just false".splitKeyValue(0) }
        testTomlValueFail("a" to  "\"\\hello tworld\"")
        testTomlValueFail("a" to "val\\ue")
        testTomlValueFail("a" to "\\x33")
        testTomlValueFail("a" to "\\UFFFFFFFF")
        testTomlValueFail("a" to "\\U00D80000")
    }
}

enum class NodeType {
    STRING, NULL, INT, FLOAT, BOOLEAN, INCORRECT, LITERAL_STRING, DATE_TIME
}

fun getNodeType(v: TomlValue): NodeType = when (v) {
    is TomlBasicString -> NodeType.STRING
    is TomlNull -> NodeType.NULL
    is TomlLong -> NodeType.INT
    is TomlDouble -> NodeType.FLOAT
    is TomlBoolean -> NodeType.BOOLEAN
    is TomlLiteralString -> NodeType.LITERAL_STRING
    is TomlDateTime -> NodeType.DATE_TIME
    else -> NodeType.INCORRECT
}


fun testTomlValue(
    keyValuePair: Pair<String, String>,
    expectedType: NodeType,
    config: TomlInputConfig = TomlInputConfig()
) {
    val nodeType = getNodeType(TomlKeyValuePrimitive(keyValuePair, 0, config = config).value)
    nodeType shouldBe expectedType
}

fun testTomlValueFail(
    keyValuePair: Pair<String, String>,
    config: TomlInputConfig = TomlInputConfig()
) {
    shouldThrow<ParseException> {
        TomlKeyValuePrimitive(keyValuePair, 0, config = config)
    }
}

fun testTomlValueContent(
    keyValuePair: Pair<String, String>,
    expectedContent: Any
) {
    TomlKeyValuePrimitive(keyValuePair, 0).value.content shouldBe expectedContent
}