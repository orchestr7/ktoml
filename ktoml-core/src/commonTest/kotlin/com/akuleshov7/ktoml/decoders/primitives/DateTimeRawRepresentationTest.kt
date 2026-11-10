package com.akuleshov7.ktoml.decoders.primitives

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.TomlParser
import com.akuleshov7.ktoml.tree.nodes.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLocalDate
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLocalDateTime
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlLocalTime
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlOffsetDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Date-time behaviour that requires NO date library on the classpath. ktoml parses and validates
 * date-time literals on its own and keeps them as raw (normalized) text in the AST; offset date-times
 * decode to the stdlib `kotlin.time.Instant`, and any local date/date-time/time can be decoded as a
 * plain `String`.
 *
 * Decoding into the `kotlinx.datetime.Local*` types is the opt-in path (add kotlinx-datetime) and is
 * covered by [DateTimeDecoderTest], which uses kotlinx-datetime as a test-only dependency.
 *
 * This file deliberately imports no date library (only the stdlib `kotlin.time.Instant`).
 */
@kotlin.time.ExperimentalTime
class DateTimeRawRepresentationTest {
    private fun parsedContent(literal: String): Any =
        TomlParser(TomlInputConfig())
            .parseString("v = $literal")
            .children
            .filterIsInstance<TomlKeyValuePrimitive>()
            .single()
            .value
            .content

    @Test
    fun offsetDateTimesAreStoredAsNormalizedRawText() {
        assertEquals(TomlOffsetDateTime("1979-05-27T07:32:00Z"), parsedContent("1979-05-27T07:32:00Z"))
        // space separator -> T, lowercase z -> Z, omitted seconds padded
        assertEquals(TomlOffsetDateTime("1979-05-27T07:32:00Z"), parsedContent("1979-05-27 07:32z"))
        assertEquals(
            TomlOffsetDateTime("1979-05-27T07:32:00-07:00"),
            parsedContent("1979-05-27T07:32-07:00"),
        )
    }

    @Test
    fun localDateTimesAreStoredAsNormalizedRawText() {
        assertEquals(TomlLocalDateTime("1979-05-27T07:32:00"), parsedContent("1979-05-27 07:32"))
        assertEquals(TomlLocalDate("1979-05-27"), parsedContent("1979-05-27"))
        assertEquals(TomlLocalTime("07:32:00"), parsedContent("07:32"))
        assertEquals(TomlLocalTime("10:32:00.555"), parsedContent("10:32:00.555"))
    }

    @Test
    fun semanticallyInvalidDateTimesAreRejectedByOurOwnValidation() {
        listOf(
            "2006-13-01", "2007-00-01",          // month over / under
            "2006-01-32", "2006-01-00",          // day over / under
            "2100-02-29", "1988-02-30",          // not a leap year / no Feb 30
            "24:00:00", "00:60:00", "00:00:61",  // local time: hour / minute / second
            "2006-01-01T24:00:00",               // local date-time hour
            "2006-13-01T00:00:00-00:00",         // offset date-time month
            "1985-06-18 17:04:07+25:00",         // offset hour overflow
            "1985-06-18 17:04:07+12:60",         // offset minute overflow
        ).forEach { literal ->
            assertFailsWith<ParseException>("expected <$literal> to be rejected") {
                TomlParser(TomlInputConfig.compliant()).parseString("v = $literal")
            }
        }
    }

    @Serializable
    data class WithInstant(val ts: kotlin.time.Instant)

    @Test
    fun offsetDateTimeDecodesToStdlibInstant() {
        assertEquals(
            WithInstant(kotlin.time.Instant.parse("1979-05-27T07:32:00Z")),
            Toml.decodeFromString<WithInstant>("ts = 1979-05-27T07:32:00Z"),
        )
    }

    @Test
    fun instantEncodeDecodeRoundTrip() {
        // Encoding -> decoding an Instant round-trips to an equal value, with no date library present.
        listOf(
            WithInstant(kotlin.time.Instant.parse("1979-05-27T07:32:00Z")),
            WithInstant(kotlin.time.Instant.parse("1979-05-27T00:32:00.999999Z")),
        ).forEach { original ->
            val toml = Toml.encodeToString(original)
            assertEquals(original, Toml.decodeFromString<WithInstant>(toml))
        }
    }

    @Serializable
    data class WithStringDates(val d: String, val t: String, val dt: String)

    @Test
    fun localDateTimesCanBeDecodedAsStrings() {
        // No date library needed: a bare local literal decodes into a String field as canonical text.
        assertEquals(
            WithStringDates(d = "1979-05-27", t = "07:32:00", dt = "1979-05-27T07:32:00"),
            Toml.decodeFromString<WithStringDates>(
                """
                d = 1979-05-27
                t = 07:32
                dt = 1979-05-27 07:32:00
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun malformedDatetimeLiteralsAreRejectedDuringParse() {
        listOf(
            "1997-09-09T09:09:09.09+09",
            "1997-09-09T09:09:09.09-09",
            "1997-09-09T09:09:09.",
            "12:13:14.",
        ).forEach { literal ->
            assertFailsWith<ParseException>("expected <$literal> to be rejected") {
                TomlParser(TomlInputConfig.compliant()).parseString("value = $literal")
            }
        }
    }

    @Test
    fun secondsOmittedToml11TimeLiteralsStillParse() {
        TomlParser(TomlInputConfig.compliant()).parseString(
            """
            localTime = 17:45
            localDateTime = 1987-07-05T17:45
            offsetDateTime = 1987-07-05T17:45-07:00
            """.trimIndent(),
        )
    }
}
