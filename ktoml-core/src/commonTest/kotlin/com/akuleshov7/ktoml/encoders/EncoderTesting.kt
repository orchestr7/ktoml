package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import kotlinx.serialization.encodeToString
import kotlin.test.assertEquals

inline fun <reified T> assertEncodedEquals(
    value: T,
    expectedToml: String,
    tomlInstance: Toml = Toml
) {
    assertEquals(expectedToml, tomlInstance.encodeToString(value))
}
