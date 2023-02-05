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
        var test = """
            a = ${tripleQuotes}abc${tripleQuotes}
        """.trimIndent()
        assertEquals(SimpleString("abc"), Toml.decodeFromString(test))

        test = """
            a = $tripleQuotes
            first line
            second line$tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("first line\nsecond line"), Toml.decodeFromString(test))

        test = """
            a = ${tripleQuotes}first line
            second line$tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("first line\nsecond line"), Toml.decodeFromString(test))

        test = """
            a = ${tripleQuotes}first line

            second line$tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("first line\n\nsecond line"), Toml.decodeFromString(test))

        test = "a = \"\"\"first line\n\t  \nsecond line\"\"\""
        assertEquals(SimpleString("first line\n\t  \nsecond line"), Toml.decodeFromString(test))

        test = """
            a = $tripleQuotes
            Roses are red
            Violets are blue$tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("Roses are red\nViolets are blue"), Toml.decodeFromString(test))

        test = """
            a = $tripleQuotes
            
            Test$tripleQuotes
            b = 2
        """.trimIndent()
        assertEquals(
            StringAndInt("\nTest", 2),
            Toml.decodeFromString(test)
        )

        test = """
            a = $tripleQuotes
            Test
            b = 32
            $tripleQuotes
        """.trimIndent()
        assertEquals(
            SimpleString(a = "Test\nb = 32\n"),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun lineEndingBackslash() {
        var test = """
            a = $tripleQuotes
            first line \
            second line \
            
            third line$tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("first line second line third line"), Toml.decodeFromString(test))

        test = """
            a = $tripleQuotes
            The quick brown \
            
              fox jumps over \
                   the lazy dog.$tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("The quick brown fox jumps over the lazy dog."), Toml.decodeFromString(test))

        test = """
            a = $tripleQuotes\
                The quick brown \
                fox jumps over \
                the lazy dog.\
                $tripleQuotes
        """.trimIndent()
        assertEquals(SimpleString("The quick brown fox jumps over the lazy dog."), Toml.decodeFromString(test))
    }

    @Test
    fun testWithHashSymbol() {
        val test = """
            a = $tripleQuotes
            Roses are red # Not a comment
            # Not a comment 
            Violets are blue$tripleQuotes
        """.trimIndent()
        assertEquals(
            SimpleString("Roses are red # Not a comment\n# Not a comment \nViolets are blue"),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun correctQuotesInsideBasic() {
        var test = "a = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\"" +
            "\nb = 2"
        assertEquals(
            StringAndInt("Here are two quotation marks: \"\". Simple enough.", 2),
            Toml.decodeFromString(test)
        )

        test = """
            a = ${tripleQuotes}Here are three quotation marks: ""\".$tripleQuotes
            b = 2
        """.trimIndent()
        assertEquals(
            StringAndInt("Here are three quotation marks: \"\"\".", 2),
            Toml.decodeFromString(test)
        )

        test = "a = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\"" +
            "\nb = 2"
        assertEquals(
            StringAndInt("Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\".", 2),
            Toml.decodeFromString(test)
        )

        test = "a = \"\"\"Here are fifteen quotation marks: \"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\"\"\"\\\".\"\"\"" +
            "\nb = 2"
        assertEquals(
            StringAndInt("Here are fifteen quotation marks: \"\"\"\"\"\"\"\"\"\"\"\"\"\"\".", 2),
            Toml.decodeFromString(test)
        )

        test = "a = \"\"\"\"This,\" she said, \"is just a pointless statement.\"\"\"\"" +
            "\nb = 2"
        assertEquals(
            StringAndInt("\"This,\" she said, \"is just a pointless statement.\"", 2),
            Toml.decodeFromString(test)
        )

        test = "a = \"\"\"\"\n\nThis,\" she said, \"is just a pointless statement.\"\n\n\"\"\"" +
            "\nb = 2"
        assertEquals(
            StringAndInt("\"\n\nThis,\" she said, \"is just a pointless statement.\"\n\n", 2),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun incorrectQuotesInside() {
        val test = "a = \"\"\"Here are three quotation marks: \"\"\".\"\"\""
        val exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))
    }

    @Test
    fun leadingNewLines() {
        // "A newline immediately following the opening delimiter will be trimmed."
        var test = """
            a = $tripleQuotes
            
            My String$tripleQuotes
        """.trimIndent()
        assertEquals(
            SimpleString("\nMy String"),
            Toml.decodeFromString(test)
        )

        test = """
            a = $tripleQuotes
            My String$tripleQuotes
        """.trimIndent()
        assertEquals(
            SimpleString("My String"),
            Toml.decodeFromString(test)
        )
    }

    @Test
    fun incorrectString() {
        var test = """
            a = ${tripleQuotes}Test String 
        """.trimIndent()
        var exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = """
            a = ${tripleQuotes}Test String"
        """.trimIndent()
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = "a = \"\"\"Test String ''' "
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = """
            a = ${tripleQuotes}Test String "
            b = "abc"
        """.trimIndent()
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleStrings>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = """
            a = $tripleQuotes
            
            Test String
        """.trimIndent()
        exception = assertFailsWith<ParseException> {
            Toml.decodeFromString<SimpleString>(test)
        }
        assertTrue(exception.message!!.contains("Line 1"))

        test = """
            a = $tripleQuotes
            
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
            a = ${tripleQuotes}Test 
            String
            $tripleQuotes
            c = 3
        """.trimIndent()
        assertEquals(
            StringAndInts("Test \nString\n", 2, 3),
            Toml.decodeFromString(test)
        )
    }
}
