/**
 * Common String Utilities
 */

package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.exceptions.TomlParsingException

/**
 * Splitting dot-separated string to tokens:
 * a.b.c -> [a, b, c]; a."b.c".d -> [a, "b.c", d];
 *
 * @param lineNo - the line number in toml
 * @return list with strings after the initial string was split
 */
internal fun String.splitKeyToTokens(lineNo: Int): List<String> {
    this.validateQuotes(lineNo)
    this.validateSymbols(lineNo)

    var singleQuoteIsClosed = true
    var doubleQuoteIsClosed = true
    val dotSeparatedParts: MutableList<String> = mutableListOf()
    var currentPart = StringBuilder()
    // simple split() method won't work here, because in such case we could break following keys:
    // a."b.c.d".e (here only three tables: a/"b.c.d"/and e)
    this.forEach { ch ->
        when (ch) {
            '\'' -> {
                singleQuoteIsClosed = !singleQuoteIsClosed
                currentPart.append(ch)
            }
            '\"' -> {
                doubleQuoteIsClosed = !doubleQuoteIsClosed
                currentPart.append(ch)
            }
            '.' -> if (singleQuoteIsClosed && doubleQuoteIsClosed) {
                dotSeparatedParts.add(currentPart.toString())
                currentPart = StringBuilder()
            } else {
                currentPart.append(ch)
            }
            else -> currentPart.append(ch)
        }
    }

    val keyPart = currentPart.toString().trim()
    keyPart.validateSpaces(lineNo, this)

    dotSeparatedParts.add(keyPart)
    return dotSeparatedParts
}

/**
 * If this string starts and end with single quotes('') - will return the string with quotes removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimSingleQuotes(): String = trimSymbols(this, "'", "'")

/**
 * If this string starts and end with quotes("") - will return the string with quotes removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimQuotes(): String = trimSymbols(this, "\"", "\"")

/**
 * If this string starts and end with brackets([]) - will return the string with brackets removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimBrackets(): String = trimSymbols(this, "[", "]")

private fun String.validateSpaces(lineNo: Int, fullKey: String) {
    if (this.trim().count { it == ' ' } > 0 && this.isNotQuoted()) {
        throw TomlParsingException(
            "Not able to parse the key: [$fullKey] as it has invalid spaces." +
                    " If you would like to have spaces in the middle of the key - use quotes: \"WORD SPACE\"", lineNo
        )
    }
}

/**
 * small validation for quotes: each quote should be closed in a key
 */
private fun String.validateQuotes(lineNo: Int) {
    if (this.count { it == '\"' } % 2 != 0 || this.count { it == '\'' } % 2 != 0) {
        throw TomlParsingException(
            "Not able to parse the key: [$this] as it does not have closing quote." +
                    " Please note, that you cannot use even escaped quotes in the bare keys.",
            lineNo
        )
    }
}

/**
 * validate that bare key parts (not quoted) contain only valid symbols A..Z, a..z, 0..9, -, _
 */
private fun String.validateSymbols(lineNo: Int) {
    var singleQuoteIsClosed = true
    var doubleQuoteIsClosed = true
    this.trim().trimQuotes().forEach { ch ->
        when (ch) {
            '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
            '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
            else -> if (doubleQuoteIsClosed && singleQuoteIsClosed &&
                    // FixMe: isLetterOrDigit is not supported in Kotlin 1.4, but 1.5 is not compiling right now
                    !setOf('_', '-', '.', '"', '\'', ' ', '\t').contains(ch) && !ch.isLetterOrDigit()
            ) {
                throw TomlParsingException(
                    "Not able to parse the key: [$this] as it contains invalid symbols." +
                            " In case you would like to use special symbols - use quotes as" +
                            " it is required by TOML standard: \"My key ~ with special % symbols\"",
                    lineNo
                )
            }
        }
    }
}

private fun Char.isLetterOrDigit() = CharRange('A', 'Z').contains(this) ||
        CharRange('a', 'z').contains(this) ||
        CharRange('0', '9').contains(this)

private fun String.isNotQuoted() = !(this.startsWith("\"") && this.endsWith("\""))

private fun trimSymbols(
    str: String,
    prefix: String,
    suffix: String
): String {
    if (str.startsWith(prefix) && str.endsWith(suffix)) {
        return str.removePrefix(prefix).removeSuffix(suffix)
    }
    return str
}
