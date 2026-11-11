/**
 * This file contains utility methods for correct processing of escaped characters.
 * This logic is used for processing of Chars, Literal Strings and basic Strings.
 *
 * In TOML we need properly process escaped symbols like '\t', '\n', unicode symbols and other.
 * For Literal Strings ('') these symbols should be parsed "as is", for basic strings ("") and chars ('')
 * they should be decoded to proper characters.
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.UnknownEscapeSymbolsException
import com.akuleshov7.ktoml.parsers.isLineEndingBackslash

internal const val COMPLEX_UNICODE_LENGTH = 8
internal const val COMPLEX_UNICODE_PREFIX = 'U'
internal const val DEC_RADIX = 10
internal const val BIN_RADIX = 2
internal const val OCT_RADIX = 8
internal const val HEX_RADIX = 16
internal const val SIMPLE_UNICODE_LENGTH = 4
internal const val SIMPLE_UNICODE_PREFIX = 'u'

// TOML 1.1 hex escape (\xHH) — exactly two hex digits, equivalent to \u00HH
internal const val HEX_ESCAPE_LENGTH = 2
internal const val HEX_ESCAPE_PREFIX = 'x'

private const val MIN_UNICODE_CODE_POINT = 0x0000
private const val MIN_SURROGATE_CODE_POINT = 0xD800
private const val MAX_SURROGATE_CODE_POINT = 0xDFFF
private const val MAX_UNICODE_CODE_POINT = 0x10_FF_FF

// TOML 1.1 escape escape (\e) — the ESC control character (U+001B)
internal const val ESCAPE_CHAR = '\u001B'

/**
 * Converting special escaped symbols like newlines, tabs and unicode symbols to proper characters for decoding
 *
 * @param lineNo line number of a string
 * @return returning a string with converted escaped special symbols
 * @throws ParseException if unknown escaped symbols were used
 * @throws UnknownEscapeSymbolsException
 */
