package node.parser

import com.akuleshov7.ktoml.parsers.node.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

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

    @Test
    fun parsingIssueValue() {
        assertFails{  TomlKeyValue("   a  =", 0)  }
        assertFails{  TomlKeyValue("   a  =", 0)  }
        assertFails{  TomlKeyValue("   a  = b = c", 0)  }
        assertFails{  TomlKeyValue(" = false",0)  }
        assertFails{  TomlKeyValue(" just false",0)  }
        assertFails{  TomlKeyValue(" a = # dfg",0)  }
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





