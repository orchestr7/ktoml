package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertFailsWith

@Serializable
data class RequiredStr(
    val a: String = "a",
    @Required
    val b: String = "b",
    val c: String,
)

class RequiredAnnotation {
    @Test
    fun testMissingRequiredPropertyException() {
        assertFailsWith<MissingRequiredPropertyException> { Toml.decodeFromString<RequiredStr>("c = \"100\"") }
    }
}
