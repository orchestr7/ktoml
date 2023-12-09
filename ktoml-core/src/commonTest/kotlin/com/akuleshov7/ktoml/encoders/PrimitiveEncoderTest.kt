package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.annotations.TomlLiteral
import com.akuleshov7.ktoml.annotations.TomlMultiline
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test

class PrimitiveEncoderTest {
    @Serializable
    enum class Greeting {
        @SerialName("hello") Hello
    }

    @Test
    fun primitivesTest() {
        @Serializable
        data class File(
            val enabled: Boolean = true,
            val pi: Double = 3.14,
            val count: Long = 3,
            val port: Int = 8080,
            val greeting: String = "hello",
            val enumGreeting: Greeting = Greeting.Hello,
            @TomlLiteral
            val path: String = """C:\some\path\"""
        )

        File().shouldEncodeInto(
            """
                enabled = true
                pi = 3.14
                count = 3
                port = 8080
                greeting = "hello"
                enumGreeting = "hello"
                path = 'C:\some\path\'
            """.trimIndent()
        )
    }

    @Test
    fun stringEscapeTest() {
        @Serializable
        data class File(
            val escapeString: String? = null,
            @TomlLiteral
            val literalEscapeString: String? = null
        )

        val tab = '\t'

        File("\"hello world\"").shouldEncodeInto(
            """escapeString = "\"hello world\"""""
        )

        File("hello \b\t\n\u000C\r world").shouldEncodeInto(
            """escapeString = "hello \b$tab\n\f\r world""""
        )

        File("hello \u0000 world").shouldEncodeInto(
            """escapeString = "hello \u0000 world""""
        )

        File("""hello\world""").shouldEncodeInto(
            """escapeString = "hello\\world""""
        )

        File("""hello \Uffffffff world""").shouldEncodeInto(
            """escapeString = "hello \Uffffffff world""""
        )

        File(literalEscapeString = "'quotes'").shouldEncodeInto(
            """literalEscapeString = '\'quotes\''"""
        )
    }

    @Test
    fun multilineStringsSpecifications() {
        @Serializable
        data class MultilineLiteralStr(
            @TomlMultiline
            @TomlLiteral
            val a: String
        )

        @Serializable
        data class MultilineBasicStr(
            @TomlMultiline
            val a: String
        )

        MultilineLiteralStr("test \n test \n test \'\'\'").shouldEncodeInto(
            """
                |a = '''
                |test 
                | test 
                | test ''\'
                |'''
            """.trimMargin()
        )

        MultilineBasicStr("test \n test \n test \'\'\'").shouldEncodeInto(
            "a = \"\"\"\ntest \n test \n test \'\'\'\n\"\"\""
        )
    }

    @Test
    fun jsWholeDoubleRegression() {
        @Serializable
        data class File(
            val wholeNumberDouble: Double = 3.0
        )

        File().shouldEncodeInto("wholeNumberDouble = 3.0")
    }
}
