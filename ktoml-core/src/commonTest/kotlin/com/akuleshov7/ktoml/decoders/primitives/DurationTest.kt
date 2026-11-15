package com.akuleshov7.ktoml.decoders.primitives

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * TOML has no duration type, so `kotlin.time.Duration` is carried as a (quoted) string. It works out of
 * the box via kotlinx-serialization's built-in ISO-8601 `Duration` serializer — `Duration` is stdlib,
 * so this needs no date library either.
 */
class DurationTest {
    @Serializable
    data class Timeouts(val connect: Duration, val read: Duration)

    @Test
    fun durationDecodesFromQuotedIsoString() {
        val decoded = Toml.decodeFromString<Timeouts>(
            """
            connect = "PT30S"
            read = "PT1M30S"
            """.trimIndent(),
        )
        assertEquals(30.seconds, decoded.connect)
        assertEquals(1.minutes + 30.seconds, decoded.read)
    }

    @Test
    fun durationEncodeDecodeRoundTrip() {
        val original = Timeouts(connect = 30.seconds, read = 1.minutes + 30.seconds)
        // emitted as quoted strings, since TOML has no bare duration literal
        assertEquals(
            """
            connect = "PT30S"
            read = "PT1M30S"
            """.trimIndent(),
            Toml.encodeToString(original),
        )
        assertEquals(original, Toml.decodeFromString<Timeouts>(Toml.encodeToString(original)))
    }
}
