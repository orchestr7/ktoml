package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FloatLiteralTest {
    private val format = Toml(TomlInputConfig.compliant())

    @Test
    fun longFractionDoesNotOverflowStack() {
        val input = "x = 1." + "0".repeat(10_000)
        assertEquals(mapOf("x" to 1.0), format.decodeFromString<Map<String, Double>>(input))
    }

    @Test
    fun longExponentDoesNotOverflowStack() {
        val input = "x = 1e" + "0".repeat(10_000)
        assertEquals(mapOf("x" to 1.0), format.decodeFromString<Map<String, Double>>(input))
    }

    @Test
    fun longUnderscoredFractionDoesNotOverflowStack() {
        val input = "x = 1." + "0_".repeat(5_000) + "0"
        assertEquals(mapOf("x" to 1.0), format.decodeFromString<Map<String, Double>>(input))
    }

    @Test
    fun longMalformedFloatIsRejectedWithoutOverflow() {
        val input = "x = 1." + "0".repeat(10_000) + "_"
        assertFailsWith<ParseException> {
            format.decodeFromString<Map<String, Double>>(input)
        }
    }

    @Test
    fun misplacedFloatUnderscoresAreRejected() {
        listOf("1_.0", "1._0", "1.0_", "1e_2", "1e+_2", "1__0.0", "0_1.0", "1.0e", "1.0e+").forEach { literal ->
            assertFailsWith<ParseException>(literal) {
                format.decodeFromString<Map<String, Double>>("x = $literal")
            }
        }
    }

    @Test
    fun underscoredFractionAndExponentDecode() {
        assertEquals(mapOf("x" to 123.4), format.decodeFromString<Map<String, Double>>("x = 1_2.3_4e+0_1"))
    }
}
