package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.TomlFile
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DottedKeyParserTest {
    @Test
    fun positiveParsingTest() {
        var test = TomlKey("\"a.b.c\"", 0)
        test.last() shouldBe "a.b.c"
        test.isDotted.shouldBeFalse()

        test = TomlKey("\"a.b.c\".b.c", 0)
        test.last() shouldBe "c"
        test.keyParts shouldBe listOf("\"a.b.c\"", "b", "c")
        test.isDotted.shouldBeTrue()

        test = TomlKey("\"a\".b.c", 0)
        test.last() shouldBe "c"
        test.keyParts shouldBe listOf("\"a\"", "b", "c")
        test.isDotted.shouldBeTrue()

        test = TomlKey("\"  a  \"", 0)
        test.last() shouldBe "a"
        test.isDotted.shouldBeFalse()

        test = TomlKey("a.b.c", 0)
        test.last() shouldBe "c"
        test.isDotted.shouldBeTrue()

        test = TomlKey("a.\"  b  .c \"", 0)
        test.last() shouldBe "b  .c"
        test.isDotted.shouldBeTrue()

        test = TomlKey("a  .  b .  c ", 0)
        test.last() shouldBe "c"
        test.isDotted.shouldBeTrue()

        shouldThrow<ParseException> {
            TomlKey("SPACE AND SPACE", 0)
        }
    }

    @Test
    fun createTable() {
        var test = TomlKeyValuePrimitive(Pair("google.com","5"), 0).createTomlTableFromDottedKey(TomlFile())
        test.fullTableKey.toString() shouldBe "google"

        test = TomlKeyValuePrimitive(Pair("a.b.c.d", "5"), 0).createTomlTableFromDottedKey(TomlFile())
        test.fullTableKey.toString() shouldBe "a.b.c"

        val testKeyValue = TomlKeyValuePrimitive(Pair("a.b.c", "5"), 0)
        testKeyValue.key.last() shouldBe "c"
    }

    @Test
    fun parseDottedKey1() {
        val string = """
            ["a.b.c"]
            f.e.g."a.b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()

        parsedToml.prettyStr() shouldBe """
                | - TomlFile (rootNode)
                |     - TomlTable (["a.b.c"])
                |         - TomlTable (["a.b.c".f])
                |             - TomlTable (["a.b.c".f.e])
                |                 - TomlTable (["a.b.c".f.e.g])
                |                     - TomlTable (["a.b.c".f.e.g."a.b.c"])
                |                         - TomlKeyValuePrimitive (d=10)
                |
        """.trimMargin()
    }

    @Test
    fun parseDottedKey2() {
        val string = """
            a."a.b.c".d = 10
            ["a.b.c"]
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()

        parsedToml.prettyStr() shouldBe """
                 | - TomlFile (rootNode)
                 |     - TomlTable ([a])
                 |         - TomlTable ([a."a.b.c"])
                 |             - TomlKeyValuePrimitive (d=10)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlStubEmptyNode (technical_node)
                 |
        """.trimMargin()
    }

    @Test
    fun parseSimpleDottedKey() {
        val string = """
            ["a.b.c"]
                a."a.b.c".d = 10
        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()

        parsedToml.prettyStr() shouldBe """
                 | - TomlFile (rootNode)
                 |     - TomlTable (["a.b.c"])
                 |         - TomlTable (["a.b.c".a])
                 |             - TomlTable (["a.b.c".a."a.b.c"])
                 |                 - TomlKeyValuePrimitive (d=10)
                 |
        """.trimMargin()
    }
}
