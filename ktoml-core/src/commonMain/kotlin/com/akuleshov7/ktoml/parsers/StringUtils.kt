/**
 * Common String Utilities
 */

package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.utils.convertSpecialCharacters
import com.akuleshov7.ktoml.utils.newLineChar
import com.akuleshov7.ktoml.utils.validateIntegerSyntax

private const val MULTILINE_STRING_QUOTE_LENGTH = 3
private const val TOML_DECIMAL_INTEGER_PATTERN = "(?:0|[1-9](?:_?[0-9])*)"
private const val TOML_DIGITS_PATTERN = "[0-9](?:_?[0-9])*"
private const val TOML_EXPONENT_PATTERN = "[eE][+-]?$TOML_DIGITS_PATTERN"

private val tomlFloatRegex = Regex(
    "[+-]?(?:" +
            "$TOML_DECIMAL_INTEGER_PATTERN\\.$TOML_DIGITS_PATTERN(?:$TOML_EXPONENT_PATTERN)?|" +
            "$TOML_DECIMAL_INTEGER_PATTERN$TOML_EXPONENT_PATTERN" +
            ")"
)

/**
 * Callback invoked for every character while scanning a key string: receives the character and
 * whether it lies inside a quoted segment.
 */
private typealias KeyCharAction = (ch: Char, insideQuotes: Boolean) -> Unit

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

    // simple split() method won't work here, because in such case we could break following keys:
    // a."b.c.d".e (here only three tables: a/"b.c.d"/and e)
    val dotSeparatedParts: MutableList<String> = mutableListOf()
    var currentPart = StringBuilder()
    scanKeyChars { ch, insideQuotes ->
        if (ch == '.' && !insideQuotes) {
            dotSeparatedParts.add(currentPart.toString().trim())
            currentPart = StringBuilder()
        } else {
            currentPart.append(ch)
        }
    }

    val keyPart = currentPart.toString().trim()
    dotSeparatedParts.add(keyPart)
    dotSeparatedParts.forEach { it.validateKeyPart(lineNo, this) }
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
@Suppress("TOO_LONG_FUNCTION")
internal fun String.convertLineEndingBackslash(): String {
    val result = StringBuilder(length)
    var i = 0

    while (i < length) {
        if (this[i] != '\\') {
            result.append(this[i])
            i++
            continue
        }

        val slashStart = i
        while (i < length && this[i] == '\\') {
            i++
        }
        val slashCount = i - slashStart

        if (slashCount % 2 == 1) {
            val continuationStart = i
            var j = continuationStart
            while (j < length && this[j].isWhitespace() && lineBreakLengthAt(j) == 0) {
                j++
            }

            val lineBreakLength = lineBreakLengthAt(j)
            if (lineBreakLength > 0) {
                repeat(slashCount - 1) {
                    result.append('\\')
                }

                j += lineBreakLength
                while (j < length && this[j].isWhitespace()) {
                    j++
                }

                i = j
                continue
            }
        }

        repeat(slashCount) {
            result.append('\\')
        }
    }

    return result.toString()
}

/**
 * Checks if the backslash at the given index is a line-ending backslash.
 * A line-ending backslash is followed only by whitespace characters and then
 * a newline character.
 *
 * @param backslashIndex The index of the backslash to check
 * @return `true` if the backslash is a line-ending backslash, `false` otherwise
 */
