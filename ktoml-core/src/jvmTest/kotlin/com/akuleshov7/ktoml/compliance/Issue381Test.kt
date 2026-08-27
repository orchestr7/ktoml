package com.akuleshov7.ktoml.compliance

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.parsers.TomlParser
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class Issue381Test {
    @Test
    fun parsesNestedMultilineArrays() {
        val toml = """
            nest = [
            	[
            		["a"],
            		[1, 2, [3]]
            	]
            ]
        """.trimIndent()

        val actualJson = TomlTestConverter.toJson(TomlParser(TomlInputConfig.compliant()).parseString(toml))
        val expectedJson = Json.parseToJsonElement(
            """
            {"nest":[[[{"type":"string","value":"a"}],[{"type":"integer","value":"1"},{"type":"integer","value":"2"},[{"type":"integer","value":"3"}]]]]}
            """.trimIndent()
        )

        assertEquals(expectedJson, actualJson)
    }

    @Test
    fun parsesMultilineArraysWithInlineComments() {
        val toml = """
            arr5 = [[[[#["#"],
            ["#"]]]]#]
            ]
        """.trimIndent()

        val actualJson = TomlTestConverter.toJson(TomlParser(TomlInputConfig.compliant()).parseString(toml))
        val expectedJson = Json.parseToJsonElement(
            """
            {"arr5":[[[[[{"type":"string","value":"#"}]]]]]}
            """.trimIndent()
        )

        assertEquals(expectedJson, actualJson)
    }
}
