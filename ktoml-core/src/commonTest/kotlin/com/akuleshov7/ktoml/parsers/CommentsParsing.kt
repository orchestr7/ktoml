package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import io.kotest.matchers.collections.containOnly
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

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

        val pairA = tableA.children
            .first { it is TomlKeyValuePrimitive }

        tableA.comments should containOnly("comment 1")
        tableA.inlineComment shouldBe "comment 2"

        pairA.comments should containOnly("comment 3")
        pairA.inlineComment shouldBe "comment 4"

        tableB.comments.shouldBeEmpty()
        tableB.inlineComment shouldBe "comment 5"
    }
}
