package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TableType
import com.akuleshov7.ktoml.tree.nodes.TomlTable
import io.kotest.matchers.collections.haveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

        parsedToml.children.single().children should haveSize(3)
    }

    @Test
    fun nestedTomlTable() {
        val string = """
            [a]
                [a.b]
            
            [a]
            c = 3
            """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()

        parsedToml.prettyStr() shouldBe """
            | - TomlFile (rootNode)
            |     - TomlTable ([a])
            |         - TomlStubEmptyNode (technical_node)
            |         - TomlTable ([a.b])
            |             - TomlStubEmptyNode (technical_node)
            |         - TomlKeyValuePrimitive (c=3)
            |
        """.trimMargin()
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

        parsedToml.prettyStr() shouldBe """
                | - TomlFile (rootNode)
                |     - TomlTable ([a])
                |         - TomlKeyValuePrimitive (name=1)
                |         - TomlTable ([a.b])
                |             - TomlKeyValuePrimitive (name=2)
                |     - TomlTable ([c])
                |         - TomlTable ([c.a])
                |             - TomlTable ([c.a.b])
                |                 - TomlKeyValuePrimitive (name=3)
                |                 - TomlTable ([c.a.b.a])
                |                     - TomlTable ([c.a.b.a.b])
                |                         - TomlTable ([c.a.b.a.b.c])
                |                             - TomlKeyValuePrimitive (test=3)
                |         - TomlKeyValuePrimitive (name=5)
                |     - TomlTable (["a.b.c"])
                |         - TomlTable (["a.b.c".a])
                |             - TomlTable (["a.b.c".a."a.b.c"])
                |                 - TomlKeyValuePrimitive (d=10)
                |
        """.trimMargin()
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
        val table = TomlTable("[abcd]", 0, TableType.PRIMITIVE)
        table.fullTableKey.toString() shouldBe "abcd"
    }
}
