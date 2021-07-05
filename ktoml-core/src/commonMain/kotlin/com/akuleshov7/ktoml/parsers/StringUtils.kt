/**
 * Common String Utilities
 */

package com.akuleshov7.ktoml.parsers

/**
 * If this string starts and end with quotes("") - will return the string with quotes removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
fun String.trimQuotes(): String = trimSymbols(this, "\"", "\"")

/**
 * If this string starts and end with brackets([]) - will return the string with brackets removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
fun String.trimBrackets(): String = trimSymbols(this, "[", "]")

private fun trimSymbols(
    str: String,
    prefix: String,
    suffix: String): String {
    if (str.startsWith(prefix) && str.endsWith(suffix)) {
        return str.removePrefix(prefix).removeSuffix(suffix)
    }
    return str
}
