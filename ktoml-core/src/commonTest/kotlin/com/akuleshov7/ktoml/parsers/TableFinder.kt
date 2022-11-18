package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.utils.findPrimitiveTableInAstByName
import kotlin.test.Test
import kotlin.test.assertEquals

class TableFinder {
    @Test
    fun findTableTest() {
        val string = """
            [a]
             test = 1
             [a.b]
                test = 1
                [a.b.c]
                   test = 1
                [a.b.d]
                   test = 1
                   [a.b.d.e.f.g]
             [a.c]
                test = 1
                [a.c.e.f.g.h]
             [a.d]
                test = 1


        """.trimIndent()

        val parsedToml = Toml.tomlParser.parseString(string)
        assertEquals("a.b.d.e.f", findPrimitiveTableInAstByName(listOf(parsedToml), "a.b.d.e.f")?.fullTableKey.toString())
    }
}
