package com.akuleshov7.ktoml.parsers

import kotlin.test.Test
import kotlin.test.assertEquals

class StringUtilsTest {
    @Test
    fun testForConvertLineEndingBackslashKeepsEscapedBackslashNewline() {
        val multilineString = "a \\\\" + "\n" + "b"
        assertEquals(multilineString, multilineString.convertLineEndingBackslash())
    }

    @Test
    fun testForConvertLineEndingBackslashTrimsWhitespaceBeforeAndAfterNewline() {
        val multilineString = "fox jumps over \\\t  \n   the lazy dog."
        assertEquals("fox jumps over the lazy dog.", multilineString.convertLineEndingBackslash())
    }

    @Test
    fun testForConvertLineEndingBackslashSkipsAllTrailingWhitespace() {
        val multilineString = "geeee\\  \n\n\n      "
        assertEquals("geeee", multilineString.convertLineEndingBackslash())
    }

    @Test
    fun testForConvertLineEndingBackslashHandlesCrLf() {
        val multilineString = "hello\\  \r\n\t world"
        assertEquals("helloworld", multilineString.convertLineEndingBackslash())
    }

    @Test
    fun testForConvertLineEndingBackslashKeepsTrailingSpacesWithoutNewline() {
        val multilineString = "t\\ "
        assertEquals(multilineString, multilineString.convertLineEndingBackslash())
    }

    @Test
    fun testForTakeBeforeComment() {
        var lineWithoutComment = "test_key = \"test_value\"# \" some comment".takeBeforeComment(false)
        assertEquals("test_key = \"test_value\"", lineWithoutComment)

        lineWithoutComment = "key = \"\"\"value\"\"\"# \"".takeBeforeComment(false)
        assertEquals("key = \"\"\"value\"\"\"", lineWithoutComment)

        lineWithoutComment = "key = 123# \"\"\"abc".takeBeforeComment(false)
        assertEquals("key = 123", lineWithoutComment)

        lineWithoutComment = "key = \"ab\\\"#cdef\"#123".takeBeforeComment(false)
        assertEquals("key = \"ab\\\"#cdef\"", lineWithoutComment)

        lineWithoutComment = "#123".takeBeforeComment(false)
        assertEquals("", lineWithoutComment)

        lineWithoutComment = "key = \"ab\'c\"# ".takeBeforeComment(false)
        assertEquals("key = \"ab\'c\"", lineWithoutComment)

        lineWithoutComment = """
            a = 'C:\some\path\'#\abc
        """.trimIndent().takeBeforeComment(true)
        assertEquals("""a = 'C:\some\path\'""", lineWithoutComment)
    }

    @Test
    fun testForTrimComment() {
        var comment = "a = \"here#hash\" # my comment".trimComment(false)
        assertEquals("my comment", comment)

        comment = "a = \"here#\\\"hash\" # my comment".trimComment(false)
        assertEquals("my comment", comment)

        comment = " # my comment".trimComment(false)
        assertEquals("my comment", comment)

        comment = """
            a = 'C:\some\path\' #\abc
        """.trimIndent().trimComment(true)
        assertEquals("\\abc", comment)
    }
}
