package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.exceptions.MissingRequiredPropertyException
import kotlinx.serialization.Required
import kotlinx.serialization.Serializable
import kotlin.test.Test

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
        "c = \"100\""
            .shouldThrowExceptionWhileDecoding<RequiredStr, MissingRequiredPropertyException>()
    }
}
