package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.decodeFromString

inline fun <reified T, reified E: Exception> String.shouldThrowExceptionWhileDecoding(
    tomlInstance: Toml = Toml
): E {
    return shouldThrow<E> {
        tomlInstance.decodeFromString<T>(this)
    }
}

fun Exception.shouldFailAtLine(line: Int) = apply {
    message.shouldContain("Line $line")
}

inline fun <reified T> String.shouldDecodeInto(
    decodedValue: T,
    tomlInstance: Toml = Toml
) = apply {
    this should decodeInto(decodedValue, tomlInstance)
}

inline fun <reified T> decodeInto(
    decodedValue: T,
    tomlInstance: Toml = Toml
) = Matcher<String> { value ->
    val result: T = tomlInstance.decodeFromString(value)
    MatcherResult(
        result == decodedValue,
        { "object was decoded as $result but we expected $decodedValue" },
        { "object should not have been decoded as $decodedValue" },
    )
}