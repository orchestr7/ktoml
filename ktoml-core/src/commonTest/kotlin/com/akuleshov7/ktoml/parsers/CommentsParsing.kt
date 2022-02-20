package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
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
    }
}
