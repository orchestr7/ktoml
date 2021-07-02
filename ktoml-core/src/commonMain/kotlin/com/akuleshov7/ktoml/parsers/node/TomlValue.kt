package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.trimBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes


sealed class TomlValue(val lineNo: Int) {
    abstract var content: Any
}

class TomlString(content: String, lineNo: Int) : TomlValue(lineNo) {
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
                    "Found invalid quote that is not escaped, index = [$index]." +
                            " Please remove the quote or use escape symbols in <$content>", lineNo
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
                    '\\' -> '\\'
                    't' -> '\t'
                    'b' -> '\b'
                    'r' -> '\r'
                    'n' -> '\n'
                    else -> throw TomlParsingException(
                        "According to TOML documentation unknown" +
                                " escape symbols are not allowed. Please check [\\${stringWithoutQuotes[i + 1]}]",
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

class TomlInt(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toInt()
}

class TomlFloat(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toFloat()
}

class TomlBoolean(content: String, lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = content.toBoolean()
}

class TomlNull(lineNo: Int) : TomlValue(lineNo) {
    override var content: Any = "null"
}

class TomlArray(private val rawContent: String, lineNo: Int) : TomlValue(lineNo) {
    override lateinit var content: Any

    init {
        validateBrackets()
        this.content = parse()
    }

    /**
     * small adaptor to make proper testing of parsing
     */
    fun parse(): List<Any> = rawContent.parse()

    /**
     * recursively parse array
     */
    private fun String.parse(): List<Any> =
        this.parseArray()
            .map { it.trim() }
            .map { if (it.startsWith("[")) it.parse() else it.parseValue(lineNo) }


    /**
     * method for splitting the string to the array: "[[a, b], [c], [d]]" to -> [a,b] [c] [d]
     */
    private fun String.parseArray(): MutableList<String> {
        var numberOfOpenBrackets = 0
        var numberOfClosedBrackets = 0
        var bufferBetweenCommas = StringBuilder()
        val result = mutableListOf<String>()

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
