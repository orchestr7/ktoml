package com.akuleshov7.ktoml.parsers

/**
 * If this string starts and end with quotes - will return the string with quotes removed
 * Otherwise, returns this string.
 */
fun String.trimQuotes(): String {
    if (this.startsWith("\"") && this.endsWith("\"")) {
        return this.removePrefix("\"").removeSuffix("\"")
    }

    return this
}
