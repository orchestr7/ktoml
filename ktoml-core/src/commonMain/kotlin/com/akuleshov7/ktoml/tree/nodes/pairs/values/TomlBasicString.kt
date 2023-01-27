package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
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
    }
}
