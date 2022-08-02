package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import kotlinx.datetime.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeEncoderTest {
    @Test
    fun dateTimeTest() {
        @Serializable
        data class DateTimes(
            val instants: List<Instant> =
                    listOf(
                        LocalDateTime(1979, 5, 27, 7, 32, 0)
                            .toInstant(TimeZone.UTC),
                        LocalDateTime(1979, 5, 27, 0, 32, 0, 999999000)
                            .toInstant(TimeZone.UTC)
                    ),
            val localDateTimes: List<LocalDateTime> =
                    listOf(
                        LocalDateTime(1979, 5, 27, 7, 32, 0),
                        LocalDateTime(1979, 5, 27, 0, 32, 0, 999999000)
                    ),
            val localDate: LocalDate = LocalDate(1979, 5, 27)
        )

        assertEquals(
            """
                instants = [ 1979-05-27T07:32:00Z, 1979-05-27T00:32:00.999999Z ]
                
                localDateTimes = [ 1979-05-27T07:32, 1979-05-27T00:32:00.999999 ]
                
                localDate = 1979-05-27
            """.trimIndent(),
            Toml.encodeToString(DateTimes())
        )
    }
}
