package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.tree.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.Double.Companion.NEGATIVE_INFINITY
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PrimitiveValueWriteTest {
    @Test
    fun literalStringWriteTest() {
        // Valid, normal case
        testTomlValue(TomlLiteralString("literal \tstring" as Any, 0), "'literal \tstring'")

        // Control characters rejection
        testTomlValueFailure(TomlLiteralString("control \u0000\bchars" as Any, 0))

        // Escape rejection
        testTomlValueFailure(TomlLiteralString("\\\" escapes\\n \\\"" as Any, 0))

        // Escaped single quotes

        val disallowQuotes = TomlConfig(allowEscapedQuotesInLiteralStrings = false)

        val escapedSingleQuotes = TomlLiteralString("'escaped quotes'" as Any, 0)

        testTomlValueFailure(escapedSingleQuotes, disallowQuotes)

        testTomlValue(escapedSingleQuotes, "'\\'escaped quotes\\''")
    }

    @Test
    fun basicStringWriteTest() {
        testTomlValue(TomlBasicString("hello world" as Any, 0), "\"hello world\"")

        // Control character escaping
        testTomlValue(TomlBasicString("hello \b\t\n\u000C\r world" as Any, 0), "\"hello \\b\t\\n\\f\\r world\"")
        testTomlValue(TomlBasicString("hello \u0000 world" as Any, 0), "\"hello \\u0000 world\"")

        // Backslash escaping
        testTomlValue(TomlBasicString("""hello\world""" as Any, 0), """"hello\\world"""")
        testTomlValue(TomlBasicString("""hello\\\ world""" as Any, 0), """"hello\\\\ world"""")
        testTomlValue(TomlBasicString("""hello\b\t\n\\\f\r world""" as Any, 0), """"hello\b\t\n\\\f\r world"""")
        testTomlValue(TomlBasicString("""hello\u0000\\\Uffffffff world""" as Any, 0), """"hello\u0000\\\Uffffffff world"""")
    }

    @Suppress("COMMENTED_CODE")
    @Test
    fun integerWriteTest() {
        // Decimal
        testTomlValue(TomlLong(1234567L, 0), "1234567")
        testTomlValue(TomlLong(-1234567L, 0), "-1234567")

        // Hex
        //testTomlValue(TomlLong(0xdeadc0de, 0, IntegerRepresentation.HEX), "0xdeadc0de")

        // Binary
        //testTomlValue(TomlLong(0b10000000, 0, IntegerRepresentation.BINARY), "0b10000000")

        // Octal
        //testTomlValue(TomlLong(0x1FF, 0, IntegerRepresentation.OCTAL), "0o777")
    }

    @Test
    fun floatWriteTest() {
        testTomlValue(TomlDouble(PI, 0), "$PI")
        testTomlValue(TomlDouble(NaN, 0), "nan")
        testTomlValue(TomlDouble(POSITIVE_INFINITY, 0), "inf")
        testTomlValue(TomlDouble(NEGATIVE_INFINITY, 0), "-inf")
    }

    @Test
    fun booleanWriteTest() {
        testTomlValue(TomlBoolean(true, 0), "true")
        testTomlValue(TomlBoolean(false, 0), "false")
    }

    @Test
    fun dateTimeWriteTest() {
        val instant = "1979-05-27T07:32:00Z"
        val localDt = "1979-05-27T07:32"
        val localD = "1979-05-27"

        testTomlValue(TomlDateTime(Instant.parse(instant), 0), instant)
        testTomlValue(TomlDateTime(LocalDateTime.parse(localDt), 0), localDt)
        testTomlValue(TomlDateTime(LocalDate.parse(localD), 0), localD)
    }

    @Test
    fun nullWriteTest() = testTomlValue(TomlNull(0), "null")

    @Test
    fun arrayWriteTest() {
        val array = TomlArray(
            listOf(
                TomlLong(1L, 0),
                TomlBasicString("string" as Any, 0),
                TomlArray(
                    listOf(
                        TomlDouble(3.14, 0)
                    ),
                    "",
                    0
                )
            ),
            "",
            0
        )

        // Inline

        testTomlValue(
            array,
            """[ 1, "string", [ 3.14 ] ]"""
        )

        // Multiline

        testTomlValue(
            array,
            """
            [
                1,
                "string",
                [
                    3.14
                ]
            ]
            """.trimIndent(),
            multiline = true
        )
    }
}

fun testTomlValue(
    value: TomlValue,
    expectedString: String,
    config: TomlConfig = TomlConfig(),
    multiline: Boolean = false
) {
    assertEquals(
        expectedString,
        actual = buildString {
            val emitter = TomlStringEmitter(this, config)

            value.write(emitter, config, multiline)
        }
    )
}

fun testTomlValueFailure(
    value: TomlValue,
    config: TomlConfig = TomlConfig()
) {
    assertFailsWith<TomlWritingException> {
        val emitter = TomlStringEmitter(StringBuilder(), config)

        value.write(emitter, config)
    }
}