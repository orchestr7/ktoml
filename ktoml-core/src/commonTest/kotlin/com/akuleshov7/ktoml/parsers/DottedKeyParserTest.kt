package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DottedKeyParserTest {
    @Test
    fun positiveParsingTest() {
        var test = TomlKey("\"a.b.c\"", 0)
        assertEquals("a.b.c", test.last())
        assertEquals(false, test.isDotted)

        test = TomlKey("\"a.b.c\".b.c", 0)
        assertEquals("c", test.last())
        assertEquals(listOf("\"a.b.c\"", "b", "c"), test.keyParts)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"a\".b.c", 0)
        assertEquals("c", test.last())
        assertEquals(listOf("\"a\"", "b", "c"), test.keyParts)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"  a  \"", 0)
        assertEquals("a", test.last())
        assertEquals(false, test.isDotted)

        test = TomlKey("a.b.c", 0)
        assertEquals("c", test.last())
        assertEquals(true, test.isDotted)

        test = TomlKey("a.\"  b  .c \"", 0)
        assertEquals("b  .c", test.last())
        assertEquals(true, test.isDotted)

        test = TomlKey("a  .  b .  c ", 0)
        assertEquals("c", test.last())
        assertEquals(true, test.isDotted)

        assertFailsWith<ParseException> { TomlKey("SPACE AND SPACE", 0) }
    }

    @Test
    fun createTable() {
        var test = TomlKeyValuePrimitive(Pair("google.com","5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("google", test.fullTableKey.toString())

        test = TomlKeyValuePrimitive(Pair("a.b.c.d", "5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("a.b.c", test.fullTableKey.toString())

        val testKeyValue = TomlKeyValuePrimitive(Pair("a.b.c", "5"), 0)
        assertEquals("c", testKeyValue.key.last())
    }

    @Test
    fun parseDottedKey1() {
        val string = """
            ["a.b.c"]
            f.e.g."a.b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTable (["a.b.c"])
                |         - TomlTable (["a.b.c".f])
                |             - TomlTable (["a.b.c".f.e])
                |                 - TomlTable (["a.b.c".f.e.g])
                |                     - TomlTable (["a.b.c".f.e.g."a.b.c"])
                |                         - TomlKeyValuePrimitive (d=10)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parseDottedKey2() {
        val string = """
            a."a.b.c".d = 10
            ["a.b.c"]
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable ([a])
                 |         - TomlTable ([a."a.b.c"])
                 |             - TomlKeyValuePrimitive (d=10)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlStubEmptyNode (technical_node)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun parseSimpleDottedKey() {
        val string = """
            ["a.b.c"]
                a."a.b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                 | - TomlFile (rootNode)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlTable (["a.b.c".a])
                 |             - TomlTable (["a.b.c".a."a.b.c"])
                 |                 - TomlKeyValuePrimitive (d=10)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }
}
