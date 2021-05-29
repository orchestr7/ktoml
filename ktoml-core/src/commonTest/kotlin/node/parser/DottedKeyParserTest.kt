package com.akuleshov7.ktoml.test.node.parser

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.node.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith



class KeyParserTest {
    @Test
    fun positiveParsingTest() {
        var test = TomlKey("\"a.b.c\"", 0)
        assertEquals("a.b.c", test.content)
        assertEquals(false, test.isDotted)

        test = TomlKey("\"a\".b.c", 0)
        assertEquals("\"a\".b.c", test.content)
        assertEquals(true, test.isDotted)

        test = TomlKey("\"  a  \"", 0)
        assertEquals("  a  ", test.content)
        assertEquals(false, test.isDotted)

        test = TomlKey("a.b.c", 0)
        assertEquals("a.b.c", test.content)
        assertEquals(true, test.isDotted)

        test = TomlKey("a.\"  b  \".c", 0)
        assertEquals("a.\"  b  \".c", test.content)
        assertEquals(true, test.isDotted)
    }
}

