package com.akuleshov7.ktoml.parsers

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import kotlin.test.Test

class StringUtilsTest {
    @Test
    fun testForTakeBeforeComment() {
        var lineWithoutComment = "test_key = \"test_value\"  # \" some comment".takeBeforeComment(false)
        lineWithoutComment shouldBe "test_key = \"test_value\""

        lineWithoutComment = "key = \"\"\"value\"\"\" # \"".takeBeforeComment(false)
        lineWithoutComment shouldBe "key = \"\"\"value\"\"\""

        lineWithoutComment = "key = 123 # \"\"\"abc".takeBeforeComment(false)
        lineWithoutComment shouldBe "key = 123"

        lineWithoutComment = "key = \"ab\\\"#cdef\"#123".takeBeforeComment(false)
        lineWithoutComment shouldBe "key = \"ab\\\"#cdef\""

        lineWithoutComment = "  \t#123".takeBeforeComment(false)
        lineWithoutComment.shouldBeEmpty()

        lineWithoutComment = "key = \"ab\'c\" # ".takeBeforeComment(false)
        lineWithoutComment shouldBe "key = \"ab\'c\""

        lineWithoutComment = """
            a = 'C:\some\path\' #\abc
        """.trimIndent().takeBeforeComment(true)
        lineWithoutComment shouldBe """a = 'C:\some\path\'"""
    }

    @Test
    fun testForTrimComment() {
        var comment = "a = \"here#hash\" # my comment".trimComment(false)
        comment shouldBe "my comment"

        comment = "a = \"here#\\\"hash\" # my comment".trimComment(false)
        comment shouldBe "my comment"

        comment = " # my comment".trimComment(false)
        comment shouldBe "my comment"

        comment = """
            a = 'C:\some\path\' #\abc
        """.trimIndent().trimComment(true)
        comment shouldBe "\\abc"
    }
}
