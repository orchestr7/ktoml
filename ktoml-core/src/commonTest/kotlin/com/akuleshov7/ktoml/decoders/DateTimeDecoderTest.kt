package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlin.test.Test

class DateTimeDecoderTest {

    @Serializable
    data class TimeTable(
        val instants: List<Instant>,
        val localDateTimes: List<LocalDateTime>,
        val localDate: LocalDate,
        val localTime: LocalTime,
        val dateInString: String
    )

    @Test
    fun testDateParsing() {
        val expectedInstants = listOf(
            LocalDateTime(1979, 5, 27, 7, 32, 0)
                .toInstant(TimeZone.UTC),
            LocalDateTime(1979, 5, 27, 0, 32, 0)
                .toInstant(TimeZone.of("UTC-7")),
            LocalDateTime(1979, 5, 27, 0, 32, 0, 999999000)
                .toInstant(TimeZone.of("UTC-7")),
            LocalDateTime(1979, 5, 27, 7, 32, 0)
                .toInstant(TimeZone.UTC),
        )
        val expectedLocalDateTimes = listOf(
            LocalDateTime(1979, 5, 27, 7, 32, 0),
            LocalDateTime(1979, 5, 27, 0, 32, 0, 999999000)
        )
        val expectedLocalDate = LocalDate(1979, 5, 27)
        val expectedLocalTime = LocalTime(7, 45, 33)

        val expectedTimeTable = TimeTable(
            expectedInstants,
            expectedLocalDateTimes,
            expectedLocalDate,
            expectedLocalTime,
            "1979-05-27T00:32:00-07:00"
        )

        """
            instants = [1979-05-27T07:32:00Z, 1979-05-27T00:32:00-07:00, 1979-05-27T00:32:00.999999-07:00, 1979-05-27 07:32:00Z]
            localDateTimes = [1979-05-27T07:32:00, 1979-05-27T00:32:00.999999]
            localDate = 1979-05-27
            localTime = 07:45:33
            dateInString = "1979-05-27T00:32:00-07:00"
        """.trimIndent()
            .shouldDecodeInto(expectedTimeTable)
    }

    @Serializable
    data class InvalidInstant(val instant: Instant)

    @Serializable
    data class InvalidDateTime(val dateTime: LocalDateTime)

    @Serializable
    data class InvalidDate(val date: LocalDate)

    @Serializable
    data class InvalidTime(val time: LocalTime)

    @Test
    fun testInvalidData() {
        "instant=1979-05-27T07:32:00INVALID"
            .shouldThrowExceptionWhileDecoding<InvalidInstant, ParseException>()
        "dateTime=1979/05/27T07:32:00"
            .shouldThrowExceptionWhileDecoding<InvalidDateTime, ParseException>()
        "date=1979/05/27"
            .shouldThrowExceptionWhileDecoding<InvalidDate, ParseException>()
        "time=07/35/12"
            .shouldThrowExceptionWhileDecoding<InvalidTime, ParseException>()
    }
}