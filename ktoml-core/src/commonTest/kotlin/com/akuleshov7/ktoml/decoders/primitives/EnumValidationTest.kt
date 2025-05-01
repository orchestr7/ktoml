package com.akuleshov7.ktoml.decoders.primitives

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.InvalidEnumValueException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
        var exception = assertFailsWith<InvalidEnumValueException> {
            Toml.decodeFromString<Color>("myEnum = \"KANATA\"")
        }

        exception.exceptionValidation(
            "Line 1: value <KANATA> is not a valid enum option. Did you mean <KANAPA>? " +
                    "Permitted choices are: CANAPA, KANADA, KANAPA, MEXICO, USA."
        )

        exception = assertFailsWith<InvalidEnumValueException> {
            Toml.decodeFromString<Color>("myEnum = \"TEST\"")
        }

        exception.exceptionValidation(
            "Line 1: value <TEST> is not a valid enum option. Did you mean <USA>? " +
                    "Permitted choices are: CANAPA, KANADA, KANAPA, MEXICO, USA."
        )

        exception = assertFailsWith<InvalidEnumValueException> {
            Toml.decodeFromString<Color>("myEnum = \"MEKSICA\"")
        }

        exception.exceptionValidation(
            "Line 1: value <MEKSICA> is not a valid enum option. Did you mean <MEXICO>? " +
                    "Permitted choices are: CANAPA, KANADA, KANAPA, MEXICO, USA."
        )
    }
}

private fun InvalidEnumValueException.exceptionValidation(expected: String) {
    assertEquals(expected, this.message)
}
