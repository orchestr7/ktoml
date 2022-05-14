package com.akuleshov7.ktoml.writers

import kotlin.test.Test

class CommentWriteTest {
    @Test
    fun commentWriteTest() {
        val toml = """
            # Comment
            # Comment
            x = 0 # Comment
            
            # Comment
            [a] # Comment
                b = [ ] # Comment
            
                # Comment
                [a.c]
                    d = 1 # Comment
            
            # Comment
            [[e]] # Comment
        """.trimIndent()

        testTable(toml)
    }
}