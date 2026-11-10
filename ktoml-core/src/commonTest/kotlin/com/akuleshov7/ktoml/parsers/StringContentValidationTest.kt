package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class StringContentValidationTest {
    private val parser = TomlParser(TomlInputConfig.compliant())

    @Test
    fun rejectsRawControlCharsInParsedSource() {
        listOf(
            "value = \"ok\" # \u0000\n",
            "\u000C",
            "value = \"bad\u007F\"\n",
            "value = 'bad\u001F'\n",
            "value = \"\"\"bad\u0000\"\"\"\n",
            "value = '''bad\u001F'''\n",
            "value = 1\r",
        ).forEach { toml ->
            assertFailsWith<ParseException> {
                parser.parseString(toml)
            }
        }
    }

    @Test
    fun allowsTabsAndMultilineLineFeeds() {
        parser.parseString(
            "basic = \"a\tb\" # \t\n" +
                    "literal = 'a\tb'\n" +
                    "multi = \"\"\"a\nb\"\"\"\n" +
                    "rawmulti = '''a\nb'''\n"
        )
    }

    @Test
    fun rejectsInvalidBasicStringEscapes() {
        listOf(
            "\"" + "\\" + "\"",
            "\"\\uD801\"",
            "\"\\xG0\"",
            "\"\"\"" + "\\" + "\"\"\"",
        ).forEach { value ->
            assertFailsWith<ParseException> {
                TomlKeyValuePrimitive("a" to value, 1)
            }
        }
    }

    @Test
    fun allowsValidBasicStringEscapes() {
        val value = "\"\\b\\t\\n\\f\\r\\\"\\\\\\u0041\\U0001F615\\x21\\e\""
        val parsed = TomlKeyValuePrimitive("a" to value, 1)

        assertEquals("\b\t\n\u000C\r\"\\A\uD83D\uDE15!\u001B", parsed.value.content)
    }
}
