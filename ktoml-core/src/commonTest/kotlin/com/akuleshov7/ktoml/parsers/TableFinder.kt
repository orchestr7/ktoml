package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.utils.findPrimitiveTableInAstByName
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

        val primitiveTable = findPrimitiveTableInAstByName(listOf(parsedToml), "a.b.d.e.f")
        primitiveTable.shouldNotBeNull()
        primitiveTable.fullTableKey.toString() shouldBe "a.b.d.e.f"
    }
}
