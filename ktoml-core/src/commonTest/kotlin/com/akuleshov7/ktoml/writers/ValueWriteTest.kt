package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.tree.nodes.pairs.values.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
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
        testTomlValue(TomlLiteralString("literal \tstring" as Any), "'literal \tstring'")

        // Control characters rejection
        testTomlValueFailure(TomlLiteralString("control \u0000\bchars" as Any))

        // Escaped single quotes

        val disallowQuotes = TomlOutputConfig(allowEscapedQuotesInLiteralStrings = false)

        val escapedSingleQuotes = TomlLiteralString("'escaped quotes'" as Any)

        testTomlValueFailure(escapedSingleQuotes, disallowQuotes)

        testTomlValue(escapedSingleQuotes, "'\\'escaped quotes\\''")
    }

    @Test
    fun basicStringWriteTest() {
        testTomlValue(TomlBasicString("hello world" as Any), "\"hello world\"")

        // Control character escaping
        testTomlValue(TomlBasicString("hello \b\t\n\u000C\r world" as Any), "\"hello \\b\t\\n\\f\\r world\"")
        testTomlValue(TomlBasicString("hello \u0000 world" as Any), "\"hello \\u0000 world\"")

        // Backslash escaping
        testTomlValue(TomlBasicString("""hello\world""" as Any), """"hello\\world"""")
        testTomlValue(TomlBasicString("""hello\\\ world""" as Any), """"hello\\\\ world"""")
        testTomlValue(TomlBasicString("""hello\b\t\n\\\f\r world""" as Any), """"hello\b\t\n\\\f\r world"""")
        testTomlValue(TomlBasicString("""hello\u0000\\\Uffffffff world""" as Any), """"hello\u0000\\\Uffffffff world"""")
    }

    @Suppress("COMMENTED_CODE")
    @Test
    fun integerWriteTest() {
        // Decimal
        testTomlValue(TomlLong(1234567L), "1234567")
        testTomlValue(TomlLong(-1234567L), "-1234567")

        // Hex
        //testTomlValue(TomlLong(0xdeadc0de, IntegerRepresentation.HEX), "0xdeadc0de")

        // Binary
        //testTomlValue(TomlLong(0b10000000, IntegerRepresentation.BINARY), "0b10000000")

        // Octal
        //testTomlValue(TomlLong(0x1FF, IntegerRepresentation.OCTAL), "0o777")
    }

    @Test
    fun floatWriteTest() {
        testTomlValue(TomlDouble(PI), "$PI")
        testTomlValue(TomlDouble(NaN), "nan")
        testTomlValue(TomlDouble(POSITIVE_INFINITY), "inf")
        testTomlValue(TomlDouble(NEGATIVE_INFINITY), "-inf")
    }

    @Test
    fun wholeNumberFloatRegressionTest() {
        testTomlValue(TomlDouble(3.0), "3.0")
    }

    @Test
    fun booleanWriteTest() {
        testTomlValue(TomlBoolean(true), "true")
        testTomlValue(TomlBoolean(false), "false")
    }

    @Test
    fun dateTimeWriteTest() {
        val instant = "1979-05-27T07:32:00Z"
        val localDt = "1979-05-27T07:32"
        val localD = "1979-05-27"
        val localT = "07:32:32"

        testTomlValue(TomlDateTime(Instant.parse(instant)), instant)
        testTomlValue(TomlDateTime(LocalDateTime.parse(localDt)), localDt)
        testTomlValue(TomlDateTime(LocalDate.parse(localD)), localD)
        testTomlValue(TomlDateTime(LocalTime.parse(localT)), localT)
    }

    @Test
    fun nullWriteTest() = testTomlValue(TomlNull(), "null")

    @Test
    fun arrayWriteTest() {
        val array = TomlArray(
            listOf(
                TomlLong(1L),
                TomlBasicString("string" as Any),
                TomlArray(
                    listOf(
                        TomlDouble(3.14)
                    )
                )
            )
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
    config: TomlOutputConfig = TomlOutputConfig(),
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
    config: TomlOutputConfig = TomlOutputConfig()
) {
    assertFailsWith<TomlWritingException> {
        val emitter = TomlStringEmitter(StringBuilder(), config)

        value.write(emitter, config)
    }
}
