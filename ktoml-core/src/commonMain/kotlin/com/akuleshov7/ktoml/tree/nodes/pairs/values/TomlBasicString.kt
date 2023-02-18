package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.parsers.convertLineEndingBackslash
import com.akuleshov7.ktoml.parsers.getCountOfOccurrencesOfSubstring
import com.akuleshov7.ktoml.parsers.trimMultilineQuotes
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.utils.convertSpecialCharacters
import com.akuleshov7.ktoml.utils.escapeSpecialCharacters
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of string values: key = "value" (always should have quotes due to TOML standard)
 * @property content
 * @property multiline Whether the string is multiline.
 */
public class TomlBasicString internal constructor(
    override var content: Any,
    public var multiline: Boolean = false
) : TomlValue() {
    public constructor(
        content: String,
        lineNo: Int
    ) : this(content.verifyAndTrimQuotes(lineNo))

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        val content = content as String

        emitter.emitValue(
            content.escapeSpecialCharacters(multiline),
            isLiteral = false,
            multiline
        )
    }

    public companion object {
        private fun String.verifyAndTrimQuotes(lineNo: Int): Any =
                when {
                    // ====== multiline string (""") =======
                    startsWith("\"\"\"") && endsWith("\"\"\"") ->
                        trimMultilineQuotes()
                            .checkCountOfOtherUnescapedQuotes(lineNo)
                            .convertLineEndingBackslash()
                            .convertSpecialCharacters(lineNo)

                    // ========= basic string ("abc") =======
                    startsWith("\"") && endsWith("\"") ->
                        trimQuotes()
                            .checkOtherQuotesAreEscaped(lineNo)
                            .convertSpecialCharacters(lineNo)

                    // ============= other ===========
                    else ->
                        throw ParseException(
                            "According to the TOML specification string values (even Enums)" +
                                    " should be wrapped (start and end) with quotes (\"\")," +
                                    " but the following value was not: <$this>.",
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

        private fun String.checkCountOfOtherUnescapedQuotes(lineNo: Int): String {
            // Here we do replace because the following is valid: a = """ \""" """
            // We have 1 escaped quote + 2 unescaped quotes
            if (this.replace("\\\"", " ").getCountOfOccurrencesOfSubstring("\"\"\"") != 0) {
                throw ParseException(
                    "Multi-line basic string cannot contain 3 or more quotes (\") in a row." +
                            " Please remove the quotes or use escaping in <$this>",
                    lineNo
                )
            }
            return this
        }
    }
}
