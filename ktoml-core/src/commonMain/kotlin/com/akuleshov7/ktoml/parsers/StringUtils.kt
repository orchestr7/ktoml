/**
 * Common String Utilities
 */

package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.utils.newLineChar

/**
 * Splitting dot-separated string to the list of tokens:
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
                dotSeparatedParts.add(currentPart.toString().trim())
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
 * If this multiline string starts and end with triple quotes(''') - will return the string with
 * quotes and newline removed.
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimMultilineLiteralQuotes(): String = trimMultilineQuotes("'''")
    .removePrefix(newLineChar().toString())

/**
 *  When the last non-whitespace character on a line is an unescaped \, it will
 *  be trimmed along with all whitespace (including newlines) up to the next
 *  non-whitespace character or closing delimiter.
 *
 * @return string with the result
 */
internal fun String.convertLineEndingBackslash(): String {
    // We shouldn't trim if the size of the split array == 1
    // It means there is no ending backslash, and we should keep all spaces
    val splitEndingBackslash = this.split("\\\n")
    return if (splitEndingBackslash.size == 1) {
        this
    } else {
        splitEndingBackslash.joinToString("") { it.trimStart() }
    }
}

/**
 * If this string starts and end with quotes("") - will return the string with quotes removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimQuotes(): String = trimSymbols(this, "\"", "\"")

/**
 * If this string starts and end with quotes("" or '') - will return the string with quotes removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimAllQuotes(): String {
    val doubleQuote = "\""
    val singleQuote = "'"

    return if (this.startsWith(doubleQuote) && this.endsWith(doubleQuote)) {
        this.removePrefix(doubleQuote).removeSuffix(doubleQuote)
    } else if (this.startsWith(singleQuote) && this.endsWith(singleQuote)) {
        this.removePrefix(singleQuote).removeSuffix(singleQuote)
    } else {
        this
    }
}

/**
 * If this multiline string starts and end with triple quotes(""") - will return the string with
 * quotes and newline removed.
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimMultilineQuotes(): String = trimMultilineQuotes("\"\"\"")
    .removePrefix(newLineChar().toString())

/**
 * If this string starts and end with curly braces ({}) - will return the string without them (used in inline tables)
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimCurlyBraces(): String = trimSymbols(this, "{", "}")

/**
 * If this string starts and end with brackets([]) - will return the string with brackets removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimBrackets(): String = trimSymbols(this, "[", "]")

/**
 * If this string ends with comma(,) - will return the string with trailing comma removed.
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.removeTrailingComma(): String = this.removeSuffix(",")

/**
 * If this string starts and end with a pair brackets([[]]) - will return the string with brackets removed
 * Otherwise, returns this string.
 *
 * @return string with the result
 */
internal fun String.trimDoubleBrackets(): String = trimSymbols(this, "[[", "]]")

/**
 * Takes only the text before a comment
 *
 * @param allowEscapedQuotesInLiteralStrings value from TomlInputConfig
 * @return The text before a comment, i.e.
 * ```kotlin
 * "a = 0 # Comment".takeBeforeComment() == "a = 0 "
 * ```
 */
internal fun String.takeBeforeComment(allowEscapedQuotesInLiteralStrings: Boolean): String {
    val commentStartIndex = indexOfNextOutsideQuotes(allowEscapedQuotesInLiteralStrings, '#')

    return if (commentStartIndex == -1) {
        this
    } else {
        this.substring(0, commentStartIndex)
    }
}

/**
 * Trims a comment of any text before it and its hash token.
 *
 * @param allowEscapedQuotesInLiteralStrings value from TomlInputConfig
 * @return The comment text, i.e.
 * ```kotlin
 * "a = 0 # Comment".trimComment() == "Comment"
 * ```
 */
internal fun String.trimComment(allowEscapedQuotesInLiteralStrings: Boolean): String {
    val commentStartIndex = indexOfNextOutsideQuotes(allowEscapedQuotesInLiteralStrings, '#')

    return if (commentStartIndex == -1) {
        ""
    } else {
        drop(commentStartIndex + 1).trim()
    }
}

/**
 * @param substring
 * @return count of occurrences of substring in string
 */
internal fun String.getCountOfOccurrencesOfSubstring(substring: String): Int = this.split(substring).size - 1

/**
 * @param allowEscapedQuotesInLiteralStrings value from TomlInputConfig
 * @param searchChar - the character to search for
 * @param startIndex - the index to start searching from
 * @return the index of the first occurrence of the searchChar that is not enclosed in quotation marks
 */
