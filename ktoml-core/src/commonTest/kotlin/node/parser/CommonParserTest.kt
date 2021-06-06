package node.parser

import com.akuleshov7.ktoml.parsers.TomlParser
import kotlin.test.Test

class CommonParserTest {
    @Test
    fun parseDottedKey() {
        val result = TomlParser("a.\"a.b.c\".b.d = 123").parseString()
        result.prettyPrint()

        throw IllegalArgumentException()
    }
}