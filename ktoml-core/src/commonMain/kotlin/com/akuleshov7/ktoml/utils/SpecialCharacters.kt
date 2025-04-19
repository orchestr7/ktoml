/**
 * This file contains utility methods for correct processing of escaped characters.
 * This logic is used for processing of Chars, Literal Strings and basic Strings.
 *
 * In TOML we need properly process escaped symbols like '\t', '\n', unicode symbols and other.
 * For Literal Strings ('') these symbols should be parsed "as is", for basic strings ("") and chars ('')
 * they should be decoded to proper characters.
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.exceptions.UnknownEscapeSymbolsException

internal const val COMPLEX_UNICODE_LENGTH = 8
internal const val COMPLEX_UNICODE_PREFIX = 'U'
internal const val BIN_RADIX = 2
internal const val OCT_RADIX = 8
internal const val HEX_RADIX = 16
internal const val SIMPLE_UNICODE_LENGTH = 4
internal const val SIMPLE_UNICODE_PREFIX = 'u'

/**
 * Converting special escaped symbols like newlines, tabs and unicode symbols to proper characters for decoding
 *
 * @param lineNo line number of a string
 * @return returning a string with converted escaped special symbols
 * @throws ParseException if unknown escaped symbols were used
 * @throws UnknownEscapeSymbolsException
 */
public fun String.convertSpecialCharacters(lineNo: Int): String {
    val resultString = StringBuilder()
    var i = 0
    while (i < length) {
        val currentChar = get(i)
        var offset = 1
        if (currentChar == '\\' && i != lastIndex) {
            // Escaped
            val next = get(i + 1)
            offset++
            when (next) {
                't' -> resultString.append('\t')
                'b' -> resultString.append('\b')
                'r' -> resultString.append('\r')
                'n' -> resultString.append('\n')
                'f' -> resultString.append('\u000C')
                '\\' -> resultString.append('\\')
                '\'' -> resultString.append('\'')
                '"' -> resultString.append('"')
                SIMPLE_UNICODE_PREFIX, COMPLEX_UNICODE_PREFIX ->
                    offset += resultString.appendEscapedUnicode(this, next, i + 2, lineNo)

                else -> throw UnknownEscapeSymbolsException("\\$next", lineNo)
            }
        } else {
            resultString.append(currentChar)
        }
        i += offset
    }
    return resultString.toString()
}

/**
 * Escaping and converting unicode symbols for decoding
 *
 * @param fullString
 * @param marker
 * @param codeStartIndex
 * @param lineNo line number of a string
 * @return position of
 * @throws ParseException
 * @throws UnknownEscapeSymbolsException
 */
public fun StringBuilder.appendEscapedUnicode(
    fullString: String,
    marker: Char,
    codeStartIndex: Int,
    lineNo: Int
): Int {
    val nbUnicodeChars = if (marker == SIMPLE_UNICODE_PREFIX) {
        SIMPLE_UNICODE_LENGTH
    } else {
        COMPLEX_UNICODE_LENGTH
    }
    if (codeStartIndex + nbUnicodeChars > fullString.length) {
        val invalid = fullString.substring(codeStartIndex - 1)
        throw UnknownEscapeSymbolsException("\\$invalid", lineNo)
    }
    val hexCode = fullString.substring(codeStartIndex, codeStartIndex + nbUnicodeChars)
    val codePoint = hexCode.toInt(HEX_RADIX)
    try {
        appendCodePointCompat(codePoint)
    } catch (e: IllegalArgumentException) {
        throw UnknownEscapeSymbolsException("\\$marker$hexCode", lineNo)
    }
    return nbUnicodeChars
}

/**
 * Escaping special characters for encoding
 *
 * @param multiline
 * @return converted string with escaped special symbols
 */
public fun String.escapeSpecialCharacters(multiline: Boolean = false): String =
    if (multiline) {
        escapeControlChars(Char::isMultilineControlChar)
            .replace("\"\"\"", "\"\"\\\"")
            .escapeBackslashes("btnfruU\"\r\n")
    } else {
        escapeControlChars(Char::isControlChar)
            .replace("\"", "\\\"")
            .escapeBackslashes("btnfruU\"")
    }

/**
 * Checks if a character is an invalid control character for strings. Specifically,
 * this returns true for characters in Unicode block [`Cc`](https://www.compart.com/en/unicode/category/Cc)
 * excluding tab.
 *
 * Used to replace these characters with valid Unicode escapes when writing basic
 * strings, or throw an exception for them when writing literal strings.
 *
 * @return `true` if the character is a control character, except tab.
 */
internal fun Char.isControlChar() = this in CharCategory.CONTROL && this != '\t'

/**
 * Checks if a character is an invalid control character for multiline strings.
 * This is the same as [isControlChar], but excludes line terminators.
 *
 * @return `true` if the character is a control character, except tab and line
 * terminators.
 */
internal fun Char.isMultilineControlChar() = isControlChar() && this !in "\n\r"

private inline fun String.escapeControlChars(predicate: (Char) -> Boolean): String {
    val sb = StringBuilder(length)
    var last = 0
    for ((i, char) in withIndex()) {
        if (predicate(char)) {
            sb.append(this, last, i)
                .append(char.escapeControlChar())
            last = i + 1
        }
    }

    if (last < length) {
        sb.append(this, last, length)
    }

    return sb.toString()
}

private fun Char.escapeControlChar() = when (this) {
    '\t' -> "\\t"
    '\b' -> "\\b"
    '\n' -> "\\n"
    '\u000C' -> "\\f"
    '\r' -> "\\r"
    else -> {
        val hexDigits = code.toString(HEX_RADIX)

        "\\$SIMPLE_UNICODE_PREFIX${
            hexDigits.padStart(SIMPLE_UNICODE_LENGTH, '0')
        }"
    }
}

private fun String.escapeBackslashes(escapes: String): String {
    val sb = StringBuilder(length)
    var slashCount = 0
    var last = 0
    for ((i, char) in withIndex()) {
        if (char == '\\') {
            slashCount++
        } else {
            if (slashCount > 0 && char !in escapes && slashCount % 2 != 0) {
                sb.append(this, last, i - 1)
                    .append("\\\\$char")
                last = i + 1
            }

            slashCount = 0
        }
    }

    if (last < length) {
        sb.append(this, last, length)
    }

    return sb.toString()
}

/**
 * just a newline character on different platforms/targets
 *
 * @return newline
 */
public expect fun newLineChar(): Char
