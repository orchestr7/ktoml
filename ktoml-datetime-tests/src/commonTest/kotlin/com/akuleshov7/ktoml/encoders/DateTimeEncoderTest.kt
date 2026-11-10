package com.akuleshov7.ktoml.encoders

import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.time.Instant

@kotlin.time.ExperimentalTime
class DateTimeEncoderTest {
    @Serializable
    data class DateTimes(
        val instant: kotlin.time.Instant = default.toInstant(TimeZone.UTC),
        val instantWithNanos: Instant = defaultWithNanos.toInstant(TimeZone.UTC),
        val localDateTime: LocalDateTime = default,
        val localDateTimeWithNanos: LocalDateTime = defaultWithNanos,
        val localDate: LocalDate = default.date,
        val localTime: LocalTime = default.time
    ) {
        companion object {
            private val default = LocalDateTime(1979, 5, 27, 7, 32, 0)
            private val defaultWithNanos = LocalDateTime(1979, 5, 27, 0, 32, 0, 999999000)
        }
    }

    @Test
    fun dateTimeTest() {
        assertEncodedEquals(
            value = DateTimes(),
            expectedToml = """
                instant = 1979-05-27T07:32:00Z
                instantWithNanos = 1979-05-27T00:32:00.999999Z
                localDateTime = 1979-05-27T07:32
                localDateTimeWithNanos = 1979-05-27T00:32:00.999999
                localDate = 1979-05-27
                localTime = 07:32
            """.trimIndent()
        )
    }
}
