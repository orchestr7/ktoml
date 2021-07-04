package com.akuleshov7.ktoml.test.node.parser

import com.akuleshov7.ktoml.parsers.node.*
import kotlin.test.Test
import kotlin.test.assertEquals

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

        test = TomlKey("a.\"  b .c \"", 0)
        assertEquals("b .c", test.content)
        assertEquals(true, test.isDotted)

        test = TomlKey("a  .  b .  c ", 0)
        assertEquals("c", test.content)
        assertEquals(true, test.isDotted)
    }

    @Test
    fun createTable() {
        var test = TomlKeyValueSimple(Pair("google.com","5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("google", test.fullTableName)

        test = TomlKeyValueSimple(Pair("a.b.c.d", "5"), 0).createTomlTableFromDottedKey(TomlFile())
        assertEquals("a.b.c", test.fullTableName)

        val testKeyValue = TomlKeyValueSimple(Pair("a.b.c", "5"), 0)
        test = testKeyValue.createTomlTableFromDottedKey(TomlFile())
        assertEquals("c", testKeyValue.key.content)
        assertEquals(1, test.level)
    }
}
