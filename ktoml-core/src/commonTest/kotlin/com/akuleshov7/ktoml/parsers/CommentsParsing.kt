package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CommentsParsing {
    @Test
    fun commentsParsing() {
        val string = """
            # comment 1
            [a] # comment 2
            # comment 3
             test = 1 # comment 4
             [[a.b]] # comment 5 
                test = 1
        """.trimIndent()
        val parsedToml = Toml.tomlParser.parseString(string)
        parsedToml.prettyPrint()

        val tableA = parsedToml.findTableInAstByName("a")!!
        val tableB = tableA.findTableInAstByName("a.b")?.getFirstChild()!!

        val pairA =
                tableA.children
                    .first { it is TomlKeyValuePrimitive }

        assertContentEquals(
            listOf("comment 1"),
            tableA.comments
        )

        assertEquals(
            "comment 2",
            tableA.inlineComment
        )

        assertContentEquals(
            listOf("comment 3"),
            pairA.comments
        )

        assertEquals(
            "comment 4",
            pairA.inlineComment
        )

        assertEquals(
            "comment 5",
            tableB.inlineComment
        )
    }
}
