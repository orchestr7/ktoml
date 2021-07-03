package node.parser

import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.parsers.node.TomlArray
import com.akuleshov7.ktoml.parsers.node.TomlBasicString
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonParserTest {
    @Test
    fun parseArray() {
        var highLevelValues = 0
        var midLevelValues = 0
        var lowLevelValues = 0

        TomlArray("[[\"a\", [\"b\"]], \"c\", \"d\"]", 0).parse().forEach {
            when (it) {
                is TomlBasicString -> highLevelValues++
                is List<*> -> it.forEach {
                    when (it) {
                        is TomlBasicString -> midLevelValues++
                        is List<*> -> it.forEach {
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
