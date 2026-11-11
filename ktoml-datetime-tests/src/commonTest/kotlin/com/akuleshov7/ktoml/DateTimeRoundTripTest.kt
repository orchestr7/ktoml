package com.akuleshov7.ktoml

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

/**
 * End-to-end serialization/deserialization round trips for every date-time type, with kotlinx-datetime
 * on the classpath (the opt-in scenario). Round trips assert object equality, not textual identity.
 */
@kotlin.time.ExperimentalTime
class DateTimeRoundTripTest {
    @Serializable
    data class AllDateTimes(
        val instant: Instant,
        val instantWithNanos: Instant,
        val localDateTime: LocalDateTime,
        val localDateTimeWithNanos: LocalDateTime,
        val localDate: LocalDate,
        val localTime: LocalTime,
        val instants: List<Instant>,
        val dates: List<LocalDate>,
    )

    @Test
    fun allDateTimeTypesRoundTripThroughEncodeAndDecode() {
        val original = AllDateTimes(
            instant = Instant.parse("1979-05-27T07:32:00Z"),
            instantWithNanos = Instant.parse("1979-05-27T00:32:00.999999Z"),
            localDateTime = LocalDateTime(1979, 5, 27, 7, 32, 0),
            localDateTimeWithNanos = LocalDateTime(1979, 5, 27, 0, 32, 0, 999_999_000),
            localDate = LocalDate(1979, 5, 27),
            localTime = LocalTime(7, 32, 0),
            instants = listOf(
                Instant.parse("1979-05-27T07:32:00Z"),
                Instant.parse("2024-12-31T23:59:59Z"),
            ),
            dates = listOf(LocalDate(2000, 2, 29), LocalDate(2024, 2, 29)),
        )

        val toml = Toml.encodeToString(original)
        assertEquals(original, Toml.decodeFromString<AllDateTimes>(toml))
    }

    @Serializable
    data class Offsets(val a: Instant, val b: Instant, val c: Instant, val d: Instant)

    @Test
    fun offsetLiteralsDecodeToTheSameInstantRegardlessOfSpelling() {
        // space separator, lowercase z, explicit offset, omitted seconds -- all valid TOML 1.1.
        val decoded = Toml.decodeFromString<Offsets>(
            """
            a = 1979-05-27T07:32:00Z
            b = 1979-05-27 07:32:00z
            c = 1979-05-27T00:32:00-07:00
            d = 1979-05-27T07:32Z
            """.trimIndent(),
        )
        val expected = Instant.parse("1979-05-27T07:32:00Z")
        assertEquals(expected, decoded.a)
        assertEquals(expected, decoded.b)
        assertEquals(expected, decoded.c)
        assertEquals(expected, decoded.d)
    }

    @Serializable
    data class Locals(val dt: LocalDateTime, val d: LocalDate, val t: LocalTime)

    @Test
    fun localLiteralsDecodeToTypesAndRoundTrip() {
        val decoded = Toml.decodeFromString<Locals>(
            """
            dt = 1979-05-27 07:32
            d = 1979-05-27
            t = 07:32
            """.trimIndent(),
        )
        assertEquals(LocalDateTime(1979, 5, 27, 7, 32, 0), decoded.dt)
        assertEquals(LocalDate(1979, 5, 27), decoded.d)
        assertEquals(LocalTime(7, 32, 0), decoded.t)
        // re-encode then decode again -> still equal
        assertEquals(decoded, Toml.decodeFromString<Locals>(Toml.encodeToString(decoded)))
    }
}