internal fun String.indexOfNextOutsideQuotes(
    allowEscapedQuotesInLiteralStrings: Boolean,
    searchChar: Char,
    startIndex: Int = 0,
): Int {
    val chars = this.drop(startIndex).replaceEscaped(allowEscapedQuotesInLiteralStrings)
    var currentQuoteStr: String? = null

    var idx = 0
    while (idx < chars.length) {
        val symbol = chars[idx]
        // take searchChar index if it's not enclosed in quotation marks
        if (symbol == searchChar && currentQuoteStr == null) {
            return idx + startIndex
        }

        if (symbol == '\"' || symbol == '\'') {
            val quoteStr = currentQuoteStr
            if (quoteStr == null) {
                if (idx + 2 < chars.length && chars[idx + 1] == symbol && chars[idx + 2] == symbol) {
                    currentQuoteStr = "$symbol$symbol$symbol"
                    idx += 3
                    continue // Skip the default increment
                } else {
                    currentQuoteStr = symbol.toString()
                }
            } else if (quoteStr[0] == symbol && (idx + quoteStr.length) <= chars.length) {
                val candidate = chars.substring(idx, idx + quoteStr.length)
                if (candidate == quoteStr) {
                    currentQuoteStr = null
                    idx += candidate.length
                    continue // Skip the default increment
                }
            }
        }
        idx += 1
    }

    return -1
}

/**
 * @param prefix - the string to check for
 * @return true if the string starts with the prefix, ignoring all whitespaces
 */
@Suppress("FUNCTION_BOOLEAN_PREFIX")
internal fun String.startsWithIgnoreAllWhitespaces(prefix: String): Boolean = this.filterNot {
    it.isWhitespace()
}.startsWith(prefix)

/**
 * @param allowEscapedQuotesInLiteralStrings value from TomlInputConfig
 * @param placeholder - the string to replace escaped quotes with
 * @return the string with escaped quotes replaced with a placeholder
 */
internal fun String.replaceEscaped(allowEscapedQuotesInLiteralStrings: Boolean, placeholder: String = "__"): String {
    val isEscapingDisabled = if (allowEscapedQuotesInLiteralStrings) {
        // escaping is disabled when the config option is true AND we have a literal string
        val firstQuoteLetter = this.firstOrNull { it == '\"' || it == '\'' }
        firstQuoteLetter == '\''
    } else {
        false
    }

    return if (!isEscapingDisabled) {
        this.replace("\\\"", placeholder)
            .replace("\\\'", placeholder)
    } else {
        this
    }
}

private fun String.validateSpaces(lineNo: Int, fullKey: String) {
    if (this.trim().count { it == ' ' } > 0 && this.isNotQuoted()) {
        throw ParseException(
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
        throw ParseException(
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
    this.trim().forEach { ch ->
        when (ch) {
            '\'' -> singleQuoteIsClosed = !singleQuoteIsClosed
            '\"' -> doubleQuoteIsClosed = !doubleQuoteIsClosed
            else -> if (doubleQuoteIsClosed && singleQuoteIsClosed &&
                    // FixMe: isLetterOrDigit is not supported in Kotlin 1.4, but 1.5 is not compiling right now
                    !setOf('_', '-', '.', '"', '\'', ' ', '\t').contains(ch) && !ch.isLetterOrDigit()
            ) {
                throw ParseException(
                    "Not able to parse the key: [$this] as it contains invalid symbols." +
                            " In case you would like to use special symbols - use quotes as" +
                            " it is required by TOML standard: \"My key with special (%, ±) symbols\" = \"value\"",
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

private fun String.trimMultilineQuotes(quotes: String): String {
    // if a suffix is a separator on a separate string, we need to trim whitespaces for a better user experience
    // name = '''
    // this is a "literal" multiline
    // string
    // <these whitespaces also should be removed>'''
    if (this.startsWith(quotes) && this.endsWith(quotes)) {
        val trimmedStr = this.removePrefix(quotes).removeSuffix(quotes)
        val lastNewLine = trimmedStr.lastIndexOf(newLineChar())
        if (lastNewLine != -1 &&
                // if there are only spaces after a new line - we can trim them
                // (this means that closing quotes were on a separate line):
                // """
                // aaa
                // """
                trimmedStr.removePrefix(newLineChar().toString()).substring(lastNewLine).all { it == ' ' }) {
            return trimmedStr.substring(0, lastNewLine + 1)
        }
        // we haven't found newlines (weird) or have found, but quotes are not on a separate line:
        // """
        // aaa """ <- in this case nothing to remove
        return trimmedStr
    }
    return this
}

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
