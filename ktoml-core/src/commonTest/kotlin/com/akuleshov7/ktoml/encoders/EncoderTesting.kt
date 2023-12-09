package com.akuleshov7.ktoml.encoders

import com.akuleshov7.ktoml.Toml
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import kotlinx.serialization.encodeToString

inline fun <reified T> T.shouldEncodeInto(
    encodedValue: String,
    tomlInstance: Toml = Toml
) = apply {
    this should encodeInto(encodedValue, tomlInstance)
}

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