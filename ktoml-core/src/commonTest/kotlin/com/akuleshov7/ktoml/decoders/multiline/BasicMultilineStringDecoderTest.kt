package com.akuleshov7.ktoml.decoders.multiline

import com.akuleshov7.ktoml.decoders.shouldDecodeInto
import com.akuleshov7.ktoml.decoders.shouldFailAtLine
import com.akuleshov7.ktoml.decoders.shouldThrowExceptionWhileDecoding
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.Serializable
import kotlin.test.Test

class BasicMultilineStringDecoderTest {

    @Serializable
    data class SimpleString(val a: String)

    @Serializable
    data class SimpleStrings(val a: String, val b: String)

    @Serializable
    data class StringAndInt(val a: String, val b: Int)

    @Serializable
    data class StringAndInts(val a: String, val b: Int, val c: Int)

    private val tripleQuotes = "\"\"\""

    @Test
    fun testMultilineBasicStringDecode() {
        """
            a = ${tripleQuotes}abc${tripleQuotes}
        """.trimIndent()
            .shouldDecodeInto(SimpleString("abc"))

        """
            
            a = ${tripleQuotes}abc${tripleQuotes}
            
            b = 123
        """.trimIndent()
            .shouldDecodeInto(StringAndInt("abc", 123))

        """
            a = $tripleQuotes
            first line # comment \
            second line$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line # comment second line"))

        """
            a = ${tripleQuotes}first line
            second line$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line\nsecond line"))

        """
            a = ${tripleQuotes}first line

            second line$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line\n\nsecond line"))

        "a = \"\"\"first line\n\t  \nsecond line\"\"\""
            .shouldDecodeInto(SimpleString("first line\n\t  \nsecond line"))

        """
            a = $tripleQuotes
            Roses are red
            Violets are blue$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("Roses are red\nViolets are blue"))

        """
            a = $tripleQuotes
            
            Test$tripleQuotes
            b = 2
        """.trimIndent()
            .shouldDecodeInto(StringAndInt("\nTest", 2))

        """
            a = $tripleQuotes
            Test
            b = 32
            $tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString(a = "Test\nb = 32\n"))
    }

    @Test
    fun lineEndingBackslash() {
        """
            a = $tripleQuotes
            first line \
            second line \
            
            third line$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("first line second line third line"))

        """
            a = $tripleQuotes
            The quick brown \
            
              fox jumps over \
                   the lazy dog.$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("The quick brown fox jumps over the lazy dog."))

        """
            a = $tripleQuotes\
                The quick brown \
                fox jumps over \
                the lazy dog.\
                $tripleQuotes
        """.trimIndent()
            .shouldDecodeInto("The quick brown fox jumps over the lazy dog.")
    }

    @Test
    fun testWithHashSymbol() {
        """
            a = $tripleQuotes
            Roses are red # Not a comment
            # Not a comment 
            Violets are blue$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("Roses are red # Not a comment\n# Not a comment \nViolets are blue"))
    }

    @Test
    fun correctQuotesInsideBasic() {
        "a = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\"\nb = 2"
            .shouldDecodeInto(StringAndInt("Here are two quotation marks: \"\". Simple enough.", 2))

        """
            a = ${tripleQuotes}Here are three quotation marks: ""\".$tripleQuotes
            b = 2
        """.trimIndent()
            .shouldDecodeInto(StringAndInt("Here are three quotation marks: \"\"\".", 2))

        "a = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\"\nb = 2"
            .shouldDecodeInto(StringAndInt("Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\".", 2))

        "a = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\"\nb = 2"
            .shouldDecodeInto(StringAndInt("Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\".", 2))

        "a = \"\"\"\"This,\" she said, \"is just a pointless statement.\"\"\"\"\nb = 2"
            .shouldDecodeInto(StringAndInt("\"This,\" she said, \"is just a pointless statement.\"", 2))

        "a = \"\"\"\"\n\nThis,\" she said, \"is just a pointless statement.\"\n\n\"\"\"\nb = 2"
            .shouldDecodeInto(StringAndInt("\"\n\nThis,\" she said, \"is just a pointless statement.\"\n\n", 2))
    }

    @Test
    fun incorrectQuotesInside() {
        "a = \"\"\"Here are three quotation marks: \"\"\".\"\"\""
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)
    }

    @Test
    fun leadingNewLines() {
        // "A newline immediately following the opening delimiter will be trimmed."
        """
            a = $tripleQuotes
            
            My String$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("\nMy String"))

        """
            a = $tripleQuotes
            My String$tripleQuotes
        """.trimIndent()
            .shouldDecodeInto(SimpleString("My String"))
    }

    @Test
    fun incorrectString() {
        """
            a = ${tripleQuotes}Test String 
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)
        """
            a = ${tripleQuotes}Test String"
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)

        "a = \"\"\"Test String ''' "
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)

        """
            a = ${tripleQuotes}Test String "
            b = "abc"
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleStrings, ParseException>()
            .shouldFailAtLine(1)

        """
            a = $tripleQuotes
            
            Test String
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)

        """
            a = $tripleQuotes
            
            Test String 
            
        """.trimIndent()
            .shouldThrowExceptionWhileDecoding<SimpleString, ParseException>()
            .shouldFailAtLine(1)
    }

    @Test
    fun betweenOtherValues() {
        """
            b = 2
            a = ${tripleQuotes}Test 
            String
            $tripleQuotes
            c = 3
        """.trimIndent()
            .shouldDecodeInto(StringAndInts("Test \nString\n", 2, 3))
    }
}
