package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.utils.appendCodePointCompat
import com.akuleshov7.ktoml.utils.controlCharacterRegex
import com.akuleshov7.ktoml.utils.unescapedBackslashRegex
import com.akuleshov7.ktoml.utils.unescapedDoubleQuoteRegex
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of string values: key = "value" (always should have quotes due to TOML standard)
 * @property content
 */
public class TomlBasicString internal constructor(
    override var content: Any,
    lineNo: Int
) : TomlValue(lineNo) {
    public constructor(
        content: String,
        lineNo: Int
    ) : this(content.verifyAndTrimQuotes(lineNo), lineNo)

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        if (multiline) {
            throw TomlWritingException(
                "Multiline strings are not yet supported."
            )
        }

        val content = content as String

        emitter.emitValue(
            content.escapeSpecialCharacters(),
            isLiteral = false,
            multiline
        )
    }

    public companion object {
        private const val COMPLEX_UNICODE_LENGTH = 8
        private const val COMPLEX_UNICODE_PREFIX = 'U'
        private const val HEX_RADIX = 16
        private const val SIMPLE_UNICODE_LENGTH = 4
        private const val SIMPLE_UNICODE_PREFIX = 'u'

        private fun String.verifyAndTrimQuotes(lineNo: Int): Any =
                when {
                    // ========= basic string ("abc") =======
                    startsWith("\"") && endsWith("\"") ->
                        trimQuotes()
                            .checkOtherQuotesAreEscaped(lineNo)
                            .convertSpecialCharacters(lineNo)
                    // ====== multiline string (''') =======

                    // ============= other ===========
                    else ->
                        throw ParseException(
                            "According to the TOML specification string values (even Enums)" +
                                    " should be wrapped (start and end) with quotes (\"\"), but the following value was not: <$this>." +
                                    " Please note that multiline strings are not yet supported.",
                            lineNo
                        )
                }

        private fun String.checkOtherQuotesAreEscaped(lineNo: Int): String {
            this.forEachIndexed { index, ch ->
                if (ch == '\"' && (index == 0 || this[index - 1] != '\\')) {
                    throw ParseException(
                        "Found invalid quote that is not escaped." +
                                " Please remove the quote or use escaping" +
                                " in <$this> at position = [$index].", lineNo
                    )
                }
            }
            return this
        }

        private fun String.convertSpecialCharacters(lineNo: Int): String {
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
                        '\\' -> resultString.append('\\')
                        '\'' -> resultString.append('\'')
                        '"' -> resultString.append('"')
                        SIMPLE_UNICODE_PREFIX, COMPLEX_UNICODE_PREFIX ->
                            offset += resultString.appendEscapedUnicode(this, next, i + 2, lineNo)
                        else -> throw ParseException(
                            "According to TOML documentation unknown" +
                                    " escape symbols are not allowed. Please check: [\\$next]",
                            lineNo
                        )
                    }
                } else {
                    resultString.append(currentChar)
                }
                i += offset
            }
            return resultString.toString()
        }

        private fun StringBuilder.appendEscapedUnicode(
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
                throw ParseException(
                    "According to TOML documentation unknown" +
                            " escape symbols are not allowed. Please check: [\\$invalid]",
                    lineNo
                )
            }
            val hexCode = fullString.substring(codeStartIndex, codeStartIndex + nbUnicodeChars)
            val codePoint = hexCode.toInt(HEX_RADIX)
            try {
                appendCodePointCompat(codePoint)
            } catch (e: IllegalArgumentException) {
                throw ParseException(
                    "According to TOML documentation unknown" +
                            " escape symbols are not allowed. Please check: [\\$marker$hexCode]",
                    lineNo
                )
            }
            return nbUnicodeChars
        }

        private fun String.escapeSpecialCharacters(): String {
            val withCtrlCharsEscaped = replace(controlCharacterRegex) { match ->
                when (val char = match.value.single()) {
                    '\b' -> "\\b"
                    '\n' -> "\\n"
                    '\u000C' -> "\\f"
                    '\r' -> "\\r"
                    else -> {
                        val code = char.code

                        val hexDigits = code.toString(HEX_RADIX)

                        "\\$SIMPLE_UNICODE_PREFIX${
                            hexDigits.padStart(SIMPLE_UNICODE_LENGTH, '0')
                        }"
                    }
                }
            }

            val withQuotesEscaped = withCtrlCharsEscaped.replace(unescapedDoubleQuoteRegex) { match ->
                match.value.replace("\"", "\\\"")
            }

            return withQuotesEscaped.replace(
                unescapedBackslashRegex,
                Regex.escapeReplacement("\\\\")
            )
        }
    }
}
