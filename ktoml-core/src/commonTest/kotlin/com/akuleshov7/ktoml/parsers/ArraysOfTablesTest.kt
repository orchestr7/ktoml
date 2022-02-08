package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml.Default.tomlParser
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlKey
import com.akuleshov7.ktoml.tree.TomlKeyValuePrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArraysOfTablesTest {
    @Test
    fun positiveParsingTest() {
        val string = """
            [[fruits]]
            name = "apple"

            [[fruits]]
            name = "banana"

            [[fruits]]
            name = "plantain"
        """.trimIndent()

        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()
    }

    @Test
    fun parsingRegression() {
        val string = """
            [fruits]
            name = "apple"

            [fruits]
            name = "banana"

            [fruits]
            name = "plantain"
        """.trimIndent()

        // FIXME we SHOULD raise error! Need to have the test for the deserialization
        // We have dublication of tables and no override for fields
        val parsedToml = tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertTrue { false }
    }
}
