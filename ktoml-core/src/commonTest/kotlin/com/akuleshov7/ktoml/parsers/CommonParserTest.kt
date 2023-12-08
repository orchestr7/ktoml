package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlArray
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlBasicString
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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
                is List<*> -> it.forEach {
                    when (it) {
                        is TomlBasicString -> midLevelValues++
                        is List<*> -> it.forEach { _ ->
                            lowLevelValues++
                        }
                    }
                }
            }
        }

        // highLevelValues = a and b
        // midLevelValues = c
        // lowLevelValues = d
        lowLevelValues shouldBe 1
        midLevelValues shouldBe 1
        highLevelValues shouldBe 2
    }
}
