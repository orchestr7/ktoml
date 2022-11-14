package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DottedKeyParserTest {
    @Test
    fun positiveParsingTest() {
        var test = TomlKey("\"a.b.c\"", 0)
        assertEquals("a.b.c", test.content)
        assertEquals(false, test.isDotted)

        test = TomlKey("\"a.b.c\".b.c", 0)
        assertEquals("c", test.content)
        assertEquals(listOf("\"a.b.c\"", "b", "c"), test.keyParts)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"a\".b.c", 0)
        assertEquals("c", test.content)
        assertEquals(listOf("\"a\"", "b", "c"), test.keyParts)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"  a  \"", 0)
        assertEquals("a", test.content)
        assertEquals(false, test.isDotted)

        test = TomlKey("a.b.c", 0)
        assertEquals("c", test.content)
        assertEquals(true, test.isDotted)

        test = TomlKey("a.\"  b  .c \"", 0)
        assertEquals("b  .c", test.content)
        assertEquals(true, test.isDotted)

        test = TomlKey("a  .  b .  c ", 0)
        assertEquals("c", test.content)
        assertEquals(true, test.isDotted)

        assertFailsWith<ParseException> { TomlKey("SPACE AND SPACE", 0) }
    }

    @Test
    fun createTable() {
        var test = TomlKeyValuePrimitive(Pair("google.com","5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("google", test.fullTableName)

        test = TomlKeyValuePrimitive(Pair("a.b.c.d", "5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("a.b.c", test.fullTableName)

        val testKeyValue = TomlKeyValuePrimitive(Pair("a.b.c", "5"), 0)
        assertEquals("c", testKeyValue.key.content)
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
                |     - TomlTablePrimitive (["a.b.c"])
                |         - TomlTablePrimitive (["a.b.c".f])
                |             - TomlTablePrimitive (["a.b.c".f.e])
                |                 - TomlTablePrimitive (["a.b.c".f.e.g])
                |                     - TomlTablePrimitive (["a.b.c".f.e.g."a.b.c"])
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
                 |     - TomlTablePrimitive ([a])
                 |         - TomlTablePrimitive ([a."a.b.c"])
                 |             - TomlKeyValuePrimitive (d=10)
                 |     - TomlTablePrimitive (["a.b.c"])
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
                 |     - TomlTablePrimitive (["a.b.c"])
                 |         - TomlTablePrimitive (["a.b.c".a])
                 |             - TomlTablePrimitive (["a.b.c".a."a.b.c"])
                 |                 - TomlKeyValuePrimitive (d=10)
                 |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }
}
