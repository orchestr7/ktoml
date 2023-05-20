package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.convertLineEndingBackslash
import com.akuleshov7.ktoml.parsers.getCountOfOccurrencesOfSubstring
import com.akuleshov7.ktoml.parsers.trimMultilineLiteralQuotes
import com.akuleshov7.ktoml.parsers.trimSingleQuotes
import com.akuleshov7.ktoml.utils.isControlChar
import com.akuleshov7.ktoml.utils.isMultilineControlChar
import com.akuleshov7.ktoml.utils.newLineChar
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of literal string values: key = 'value' (with single quotes and no escaped symbols)
 * The only difference from the TOML specification (https://toml.io/en/v1.0.0) is that we will have one escaped symbol -
 * single quote and so it will be possible to use a single quote inside.
 * @property content
 * @property multiline Whether the string is multiline.
 */
public class TomlLiteralString internal constructor(
    override var content: Any,
    public var multiline: Boolean = false
) : TomlValue() {
    public constructor(
        content: String,
        lineNo: Int,
        config: TomlInputConfig = TomlInputConfig()
    ) : this(content.verifyAndTrimQuotes(lineNo, config), content.contains(newLineChar()))

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        content: String,
        lineNo: Int,
        config: TomlConfig
    ) : this(
        content,
        lineNo,
        config.input
    )

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig,
    ) {
        val content = content as String

        emitter.emitValue(
            content.escapeQuotesAndVerify(config, multiline),
            isLiteral = true,
            multiline
        )
    }

    public companion object {
        private fun String.verifyAndTrimQuotes(lineNo: Int, config: TomlInputConfig): Any =
            when {
                // ====== multiline string (''') =======
                startsWith("'''") && endsWith("'''") -> {
                    val contentString = trimMultilineLiteralQuotes()
                        .checkCountOfOtherQuotes(lineNo)
                    val rawContent = if (config.allowEscapedQuotesInLiteralStrings) {
                        contentString.convertSingleQuotes()
                    } else {
                        contentString
                    }
                    rawContent.convertLineEndingBackslash()
                }
                // ====== basic literal string (') =======
                startsWith("'") && endsWith("'") -> {
                    val contentString = trimSingleQuotes()
                    if (config.allowEscapedQuotesInLiteralStrings) contentString.convertSingleQuotes() else contentString
                }
                else ->
                    // here we complain only about non-multiline strings, as for multiline string we have the same logic in parsing
                    throw ParseException(
                        "Literal string should be wrapped with single quotes (''), it looks that you have forgotten" +
                                " the single quote in the end of the following string: <$this>", lineNo
                    )
            }

        /**
         * According to the TOML standard (https://toml.io/en/v1.0.0#string) single quote is prohibited.
         * But in ktoml we don't see any reason why we cannot escape it. Anyway, by the TOML specification we should fail, so
         * why not to try to handle this situation at least somehow.
         *
         * Conversion is done after we have trimmed technical quotes and won't break cases when the user simply used a backslash
         * as the last symbol (single quote) will be removed.
         */
        private fun String.convertSingleQuotes(): String = this.replace("\\'", "'")

        private fun String.escapeQuotesAndVerify(config: TomlOutputConfig, multiline: Boolean) =
            when {
                multiline ->
                    when {
                        any(Char::isMultilineControlChar) ->
                            throw TomlWritingException(
                                "Control characters (excluding tab and line" +
                                        " terminators) are not permitted in" +
                                        " multiline literal strings. Please check: <$this>"
                            )

                        config.allowEscapedQuotesInLiteralStrings ->
                            replace("'''", "''\\'")

                        "'''" in this ->
                            throw TomlWritingException(
                                "Three or more consecutive single quotes are not" +
                                        " permitted in multiline literal strings. Please check: <$this>"
                            )

                        else -> this
                    }

                any(Char::isControlChar) ->
                    throw TomlWritingException(
                        "Control characters (excluding tab) are not permitted" +
                                " in literal strings. Please check: <$this>"
                    )

                '\'' in this ->
                    if (config.allowEscapedQuotesInLiteralStrings) {
                        replace("'", "\\'")
                    } else {
                        throw TomlWritingException(
                            "Single quotes are not permitted in literal string" +
                                    " by default. Set allowEscapedQuotesInLiteral" +
                                    "Strings to true in the config to ignore this. Please check: <$this>"
                        )
                    }

                else -> this
            }

        private fun String.checkCountOfOtherQuotes(lineNo: Int): String {
            if (this.replace("\\'", " ").getCountOfOccurrencesOfSubstring("'''") != 0) {
                throw ParseException(
                    "Multi-line literal basic string cannot contain 3 or more quotes (') in a row." +
                            " Please remove the quotes or set allowEscapedQuotesInLiteral " +
                            "Strings to true in the config and add escaping<$this>",
                    lineNo
                )
            }
            return this
        }
    }
}
