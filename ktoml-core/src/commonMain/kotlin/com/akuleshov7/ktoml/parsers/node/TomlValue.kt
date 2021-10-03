/**
 * All representations of TOML value nodes are stored in this file
 */

package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.trimBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes

/**
 * Base class for all nodes that represent values
 * @property lineNo - line number of original file
 */
public sealed class TomlValue(public val lineNo: Int) {
    public abstract var content: Any
}

/**
 * Toml AST Node for a representation of string values: key = "value" (always should have quotes due to TOML standard)
 */
public class TomlBasicString(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = if (content.startsWith("\"") && content.endsWith("\"")) {
        val stringWithoutQuotes = content.trimQuotes()
        checkOtherQuotesAreEscaped(stringWithoutQuotes)
        convertSpecialCharacters(stringWithoutQuotes)
    } else {
        throw TomlParsingException(
            "According to the TOML specification string values (even Enums)" +
                    " should be wrapped with quotes (\"\"): <$content>", lineNo
        )
    }

    private fun checkOtherQuotesAreEscaped(stringWithoutQuotes: String) {
        stringWithoutQuotes.forEachIndexed { index, ch ->
            if (ch == '\"' && (index == 0 || stringWithoutQuotes[index - 1] != '\\')) {
                throw TomlParsingException(
                    "Found invalid quote that is not escaped." +
                            " Please remove the quote or use escaping" +
                            " in <$stringWithoutQuotes> at position = [$index].", lineNo
                )
            }
        }
    }

    private fun convertSpecialCharacters(stringWithoutQuotes: String): String {
        val resultString = StringBuilder()
        var updatedOnPreviousStep = false
        var i = 0
        while (i < stringWithoutQuotes.length) {
            val newCharacter = if (stringWithoutQuotes[i] == '\\' && i != stringWithoutQuotes.length - 1) {
                updatedOnPreviousStep = true
                when (stringWithoutQuotes[i + 1]) {
                    // table that is used to convert escaped string literals to proper char symbols
                    't' -> '\t'
                    'b' -> '\b'
                    'r' -> '\r'
                    'n' -> '\n'
                    '\\' -> '\\'
                    '\'' -> '\''
                    '"' -> '"'
                    else -> throw TomlParsingException(
                        "According to TOML documentation unknown" +
                                " escape symbols are not allowed. Please check: [\\${stringWithoutQuotes[i + 1]}]",
                        lineNo
                    )
                }
            } else {
                stringWithoutQuotes[i]
            }
            // need to skip the next character if we have processed special escaped symbol
            if (updatedOnPreviousStep) {
                updatedOnPreviousStep = false
                i += 2
            } else {
                i += 1
            }

            resultString.append(newCharacter)
        }
        return resultString.toString()
    }
}

/**
 * Toml AST Node for a representation of Arbitrary 64-bit signed integers: key = 1
 */
public class TomlLong(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toLong()
}

/**
 * Toml AST Node for a representation of float types: key = 1.01.
 * Toml specification requires floating point numbers to be IEEE 754 binary64 values,
 * so it should be Kotlin Double (64 bits)
 */
public class TomlDouble(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toDouble()
}

/**
 * Toml AST Node for a representation of boolean types: key = true | false
 */
public class TomlBoolean(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toBoolean()
}

/**
 * Toml AST Node for a representation of null:
 * null, nil, NULL, NIL or empty (key = )
 */
public class TomlNull(lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = "null"
}

/**
 * Toml AST Node for a representation of arrays: key = [value1, value2, value3]
 */
public class TomlArray(private val rawContent: String, lineNo: Int) : TomlValue(lineNo) {
    override lateinit var content: Any

    init {
        validateBrackets()
        this.content = parse()
    }

    /**
     * small adaptor to make proper testing of parsing
     *
     * @return converted array to a list
     */
    public fun parse(): List<Any> = rawContent.parse()

    /**
     * recursively parse TOML array from the string
     */
    private fun String.parse(): List<Any> =
            this.parseArray()
                .map { it.trim() }
                .map { if (it.startsWith("[")) it.parse() else it.parseValue(lineNo) }

    /**
     * method for splitting the string to the array: "[[a, b], [c], [d]]" to -> [a,b] [c] [d]
     */
    @Suppress("TOO_MANY_LINES_IN_LAMBDA")
    private fun String.parseArray(): MutableList<String> {
        var numberOfOpenBrackets = 0
        var numberOfClosedBrackets = 0
        var bufferBetweenCommas = StringBuilder()
        val result: MutableList<String> = mutableListOf()

        this.trimBrackets().forEach {
            when (it) {
                '[' -> {
                    numberOfOpenBrackets++
                    bufferBetweenCommas.append(it)
                }
                ']' -> {
                    numberOfClosedBrackets++
                    bufferBetweenCommas.append(it)
                }
                // split only if we are on the highest level of brackets (all brackets are closed)
                ',' -> if (numberOfClosedBrackets == numberOfOpenBrackets) {
                    result.add(bufferBetweenCommas.toString())
                    bufferBetweenCommas = StringBuilder()
                } else {
                    bufferBetweenCommas.append(it)
                }
                else -> bufferBetweenCommas.append(it)
            }
        }
        result.add(bufferBetweenCommas.toString())
        return result
    }

    /**
     * small validation for quotes: each quote should be closed in a key
     */
    private fun validateBrackets() {
        if (rawContent.count { it == '\"' } % 2 != 0 || rawContent.count { it == '\'' } % 2 != 0) {
            throw TomlParsingException(
                "Not able to parse the key: [$rawContent] as it does not have closing bracket",
                lineNo
            )
        }
    }
}
