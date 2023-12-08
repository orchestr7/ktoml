package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import kotlinx.serialization.encodeToString
import kotlin.test.assertEquals

inline fun <reified T> encodeInto(
    encodedValue: String,
    tomlInstance: Toml = Toml
) = Matcher<T> { value ->
    val result = tomlInstance.encodeToString(value)
    MatcherResult(
        result == encodedValue,
        { "object was encoded as $result but we expected $encodedValue" },
        { "object should not have been encoded as $encodedValue" },
    )
}