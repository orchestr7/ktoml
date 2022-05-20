package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.tree.TomlInlineTable
import com.akuleshov7.ktoml.tree.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.TomlNode
import kotlin.test.Test
import kotlin.test.assertEquals

class KeyValueWriteTest {
    @Test
    fun primitiveKeyValueWriteTest() {
        val bareKey = "a"
        val dottedKey = "a.\"b.c\".d"

        // Strings

        val literalString = """'"string"'"""

        testTomlPrimitivePair(bareKey to literalString)
        testTomlPrimitivePair(dottedKey to literalString)

        val basicString = """"string""""

        testTomlPrimitivePair(bareKey to basicString)
        testTomlPrimitivePair(dottedKey to basicString)

        // Integer
        testTomlPrimitivePair(bareKey to "123456")
        testTomlPrimitivePair(dottedKey to "123456")

        // Float
        testTomlPrimitivePair(bareKey to "12.3456")
        testTomlPrimitivePair(dottedKey to "12.3456")

        // Boolean
        testTomlPrimitivePair(bareKey to "true")
        testTomlPrimitivePair(dottedKey to "true")

        // Date-Times

        testTomlPrimitivePair(bareKey to "1979-05-27T07:32:00Z")
        testTomlPrimitivePair(dottedKey to "1979-05-27T07:32:00Z")

        testTomlPrimitivePair(bareKey to "1979-05-27T07:32")
        testTomlPrimitivePair(dottedKey to "1979-05-27T07:32")

        testTomlPrimitivePair(bareKey to "1979-05-27")
        testTomlPrimitivePair(dottedKey to "1979-05-27")

        // Null
        testTomlPrimitivePair(bareKey to "null", TomlConfig(allowNullValues = true))
        testTomlPrimitivePair(dottedKey to "null", TomlConfig(allowNullValues = true))
    }

    @Test
    fun arrayKeyValueWriteTest() {
        val bareKey = "a"
        val dottedKey = "a.\"b.c\".d"

        // Single-line

        val singleLineArray = """[ 1, "string", [ 3.14 ] ]"""

        testTomlArrayPair(bareKey to singleLineArray, multiline = false)
        testTomlArrayPair(dottedKey to singleLineArray, multiline = false)

        // Multi-line

        val multiLineArray = """
        [
            1,
            "string",
            [
                3.14
            ]
        ]
        """.trimIndent()

        testTomlArrayPair(bareKey to multiLineArray, multiline = true)
        testTomlArrayPair(dottedKey to multiLineArray, multiline = true)
    }

    @Test
    fun inlineTableKeyValueWriteTest() {
        val key = "inlineTable"
        val value = """{ a = "string", b = 3.14, c = 1 }"""

        testTomlInlineTablePair(key to value)
    }
}

fun testTomlPrimitivePair(
    pair: Pair<String, String>,
    config: TomlConfig = TomlConfig()
) = testTomlPair(
    TomlKeyValuePrimitive(pair, 0, config = config),
    expectedString = "${pair.first} = ${pair.second}",
    config,
    multiline = false
)

fun testTomlArrayPair(
    pair: Pair<String, String>,
    multiline: Boolean,
    config: TomlConfig = TomlConfig(),
) = testTomlPair(
    TomlKeyValueArray(pair, 0, config = config),
    expectedString = "${pair.first} = ${pair.second}",
    config,
    multiline
)

fun testTomlInlineTablePair(
    pair: Pair<String, String>,
    config: TomlConfig = TomlConfig(),
) = testTomlPair(
    TomlInlineTable(pair, 0, config = config),
    expectedString = "${pair.first} = ${pair.second}",
    config,
    multiline = false
)

fun testTomlPair(
    pair: TomlNode,
    expectedString: String,
    config: TomlConfig,
    multiline: Boolean
) {
    assertEquals(
        expectedString,
        actual = buildString {
            val emitter = TomlStringEmitter(this, config)

            pair.write(emitter, config, multiline)
        }
    )
}