package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TomlTablePrimitive
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class TomlTableTest {
    @Test
    // FixME: https://github.com/akuleshov7/ktoml/issues/30
    fun parsingRegression() {
        val string = """
            [fruits]
            name = "apple"

            [fruits]
            name = "banana"

            [fruits]
            name = "plantain"
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals( 3, parsedToml.children.single().children.size)
    }

    @Test
    @Ignore
    fun nestedTomlTable() {
        val string = """
            [a]
                [a.b]
            
            [a]
            c = 3
            """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()

        assertEquals("""
            | - TomlFile (rootNode)
            |     - TomlTablePrimitive ([a])
            |         - TomlTablePrimitive (    [a.b])
            |             - TomlStubEmptyNode (technical_node)
            |         - TomlKeyValuePrimitive (c=3)
            |
        """.trimMargin(), parsedToml.prettyStr())

    }


    @Test
    fun createTomlTable() {
        val string = """
            [a]
                name = 1
            
            [a.b]
                name = 2
                
            [c.a.b]
                name = 3
            
            ["a.b.c"]
            
            a."a.b.c".d = 10
                
            [c]
                name = 5
            
            [c.a.b.a.b.c]
                test = 3
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
        assertEquals(
            """
                | - TomlFile (rootNode)
                |     - TomlTablePrimitive ([a])
                |         - TomlKeyValuePrimitive (name=1)
                |         - TomlTablePrimitive ([a.b])
                |             - TomlKeyValuePrimitive (name=2)
                |     - TomlTablePrimitive ([c])
                |         - TomlTablePrimitive ([c.a])
                |             - TomlTablePrimitive ([c.a.b])
                |                 - TomlKeyValuePrimitive (name=3)
                |                 - TomlTablePrimitive ([c.a.b.a])
                |                     - TomlTablePrimitive ([c.a.b.a.b])
                |                         - TomlTablePrimitive ([c.a.b.a.b.c])
                |                             - TomlKeyValuePrimitive (test=3)
                |         - TomlKeyValuePrimitive (name=5)
                |     - TomlTablePrimitive (["a.b.c"])
                |         - TomlTablePrimitive (["a.b.c".a])
                |             - TomlTablePrimitive (["a.b.c".a."a.b.c"])
                |                 - TomlKeyValuePrimitive (d=10)
                |
        """.trimMargin(),
            parsedToml.prettyStr()
        )
    }

    @Test
    fun emptyTable() {
        val string = """
            [test]
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()
    }

    @Test
    fun createSimpleTomlTable() {
        val table = TomlTablePrimitive("[abcd]", 0)
        assertEquals(table.fullTableName, "abcd")
    }
}
