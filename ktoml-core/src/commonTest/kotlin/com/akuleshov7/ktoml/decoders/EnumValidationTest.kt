package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class Color(val myEnum: EnumExample)

enum class EnumExample {
    CANAPA,
    KANAPA,
    KANADA,
    USA,
    MEXICO,
}

class EnumValidationTest {
    @Test
    fun testRegressions() {
        "myEnum = \"KANATA\""
            .shouldThrowExceptionWhileDecoding<Color, InvalidEnumValueException>()
            .shouldHaveMessage(
                "Line 1: value <KANATA> is not a valid enum option. Did you mean <KANAPA>? " +
                    "Permitted choices are: CANAPA, KANADA, KANAPA, MEXICO, USA."
            )

        "myEnum = \"TEST\""
            .shouldThrowExceptionWhileDecoding<Color, InvalidEnumValueException>()
            .shouldHaveMessage(
                "Line 1: value <TEST> is not a valid enum option. Did you mean <USA>? " +
                    "Permitted choices are: CANAPA, KANADA, KANAPA, MEXICO, USA."
            )

        "myEnum = \"MEKSICA\""
            .shouldThrowExceptionWhileDecoding<Color, InvalidEnumValueException>()
            .shouldHaveMessage(
                "Line 1: value <MEKSICA> is not a valid enum option. Did you mean <MEXICO>? " +
                    "Permitted choices are: CANAPA, KANADA, KANAPA, MEXICO, USA."
            )
    }
}
