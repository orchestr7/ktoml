package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LiteralMultilineStringDecoderTest {

    @Serializable
    data class SimpleString(val a: String)

    @Serializable
    data class SimpleStrings(val a: String, val b: String)

    @Serializable
    data class StringAndInt(val a: String, val b: Int)

    @Serializable
    data class StringAndInts(val a: String, val b: Int, val c: Int)

    @Test
    fun testMultilineLiteralStringDecode() {
        var test = """
            a = '''abc'''
        """.trimIndent()
        assertEquals(SimpleString("abc"), Toml.decodeFromString(test))

        test = """
            a = '''
            first line
            second line'''
        """.trimIndent()
        assertEquals(SimpleString("first line\nsecond line"), Toml.decodeFromString(test))

        test = """
            a = '''first line
            second line'''
        """.trimIndent()
        assertEquals(SimpleString("first line\nsecond line"), Toml.decodeFromString(test))

        test = """
            a = '''first line

            second line'''
        """.trimIndent()
        assertEquals(SimpleString("first line\n\nsecond line"), Toml.decodeFromString(test))

        test = "a = '''first line\n\t  \nsecond line'''"
        assertEquals(SimpleString("first line\n\t  \nsecond line"), Toml.decodeFromString(test))

        test = """
            a = '''
            Roses are red
            Violets are blue'''
        """.trimIndent()
        assertEquals(SimpleString("Roses are red\nViolets are blue"), Toml.decodeFromString(test))

        test = """
            a = '''
            
            Test'''
            b = 2
        """.trimIndent()
        assertEquals(
            StringAndInt("\nTest", 2),
            Toml.decodeFromString(test)
        )

        test = """
            a = '''Test
            b = 32
            '''
        """.trimIndent()
        assertEquals(
            SimpleString(a = "Test\nb = 32\n"),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun lineEndingBackslash() {
        var test = """
            a = '''
            first line \
            second line \
            
            third line'''
        """.trimIndent()
        assertEquals(SimpleString("first line second line third line"), Toml.decodeFromString(test))

        test = """
            a = '''
            The quick brown \
            
                fox jumps over \
                 the lazy dog.'''
        """.trimIndent()
        assertEquals(SimpleString("The quick brown fox jumps over the lazy dog."), Toml.decodeFromString(test))

        test = """
            a = '''\
                The quick brown \
                fox jumps over \
                the lazy dog.\
                    '''
        """.trimIndent()
        assertEquals(SimpleString("The quick brown fox jumps over the lazy dog."), Toml.decodeFromString(test))
    }

    @Test
    fun testWithHashSymbol() {
        val test = """
            a = '''
            Roses are red # Not a comment
            # Not a comment 
            Violets are blue'''
        """.trimIndent()
        assertEquals(
            SimpleString("Roses are red # Not a comment\n# Not a comment \nViolets are blue"),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun correctQuotesInsideLiteral() {
        var test = "a = '''Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"''' " +
            "\nb = 2"
        assertEquals(
            StringAndInt("Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"", 2),
            Toml.decodeFromString(test)
        )

        test = "a = ''''That,' she said, 'is still pointless.'''' " +
            "\nb = 2"
        assertEquals(
            StringAndInt("'That,' she said, 'is still pointless.'", 2),
            Toml.decodeFromString(test)
        )

        test = "a = '''Here are ten apostrophes: ''\\'''\\'''\\'' ''' " +
            "\nb = 2"
        assertEquals(
            StringAndInt("Here are ten apostrophes: '''''''''' ", 2),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun incorrectQuotesInside() {
        val test = "a = '''Here are fifteen apostrophes: ''''''''''''''''''"
        val exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))
    }

    @Test
    fun leadingNewLines() {
        // "A newline immediately following the opening delimiter will be trimmed."
        var test = """
            a = '''
            
            My String'''
        """.trimIndent()
        assertEquals(
            SimpleString("\nMy String"),
            Toml.decodeFromString(test)
        )

        test = """
            a = '''
            My String''' 
        """.trimIndent()
        assertEquals(
            SimpleString("My String"),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun incorrectString() {
        var test = "a = '''Test String "
        var exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = "a = '''Test String '"
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = "a = '''Test String \"\"\""
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = """
            a = '''
            
            Test String '
            b = "abc"
        """.trimIndent()
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleStrings>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = """
            a = '''
            
            Test String
            
        """.trimIndent()
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))
    }

    @Test
    fun betweenOtherValues() {
        val test = """
            b = 2
            a = '''Test 
            String
            '''
            c = 3
        """.trimIndent()
        assertEquals(
            StringAndInts("Test \nString\n", 2, 3),
            Toml.decodeFromString(test)
        )
    }
}