internal fun String.isLineEndingBackslash(backslashIndex: Int): Boolean {
    if (backslashIndex !in indices || this[backslashIndex] != '\\') {
        return false
    }

    val continuationStart = backslashIndex + 1
    var j = continuationStart
    while (j < length && this[j].isWhitespace() && lineBreakLengthAt(j) == 0) {
        j++
    }

    return lineBreakLengthAt(j) > 0
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
 * Parses a TOML key part into the AST form.
 *
 * Double-quoted keys use basic-string escaping, single-quoted keys are literal,
 * and bare keys are returned as-is.
 *
 * @param lineNo the line number for error reporting
 * @return the unquoted and unescaped key name
 */
internal fun String.parseKeyName(lineNo: Int): String = when {
    startsWith('"') && endsWith('"') -> trimQuotes().convertSpecialCharacters(lineNo)
    startsWith('\'') && endsWith('\'') -> trimSingleQuotes()
    else -> this
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
 * Classifies integer candidates without preventing fallback to other TOML primitive types.
 *
 * @return true when the shared integer syntax validator accepts this value
 */
internal fun String.isValidTomlIntegerLiteral(): Boolean = try {
    validateIntegerSyntax(0)
    true
} catch (ex: ParseException) {
    false
}

/**
 * Checks whether this string is a TOML float literal.
 *
 * @return true for valid decimal float literals, excluding special values handled separately
 */
internal fun String.isValidTomlFloatLiteral(): Boolean = tomlFloatRegex.matches(this)

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
        } else if (symbol != '\"' && symbol != '\'') {
            idx += 1
            continue
        }

        val quoteStr = currentQuoteStr
        if (quoteStr == null) {
            if (idx + 2 < chars.length && chars[idx + 1] == symbol && chars[idx + 2] == symbol) {
                currentQuoteStr = "$symbol$symbol$symbol"
                idx += MULTILINE_STRING_QUOTE_LENGTH
                continue  // Skip the default increment
            } else {
                currentQuoteStr = symbol.toString()
            }
        } else if (quoteStr[0] == symbol && (idx + quoteStr.length) <= chars.length) {
            val candidate = chars.substring(idx, idx + quoteStr.length)
            if (candidate == quoteStr) {
                currentQuoteStr = null
                idx += candidate.length
                continue  // Skip the default increment
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

private fun String.validateKeyPart(lineNo: Int, fullKey: String) {
    if (isEmpty()) {
        throw ParseException(
            "Not able to parse the key: [$fullKey] as it contains an empty key part.",
            lineNo
        )
    }

    validateSpaces(lineNo, fullKey)
    validateQuoteBoundaries(lineNo, fullKey)
}

private fun String.validateSpaces(lineNo: Int, fullKey: String) {
    if (this.trim().count { it == ' ' } > 0 && this.isNotQuoted()) {
        throw ParseException(
            "Not able to parse the key: [$fullKey] as it has invalid spaces." +
                    " If you would like to have spaces in the middle of the key - use quotes: \"WORD SPACE\"", lineNo
        )
    }
}

private fun String.validateQuoteBoundaries(lineNo: Int, fullKey: String) {
    if (startsWith("\"\"\"") || startsWith("'''")) {
        throw ParseException(
            "Not able to parse the key: [$fullKey] as multiline strings cannot be used as keys.",
            lineNo
        )
    }

    val first = first()
    val isQuoted = first == '"' || first == '\''
    if (isQuoted) {
        if (last() != first || !isSingleQuotedKeyToken(first)) {
            throw ParseException(
                "Not able to parse the key: [$fullKey] as quoted key parts must be fully quoted.",
                lineNo
            )
        }
    } else if (any { it == '"' || it == '\'' }) {
        throw ParseException(
            "Not able to parse the key: [$fullKey] as quoted key parts must be fully quoted.",
            lineNo
        )
    }
}

private fun String.isSingleQuotedKeyToken(quote: Char): Boolean {
    var index = 1
    while (index < lastIndex) {
        val ch = this[index]
        if (quote == '"' && ch == '\\' && index + 1 < lastIndex) {
            index += 2
            continue
        }
        if (ch == quote) {
            return false
        }
        index++
    }
    return true
}

/**
 * Validation for quotes: every opening quote in a key must be closed. The scan is quote-aware,
 * so a single quote that appears inside a basic-quoted key (and vice versa) is treated as content
 * rather than a delimiter, and an escaped quote (`\"`) inside a basic-quoted key does not count.
 */
private fun String.validateQuotes(lineNo: Int) {
    val allQuotesClosed = scanKeyChars { _, _ -> }
    if (!allQuotesClosed) {
        throw ParseException(
            "Not able to parse the key: [$this] as it does not have closing quote." +
                    " Please note, that you cannot use even escaped quotes in the bare keys.",
            lineNo
        )
    }
}

/**
 * validate that bare key parts (not quoted) contain only valid symbols A..Z, a..z, 0..9, -, _
 *
 * The scan is quote-aware: characters inside basic (`"`) or literal (`'`) quoted parts are not
 * validated, and a backslash escape (e.g. `\"`) inside a basic-quoted part is skipped so the
 * escaped quote is not mistaken for a closing delimiter.
 */
private fun String.validateSymbols(lineNo: Int) {
    this.trim().scanKeyChars { ch, insideQuotes ->
        if (!insideQuotes &&
                // FixMe: isLetterOrDigit is not supported in Kotlin 1.4, but 1.5 is not compiling right now
                !setOf('_', '-', '.', ' ', '\t').contains(ch) && !ch.isLetterOrDigit()
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

/**
 * Walks a (possibly dotted) key string left to right, calling [action] for every character with a
 * flag telling whether it is currently inside a quoted segment.
 *
 * The scan is quote-aware: a literal string (`'...'`) is taken verbatim while a basic string (`"..."`)
 * honors `\` escapes, so an escaped quote (`\"`) inside a basic string is reported as quoted content
 * and never mistaken for a closing delimiter. Quote characters themselves and the escaped pair are
 * reported with `insideQuotes = true`; only bare characters outside any quotes get `false`.
 *
 * @param action receives each character and whether it lies inside a quoted segment
 * @return `true` if every opened quote was closed, `false` if a quote was left open
 */
@Suppress("FUNCTION_BOOLEAN_PREFIX")
private inline fun String.scanKeyChars(action: KeyCharAction): Boolean {
    var inBasic = false
    var inLiteral = false
    var index = 0
    while (index < length) {
        val ch = this[index]
        when {
            inBasic && ch == '\\' && index + 1 < length -> {
                action(ch, true)
                action(this[index + 1], true)
                index++
            }
            ch == '\'' && !inBasic -> {
                inLiteral = !inLiteral
                action(ch, true)
            }
            ch == '\"' && !inLiteral -> {
                inBasic = !inBasic
                action(ch, true)
            }
            else -> action(ch, inBasic || inLiteral)
        }
        index++
    }
    return !inBasic && !inLiteral
}

private fun Char.isLetterOrDigit() = CharRange('A', 'Z').contains(this) ||
        CharRange('a', 'z').contains(this) ||
        CharRange('0', '9').contains(this)

private fun String.isNotQuoted() = !(this.startsWith("\"") && this.endsWith("\"")) &&
        !(this.startsWith("'") && this.endsWith("'"))

private fun String.lineBreakLengthAt(index: Int): Int = when {
    index >= length -> 0
    this[index] == '\r' && index + 1 < length && this[index + 1] == '\n' -> 2
    this[index] == '\r' || this[index] == '\n' -> 1
    else -> 0
}

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
