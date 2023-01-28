package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.TomlInlineTable
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
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
        testTomlPrimitivePair(bareKey to "null", TomlInputConfig(allowNullValues = true))
        testTomlPrimitivePair(dottedKey to "null", TomlInputConfig(allowNullValues = true))
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
    inputConfig: TomlInputConfig = TomlInputConfig(),
    outputConfig: TomlOutputConfig = TomlOutputConfig()
) = testTomlPair(
    TomlKeyValuePrimitive(pair, 0, config = inputConfig),
    expectedString = "${pair.first} = ${pair.second}",
    outputConfig
)

fun testTomlArrayPair(
    pair: Pair<String, String>,
    multiline: Boolean,
    inputConfig: TomlInputConfig = TomlInputConfig(),
    outputConfig: TomlOutputConfig = TomlOutputConfig()
) = testTomlPair(
    TomlKeyValueArray(pair, 0, config = inputConfig).also {
        val array = it.value as TomlArray

        array.multiline = multiline
    },
    expectedString = "${pair.first} = ${pair.second}",
    outputConfig
)

fun testTomlInlineTablePair(
    pair: Pair<String, String>,
    inputConfig: TomlInputConfig = TomlInputConfig(),
    outputConfig: TomlOutputConfig = TomlOutputConfig()
) = testTomlPair(
    TomlInlineTable(pair, 0, config = inputConfig),
    expectedString = "${pair.first} = ${pair.second}",
    outputConfig
)

fun testTomlPair(
    pair: TomlNode,
    expectedString: String,
    config: TomlOutputConfig = TomlOutputConfig()
) {
    assertEquals(
        expectedString,
        actual = buildString {
            val emitter = TomlStringEmitter(this, config)

            pair.write(emitter, config)
        }
    )
}