@InternalKtomlApi
public fun String.convertSpecialCharacters(lineNo: Int): String {
    val resultString = StringBuilder()
    var i = 0
    while (i < length) {
        val currentChar = get(i)
        var offset = 1
        if (currentChar == '\\') {
            if (i == lastIndex) {
                throw UnknownEscapeSymbolsException("", lineNo)
            }
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
                'e' -> resultString.append(ESCAPE_CHAR)
                SIMPLE_UNICODE_PREFIX, COMPLEX_UNICODE_PREFIX, HEX_ESCAPE_PREFIX ->
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
@InternalKtomlApi
public fun StringBuilder.appendEscapedUnicode(
    fullString: String,
    marker: Char,
    codeStartIndex: Int,
    lineNo: Int
): Int {
    val nbUnicodeChars = when (marker) {
        HEX_ESCAPE_PREFIX -> HEX_ESCAPE_LENGTH
        SIMPLE_UNICODE_PREFIX -> SIMPLE_UNICODE_LENGTH
        else -> COMPLEX_UNICODE_LENGTH
    }
    if (codeStartIndex + nbUnicodeChars > fullString.length) {
        val invalid = fullString.substring(codeStartIndex - 1)
        throw UnknownEscapeSymbolsException("\\$invalid", lineNo)
    }
    val hexCode = fullString.substring(codeStartIndex, codeStartIndex + nbUnicodeChars)
    if (!hexCode.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        throw UnknownEscapeSymbolsException("\\$marker$hexCode", lineNo)
    }
    val codePoint = try {
        hexCode.toInt(HEX_RADIX)
    } catch (e: NumberFormatException) {
        throw UnknownEscapeSymbolsException("\\$marker$hexCode", lineNo)
    }
    if (!codePoint.isUnicodeScalarValue()) {
        throw UnknownEscapeSymbolsException("\\$marker$hexCode", lineNo)
    }
    appendCodePointCompat(codePoint)
    return nbUnicodeChars
}

/**
 * Escaping special characters for encoding
 *
 * @param multiline
 * @return converted string with escaped special symbols
 */
@InternalKtomlApi
public fun String.escapeSpecialCharacters(multiline: Boolean = false): String =
    if (multiline) {
        escapeControlChars(isMultiline = true)
            .replace("\"\"\"", "\"\"\\\"")
    } else {
        escapeControlChars(isMultiline = false)
            .replace("\"", "\\\"")
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

/**
 * Checks if a character is disallowed by TOML's source character rules.
 * TOML permits tab, printable Unicode scalar values, and line feeds only as line terminators.
 *
 * @return true when this character is a forbidden TOML control character
 */
internal fun Char.isTomlControlChar(): Boolean = this in '\u0000'..'\u0008' ||
        this in '\u000A'..'\u001F' || this == '\u007F'

/**
 * Rejects raw control characters that TOML does not allow in source text.
 *
 * @param lineNo line number of the string for error reporting
 * @param allowLineFeed whether LF is valid here as a multiline string line separator
 * @return the original string when it contains no illegal control characters
 * @throws ParseException if an illegal control character is found
 */
internal fun String.checkNoTomlControlChars(lineNo: Int, allowLineFeed: Boolean = false): String {
    forEach { char ->
        if (char.isForbiddenTomlControlChar(allowLineFeed)) {
            throw ParseException(
                "Control character U+${char.code.toString(HEX_RADIX).padStart(SIMPLE_UNICODE_LENGTH, '0')}" +
                        " is not allowed here. Please check: <$this>",
                lineNo
            )
        }
    }
    return this
}

/**
 * Rejects a bare carriage return inside string content. A `\r` is only allowed as part of a `\r\n`
 * line ending; a `\r` that is not immediately followed by `\n` is an illegal control character.
 * CRLF line endings are normalized to LF before parsing, so a surviving lone `\r` is always bare.
 *
 * @param lineNo line number of the string for error reporting
 * @return the original string when it contains no bare carriage return
 * @throws ParseException if a bare carriage return is found
 */
internal fun String.checkNoBareCarriageReturn(lineNo: Int): String {
    forEachIndexed { index, char ->
        if (char == '\r' && (index == lastIndex || this[index + 1] != '\n')) {
            throw ParseException(
                "A bare carriage return (\\r) is not allowed in a string; it is only permitted" +
                        " as part of a \\r\\n line ending. Please check: <$this>",
                lineNo
            )
        }
    }
    return this
}

private fun Char.isForbiddenTomlControlChar(allowLineFeed: Boolean): Boolean =
    isTomlControlChar() && (this != '\n' || !allowLineFeed)

private fun String.escapeControlChars(isMultiline: Boolean): String {
    val isControlChar = if (isMultiline) {
        Char::isMultilineControlChar
    } else {
        Char::isControlChar
    }

    val sb = StringBuilder(length)
    var slashCount = 0
    for ((i, char) in withIndex()) {
        if (char == '\\') {
            slashCount++
            continue
        }

        if (slashCount > 0) {
            sb.append(
                escapeBackslashes(lastBackslashIndex = i - 1, slashCount = slashCount, isMultiline = isMultiline)
            )
            slashCount = 0
        }

        if (isControlChar(char)) {
            sb.append(char.escapeControlChar())
        } else {
            sb.append(char)
        }
    }

    if (isNotEmpty() && slashCount > 0) {
        sb.append(
            escapeBackslashes(lastBackslashIndex = length - 1, slashCount = slashCount, isMultiline = isMultiline)
        )
    }

    return sb.toString()
}

private fun String.escapeBackslashes(
    lastBackslashIndex: Int,
    slashCount: Int,
    isMultiline: Boolean,
): String {
    val isLineEndingBackslash = slashCount % 2 != 0 &&
            isMultiline &&
            isLineEndingBackslash(lastBackslashIndex)

    return if (isLineEndingBackslash) {
        "\\".repeat(slashCount * 2 - 1)
    } else {
        "\\".repeat(slashCount * 2)
    }
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

private fun Int.isUnicodeScalarValue(): Boolean = this in MIN_UNICODE_CODE_POINT..MAX_UNICODE_CODE_POINT &&
        this !in MIN_SURROGATE_CODE_POINT..MAX_SURROGATE_CODE_POINT

/**
 * just a newline character on different platforms/targets
 *
 * @return newline
 */
@InternalKtomlApi
public expect fun newLineChar(): Char
