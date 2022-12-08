package com.akuleshov7.ktoml.parsers

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {

    @Test
    fun testForTakeBeforeComment() {
        var lineWithoutComment = "test_key = \"test_value\"  # \" some comment".takeBeforeComment()
        assertEquals("test_key = \"test_value\"", lineWithoutComment)

        lineWithoutComment = "key = \"\"\"value\"\"\" # \"".takeBeforeComment()
        assertEquals("key = \"\"\"value\"\"\"", lineWithoutComment)

        lineWithoutComment = "key = 123 # \"\"\"abc".takeBeforeComment()
        assertEquals("key = 123", lineWithoutComment)

        lineWithoutComment = "key = \"ab\\\"#cdef\"#123".takeBeforeComment()
        assertEquals("key = \"ab\\\"#cdef\"", lineWithoutComment)

        lineWithoutComment = "  \t#123".takeBeforeComment()
        assertEquals("", lineWithoutComment)

        lineWithoutComment = "key = \"ab\'c\" # ".takeBeforeComment()
        assertEquals("key = \"ab\'c\"", lineWithoutComment)
    }

    @Test
    fun symbolsAfterComment() {
        var comment = "a = \"here#hash\" # my comment".trimComment()
        assertEquals("my comment", comment)

        comment = "a = \"here#\\\"hash\" # my comment".trimComment()
        assertEquals("my comment", comment)

        comment = " # my comment".trimComment()
        assertEquals("my comment", comment)
    }
}