package com.akuleshov7.ktoml.tree.nodes.pairs.values

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.removeTrailingComma
import com.akuleshov7.ktoml.parsers.trimBrackets
import com.akuleshov7.ktoml.tree.nodes.parseValue
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Toml AST Node for a representation of arrays: key = [value1, value2, value3]
 * @property content
 * @property multiline
 */
public class TomlArray internal constructor(
    override var content: Any,
    public var multiline: Boolean = false
) : TomlValue() {
    public constructor(
        rawContent: String,
        lineNo: Int,
        config: TomlInputConfig
    ) : this(rawContent.parse(lineNo, config))

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        rawContent: String,
        lineNo: Int,
        config: TomlConfig
    ) : this(rawContent)

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases.",
        replaceWith = ReplaceWith(
            "parse(config)",
            "com.akuleshov7.ktoml.TomlInputConfig"
        )
    )
    public fun parse(config: TomlConfig): List<Any> = parse(config.input)

    /**
     * small adaptor to make proper testing of parsing
     *
     * @param config
     * @return converted array to a list
     */
    @Deprecated(
        message = "parse(TomlInputConfig) is deprecated. Will be removed in next releases.",
        replaceWith = ReplaceWith("content as List<Any>"),

    )
    @Suppress("UNCHECKED_CAST")
    public fun parse(config: TomlInputConfig = TomlInputConfig()): List<Any> = content as List<Any>

    @Suppress("UNCHECKED_CAST")
    public override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        emitter.startArray()

        val content = (content as List<Any>).map {
            if (it is TomlArray) {
                TomlArray(it.content, multiline)
            } else {
                it as TomlValue
            }
        }

        val last = content.lastIndex

        if (multiline) {
            emitter.indent()

            content.forEachIndexed { i, value ->
                emitter.emitNewLine()
                    .emitIndent()

                value.write(emitter, config)

                if (i < last) {
                    emitter.emitElementDelimiter()
                }
            }

            emitter.dedent()
            emitter.emitNewLine()
                .emitIndent()
        } else {
            content.forEachIndexed { i, value ->
                emitter.emitWhitespace()

                value.write(emitter, config)

                if (i < last) {
                    emitter.emitElementDelimiter()
                }
            }

            emitter.emitWhitespace()
        }

        emitter.endArray()
    }

    public companion object {
        /**
         * recursively parse TOML array from the string: [ParsingArray -> Trimming values -> Parsing Nested Arrays]
         */
        private fun String.parse(lineNo: Int, config: TomlInputConfig = TomlInputConfig()): List<Any> =
            this.parseArray(lineNo)
                .map { it.trim() }
                .map {
                    if (it.startsWith("[")) {
                        TomlArray(it, lineNo, config)
                    } else {
                        it.parseValue(lineNo, config)
                    }
                }

        /**
         * method for splitting the string to the array: "[[a, b], [c], [d]]" to -> [a,b] [c] [d]
         */
        @Suppress("NESTED_BLOCK", "TOO_LONG_FUNCTION")
        private fun String.parseArray(lineNo: Int): MutableList<String> {
            val trimmed = trimBrackets().trim().removeTrailingComma()
            // covering cases when the array is intentionally blank: myArray = []. It should be empty and not contain null
            if (trimmed.isBlank()) {
                return mutableListOf()
            }

            var nbBrackets = 0
            var isInBasicString = false
            var isInLiteralString = false
            var bufferBetweenCommas = StringBuilder()
            val result: MutableList<String> = mutableListOf()

            for (i in trimmed.indices) {
                when (val current = trimmed[i]) {
                    '[' -> {
                        if (!isInBasicString && !isInLiteralString) {
                            nbBrackets++
                        }
                        bufferBetweenCommas.append(current)
                    }
                    ']' -> {
                        if (!isInBasicString && !isInLiteralString) {
                            nbBrackets--
                        }
                        bufferBetweenCommas.append(current)
                    }
                    '\'' -> {
                        if (!isInBasicString) {
                            isInLiteralString = !isInLiteralString
                        }
                        bufferBetweenCommas.append(current)
                    }
                    '"' -> {
                        if (!isInLiteralString) {
                            if (!isInBasicString) {
                                isInBasicString = true
                            } else if (trimmed[i - 1] != '\\') {
                                isInBasicString = false
                            }
                        }
                        bufferBetweenCommas.append(current)
                    }
                    // split only if we are on the highest level of brackets (all brackets are closed)
                    // and if we're not in a string
                    ',' -> if (isInBasicString || isInLiteralString || nbBrackets != 0) {
                        bufferBetweenCommas.append(current)
                    } else {
                        result.add(bufferBetweenCommas.toString())
                        bufferBetweenCommas = StringBuilder()
                    }
                    else -> bufferBetweenCommas.append(current)
                }
            }
            if (isInBasicString || isInLiteralString) {
                throw ParseException(
                    "Not able to parse the array: [$this] as it does not have closing quote",
                    lineNo,
                )
            }
            result.add(bufferBetweenCommas.toString())
            return result
        }
    }
}
