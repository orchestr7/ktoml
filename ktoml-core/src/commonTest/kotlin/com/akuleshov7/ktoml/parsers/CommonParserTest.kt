package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonParserTest {
    @Test
    fun parseArray() {
        var highLevelValues = 0
        var midLevelValues = 0
        var lowLevelValues = 0

        @Suppress("UNCHECKED_CAST")
        (TomlArray("[[\"a\", [\"b\"]], \"c\", \"d\"]", 0, TomlInputConfig()).content as List<Any>).forEach {
            when (it) {
                is TomlBasicString -> highLevelValues++
                is TomlArray -> (it.content as List<Any>).forEach {
                    when (it) {
                        is TomlBasicString -> midLevelValues++
                        is TomlArray -> (it.content as List<Any>).forEach { _ ->
                            lowLevelValues++
                        }
                    }
                }
            }
        }

        // highLevelValues = a and b
        // midLevelValues = c
        // lowLevelValues = d
        assertEquals(1, lowLevelValues)
        assertEquals(1, midLevelValues)
        assertEquals(2, highLevelValues)
    }
}
