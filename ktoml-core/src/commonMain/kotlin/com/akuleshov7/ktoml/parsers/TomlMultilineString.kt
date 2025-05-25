package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.enums.MultilineType
import com.akuleshov7.ktoml.utils.LinesIteratorWrapper
import com.akuleshov7.ktoml.utils.newLineChar

/**
 * @param config
 * @param linesIteratorWrapper - iterator with the rest of the toml data
 * @param firstLine - first line of multiline value where it was detected
 */
internal class TomlMultilineString(
    private val config: TomlInputConfig,
    private val linesIteratorWrapper: LinesIteratorWrapper<String>,
    firstLine: String,
) {
    private val comments: MutableList<String> = mutableListOf()
    private val lines: MutableList<String> = mutableListOf()
    private val startLineNo = linesIteratorWrapper.lineNo
    private val multilineType = getMultilineType(firstLine, config)
    private var isInMultilineBasic = false
    private var isInMultilineLiteral = false

    // If isNested is null, we don't know yet if the type is nested
    private var isNested = if (multilineType.isNestedSupported) null else false

    init {
        if (multilineType == MultilineType.NOT_A_MULTILINE) {
            throw ParseException("Internal parse exception", startLineNo)
        }
        trackMultilineString(firstLine)
        lines.add(firstLine.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings))
        parseMultiline()
    }

    fun getLine(): String = if (multilineType == MultilineType.ARRAY) {
        lines.joinToString(newLineChar().toString()) {
            it.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
        }
    } else {
        // we can't have comments inside multi-line basic/literal string
        lines.joinToString(newLineChar().toString())
    }

    fun getComments(): List<String> = comments

    private fun parseMultiline() {
        var hasFoundEnd = false

        while (linesIteratorWrapper.hasNext()) {
            val line = linesIteratorWrapper.next()
            trackMultilineString(line)

            if (!stringTypes.contains(multilineType)) {
                if (!isInMultilineString()) {
                    comments.add(line.trimComment(config.allowEscapedQuotesInLiteralStrings))
                    lines.add(line.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings))
                } else {
                    // We're inside multiline basic/literal string element, so there's no comments
                    lines.add(line)
                }
            } else {
                // We have multiline basic/literal string MultilineType; They don't have comments inside
                lines.add(line)
            }

            if (!isInMultilineString() && isEndOfMultilineValue(multilineType)) {
                hasFoundEnd = true
                break
            }
        }

        if (!hasFoundEnd) {
            throw ParseException(
                "Expected (${multilineType.closingSymbols}) in the end of ${multilineType.name}",
                startLineNo,
            )
        }
    }

    /**
     * When we have an array with multiline strings, and we're parsing line X
     * we want to know if multiline string was open before line X
     */
    private fun trackMultilineString(line: String) {
        if (stringTypes.contains(multilineType)) {
            return
        }
        for (i in 0..line.length - 3) {
            // Stumbled upon a comment, no need to analyze for the rest of the line
            if (!isInMultilineBasic && !isInMultilineLiteral && line[i] == '#') {
                break
            }

            if (!isInMultilineLiteral && isNextThreeQuotes(line, i, '"')) {
                isInMultilineBasic = !isInMultilineBasic
            } else if (!isInMultilineBasic && isNextThreeQuotes(line, i, '\'')) {
                isInMultilineLiteral = !isInMultilineLiteral
            }
        }
    }

    private fun isNextThreeQuotes(
        line: String,
        index: Int,
        quote: Char
    ): Boolean = line[index] == quote && line[index + 1] == quote && line[index + 2] == quote

    private fun isInMultilineString(): Boolean = isInMultilineBasic || isInMultilineLiteral

    /**
     * @return true if string is a last line of multiline value declaration
     */
    private fun isEndOfMultilineValue(multilineType: MultilineType): Boolean {
        isNested ?: run {
            isNested = hasTwoConsecutiveSymbolsIgnoreWhitespaces(getLine(), multilineType.openSymbols[0])
        }

        return if (isNested == true) {
            val clearedString = lines.joinToString("")
                .filter { !it.isWhitespace() }

            if (multilineType == MultilineType.ARRAY) {
                clearedString.endsWith(multilineType.closingSymbols + multilineType.closingSymbols) ||
                        clearedString.endsWith(multilineType.closingSymbols + "," + multilineType.closingSymbols)
            } else {
                clearedString.endsWith(multilineType.closingSymbols + multilineType.closingSymbols)
            }
        } else {
            lines.last()
                .trim()
                .endsWith(multilineType.closingSymbols)
        }
    }

    private fun hasTwoConsecutiveSymbolsIgnoreWhitespaces(value: String, searchSymbol: Char): Boolean? {
        val firstIndex = value.indexOf(searchSymbol)
        if (firstIndex == -1) {
            return false
        }

        val nextIndex = value.indexOf(searchSymbol, firstIndex + 1)

        if (nextIndex != -1) {
            val between = value.substring(firstIndex + 1, nextIndex)
            return between.all { it.isWhitespace() }
        }

        val isRestHasOnlyWhitespaces = !value.substring(firstIndex + 1).any { !it.isWhitespace() }
        return if (isRestHasOnlyWhitespaces) {
            null
        } else {
            false
        }
    }

    companion object {
        private val stringTypes = listOf(MultilineType.BASIC_STRING, MultilineType.LITERAL_STRING)

        /**
         * Important! We treat a multi-line that is declared in one line ("""abc""") as a regular not multiline string
         *
         * @param line
         * @param config
         * @return MultilineType
         */
        fun getMultilineType(line: String, config: TomlInputConfig): MultilineType {
            val line = line.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
            val firstEqualsSign = line.indexOfFirst { it == '=' }
            if (firstEqualsSign == -1) {
                return MultilineType.NOT_A_MULTILINE
            }
            val value = line.substring(firstEqualsSign + 1).trim()

            if (value.startsWith(MultilineType.ARRAY.openSymbols) &&
                    !value.endsWith(MultilineType.ARRAY.closingSymbols)
            ) {
                return MultilineType.ARRAY
            }

            // If we have more than 1 combination of (""") - it means that
            // multi-line is declared in one line, and we can handle it as not a multi-line
            if (value.startsWith(MultilineType.BASIC_STRING.openSymbols) && value.getCountOfOccurrencesOfSubstring(MultilineType.BASIC_STRING.openSymbols) == 1
            ) {
                return MultilineType.BASIC_STRING
            }
            if (value.startsWith(MultilineType.LITERAL_STRING.openSymbols) &&
                    value.getCountOfOccurrencesOfSubstring(MultilineType.LITERAL_STRING.openSymbols) == 1
            ) {
                return MultilineType.LITERAL_STRING
            }

            return MultilineType.NOT_A_MULTILINE
        }
    }
}
