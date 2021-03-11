package node.parser

import com.akuleshov7.parsers.node.*
import kotlin.test.Test
import kotlin.test.assertEquals

enum class NodeType {
    STRING, NULL, INT, FLOAT, BOOLEAN, INCORRECT
}

class ValueParserTest {
    @Test
    fun parsingTest() {
        testTomlValue("a = gfhgfhfg", NodeType.STRING)
        testTomlValue("a = 123456", NodeType.INT)
        testTomlValue("a = 12.2345", NodeType.FLOAT)
        testTomlValue("a = true", NodeType.BOOLEAN)
        testTomlValue("a = false", NodeType.BOOLEAN)
    }
}

fun getNodeType(v: TomlValue): NodeType = when (v) {
    is TomlString -> NodeType.STRING
    is TomlNull -> NodeType.NULL
    is TomlInt -> NodeType.INT
    is TomlFloat -> NodeType.FLOAT
    is TomlBoolean -> NodeType.BOOLEAN
    else -> NodeType.INCORRECT
}

fun testTomlValue(s: String, expectedType: NodeType) {
    assertEquals(expectedType, getNodeType(TomlKeyValue(s, 0).value))
}





