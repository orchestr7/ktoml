package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.decoders.shouldDecodeInto
import com.akuleshov7.ktoml.decoders.shouldFailAtLine
import com.akuleshov7.ktoml.decoders.shouldThrowExceptionWhileDecoding
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlin.test.Test

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
        """
            a = '''abc'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("abc"))

        """
            a = '''
            first line
            second line'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line\nsecond line"))

        """
            a = '''first line
            second line'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line\nsecond line"))

        """
            a = '''first line

            second line'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line\n\nsecond line"))

        "a = '''first line\n\t  \nsecond line'''"
            .shouldDecodeInto(SimpleString("first line\n\t  \nsecond line"))

        """
            a = '''
            Roses are red
            Violets are blue'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("Roses are red\nViolets are blue"))

        """
            a = '''
            
            Test'''
            b = 2
        """.trimIndent()
            .shouldDecodeInto(StringAndInt("\nTest", 2))

        """
            a = '''Test
            b = 32
            '''
        """.trimIndent()
            .shouldDecodeInto(SimpleString(a = "Test\nb = 32\n"))
    }

    @Test
    fun lineEndingBackslash() {
        """
            a = '''
            first line \
            second line \
            
            third line'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line second line third line"))

        """
            a = '''
            The quick brown \
            
                fox jumps over \
                 the lazy dog.'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("The quick brown fox jumps over the lazy dog."))

        """
            a = '''\
                The quick brown \
                fox jumps over \
                the lazy dog.\
                    '''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("The quick brown fox jumps over the lazy dog."))
    }

    @Test
    fun testWithHashSymbol() {
        """
            a = '''
            Roses are red # Not a comment
            # Not a comment 
            Violets are blue'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("Roses are red # Not a comment\n# Not a comment \nViolets are blue"))
    }

    @Test
    fun correctQuotesInsideLiteral() {
        ("a = '''Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"''' " + "\nb = 2")
            .shouldDecodeInto(StringAndInt("Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\"", 2))

        ("a = ''''That,' she said, 'is still pointless.'''' " + "\nb = 2")
            .shouldDecodeInto(StringAndInt("'That,' she said, 'is still pointless.'", 2))

        ("a = '''Here are ten apostrophes: ''\\'''\\'''\\'' ''' " + "\nb = 2")
            .shouldDecodeInto(StringAndInt("Here are ten apostrophes: '''''''''' ", 2))
    }

    @Test
    fun incorrectQuotesInside() {
        "a = '''Here are fifteen apostrophes: ''''''''''''''''''"
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)
    }

    @Test
    fun leadingNewLines() {
        // "A newline immediately following the opening delimiter will be trimmed."
        """
            a = '''
            
            My String'''
        """.trimIndent()
            .shouldDecodeInto(SimpleString("\nMy String"))

        """
            a = '''
            My String''' 
        """.trimIndent()
            .shouldDecodeInto(SimpleString("My String"))
    }

    @Test
    fun incorrectString() {
        "a = '''Test String "
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)

        "a = '''Test String '"
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)

        "a = '''Test String \"\"\""
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)

        """
            a = '''
            
            Test String '
            b = "abc"
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleStrings, ParseException>()
            .shouldFailAtLine(1)

        """
            a = '''
            
            Test String
            
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)
    }

    @Test
    fun betweenOtherValues() {
        """
            b = 2
            a = '''Test 
            String
            '''
            c = 3
        """.trimIndent()
            .shouldDecodeInto(StringAndInts("Test \nString\n", 2, 3))
    }
}