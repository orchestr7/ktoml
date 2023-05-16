package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.InternalDecodingException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.utils.newLineChar
import kotlin.jvm.JvmInline

/**
 * @property config - object that stores configuration options for a parser
 */
@JvmInline
@Suppress("WRONG_MULTIPLE_MODIFIERS_ORDER")
public value class TomlParser(private val config: TomlInputConfig) {
    /**
     * Method for parsing of TOML string (this string should be split with newlines \n or \r\n)
     *
     * @param toml a raw string in the toml format with '\n' separator
     * @return the root TomlFile node of the Tree that we have built after parsing
     */
    public fun parseString(toml: String): TomlFile {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        return parseLines(toml.replace("\r\n", newLineChar().toString()).splitToSequence(newLineChar().toString()))
    }

    /**
     * Method for parsing of TOML lines
     *
     * @param tomlLines toml lines
     * @return the root TomlFile node of the Tree that we have built after parsing
     */
    public fun parseLines(tomlLines: Sequence<String>): TomlFile {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        return parseStringsToTomlTree(tomlLines, config)
    }

    /**
     * Parsing the list of strings to the TOML intermediate representation (TOML- abstract syntax tree).
     *
     * @param tomlLines list with toml strings (line by line)
     * @param config
     * @return the root node of the resulted toml tree
     * @throws InternalAstException - if toml node does not inherit TomlNode class
     */
    @Suppress("TOO_LONG_FUNCTION", "NESTED_BLOCK")
    public fun parseStringsToTomlTree(tomlLines: Sequence<String>, config: TomlInputConfig): TomlFile {
        var currentParentalNode: TomlNode = TomlFile()
        // link to the head of the tree
        val tomlFileHead = currentParentalNode as TomlFile
        // need to trim empty lines BEFORE the start of processing
        val trimmedTomlLines = tomlLines.trimEmptyLines()
        // here we always store the bucket of the latest created array of tables
        var latestCreatedBucket: TomlArrayOfTablesElement? = null

        val comments: MutableList<String> = mutableListOf()
        var index = 0
        val linesIterator = trimmedTomlLines.iterator()
        // all lines will be streamed sequentially
        while (linesIterator.hasNext()) {
            val line = linesIterator.next()
            val lineNo = index + 1

            // comments and empty lines can easily be ignored in the TomlTree, but we cannot filter them out in mutableTomlLines
            // because we need to calculate and save lineNo
            if (line.isComment()) {
                comments += line.trimComment(config.allowEscapedQuotesInLiteralStrings)
            } else if (!line.isEmptyLine()) {
                // Parse the inline comment if any
                val inlineComment = line.trimComment(config.allowEscapedQuotesInLiteralStrings)

                val multilineType = line.getMultilineType()
                val tomlLine = if (multilineType != MultilineType.NOT_A_MULTILINE) {
                    // first line from multiline is already taken from the sequence and can be processed
                    val collectedMultiline = StringBuilder()
                    collectLineWithComments(collectedMultiline, comments, multilineType, line)
                    collectedMultiline.append(newLineChar())

                    // processing remaining lines from a multiline
                    val indexAtTheEndOfMultiline = collectMultiline(
                        linesIterator,
                        collectedMultiline,
                        index,
                        multilineType,
                        comments
                    )
                    index = indexAtTheEndOfMultiline
                    collectedMultiline.toString()
                } else {
                    line
                }

                if (tomlLine.isTableNode()) {
                    if (tomlLine.isArrayOfTables()) {
                        // TomlArrayOfTables contains all information about the ArrayOfTables ([[array of tables]])
                        val tableArray = TomlArrayOfTables(tomlLine, lineNo)
                        val arrayOfTables = tomlFileHead.insertTableToTree(tableArray, latestCreatedBucket)
                        // creating a new empty element that will be used as an element in array and the parent for next key-value records
                        val newArrayElement = TomlArrayOfTablesElement(lineNo, comments, inlineComment)
                        // adding this element as a child to the array of tables
                        arrayOfTables.appendChild(newArrayElement)
                        // covering the case when the processed table does not contain nor key-value pairs neither tables (after our insertion)
                        // adding fake nodes to a previous table (it has no children because we have found another table right after)
                        currentParentalNode.insertStub()
                        // and setting this element as a current parent, so new key-records will be added to this bucket
                        currentParentalNode = newArrayElement
                        // here we set the bucket that will be incredibly useful when we will be inserting the next array of tables
                        latestCreatedBucket = newArrayElement
                    } else {
                        val tableSection = TomlTablePrimitive(tomlLine, lineNo, comments, inlineComment)
                        // if the table is the last line in toml, then it has no children, and we need to
                        // add at least fake node as a child
                        if (!linesIterator.hasNext()) {
                            tableSection.appendChild(TomlStubEmptyNode(lineNo))
                        }
                        // covering the case when the processed table does not contain nor key-value pairs neither tables (after our insertion)
                        // adding fake nodes to a previous table (it has no children because we have found another table right after)
                        currentParentalNode.insertStub()
                        currentParentalNode = tomlFileHead.insertTableToTree(tableSection)
                    }
                } else {
                    val keyValue = tomlLine.parseTomlKeyValue(lineNo, comments, inlineComment, config)
                    // inserting the key-value record to the tree
                    when {
                        keyValue is TomlKeyValue && keyValue.key.isDotted ->
                            // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                            // because table is the same as dotted key
                            tomlFileHead
                                .insertTableToTree(keyValue.createTomlTableFromDottedKey(currentParentalNode))
                                .appendChild(keyValue)

                        keyValue is TomlInlineTable ->
                            // in case of inline tables (a = { b = "c" }) we need to create a new parental table and
                            // recursively process all inner nested tables (including inline and dotted)
                            tomlFileHead.insertTableToTree(keyValue.returnTable(tomlFileHead, currentParentalNode))

                        // otherwise, it should simply append the keyValue to the parent
                        else -> currentParentalNode.appendChild(keyValue)
                    }
                }
                comments.clear()
            }
            index++
        }
        return tomlFileHead
    }

    @Suppress("TOO_LONG_FUNCTION")
    // This code is heavily inspired by the TransformingSequence code in kotlin-lib, simple trimming of empty lines
    private fun Sequence<String>.trimEmptyLines(): Sequence<String> = object : Sequence<String> {
        override fun iterator(): Iterator<String> = object : Iterator<String> {
            private val linesIterator = this@trimEmptyLines.iterator()

            // -1 for unknown, 0 for done, 1 for empty lines, 2 for continue
            private var nextState: Int = -1
            private var nextItem: String? = null
            private var nextRealItem: String? = null
            private val emptyLinesBuffer: ArrayDeque<String> = ArrayDeque()

            private fun calcNext() {
                var nextEmptyLine = emptyLinesBuffer.removeFirstOrNull()
                nextEmptyLine?.let {
                    nextState = 1
                    nextItem = nextEmptyLine
                    return
                }
                nextRealItem?.let {
                    nextState = 2
                    nextItem = nextRealItem
                    nextRealItem = null
                    return
                }
                while (linesIterator.hasNext()) {
                    val line = linesIterator.next()
                    if (line.isEmptyLine()) {
                        emptyLinesBuffer.add(line)
                    } else {
                        nextRealItem = line
                        nextEmptyLine = emptyLinesBuffer.removeFirstOrNull()
                        nextEmptyLine?.let {
                            nextState = 1
                            nextItem = nextEmptyLine
                        }
                            ?: run {
                                nextState = 2
                                nextItem = nextRealItem
                                nextRealItem = null
                            }
                        return
                    }
                }
                nextState = 0
            }

            override fun hasNext(): Boolean {
                if (nextState == -1) {
                    calcNext()
                }
                return nextState == 1 || nextState == 2
            }

            override fun next(): String {
                if (nextState == -1) {
                    calcNext()
                }
                if (nextState == 0) {
                    throw InternalDecodingException("Tried to trim empty lines in the input, " +
                            "but was not able to iterate through an input, because next object is unexpectedly missing.")
                }
                val result = nextItem
                nextItem = null
                nextState = -1
                return result as String
            }
        }
    }

    /**
     * @param collectedMultiline append all multi-lines to this argument
     * @return index at the end of multiline
     */
    private fun collectMultiline(
        linesIterator: Iterator<String>,
        collectedMultiline: StringBuilder,
        startIndex: Int,
        multilineType: MultilineType,
        comments: MutableList<String>
    ): Int {
        var index = startIndex
        var line: String
        var hasFoundEnd = false

        // all lines will be streamed sequentially
        while (linesIterator.hasNext()) {
            line = linesIterator.next()
            collectLineWithComments(collectedMultiline, comments, multilineType, line)

            if (line.isEndOfMultilineValue(multilineType, index + 1)) {
                hasFoundEnd = true
                break
            }
            // append new line to collect string as is
            collectedMultiline.append(newLineChar())
            index++
        }

        if (!hasFoundEnd) {
            throw ParseException(
                "Expected (${multilineType.closingSymbols}) in the end of ${multilineType.name}",
                startIndex + 1
            )
        }
        return index
    }

    private fun collectLineWithComments(
        collectTo: StringBuilder,
        comments: MutableList<String>,
        multilineType: MultilineType,
        line: String
    ) {
        if (multilineType == MultilineType.ARRAY) {
            collectTo.append(line.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings))
            comments += line.trimComment(config.allowEscapedQuotesInLiteralStrings)
        } else {
            // we can't have comments inside a multi-line basic/literal string
            collectTo.append(line)
        }
    }

    /**
     * Important! We treat a multi-line that is declared in one line ("""abc""") as a regular not multiline string
     */
    private fun String.getMultilineType(): MultilineType {
        val line = this.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
        val firstEqualsSign = line.indexOfFirst { it == '=' }
        if (firstEqualsSign == -1) {
            return MultilineType.NOT_A_MULTILINE
        }
        val value = line.substring(firstEqualsSign + 1).trim()

        if (value.startsWith("[") && !value.endsWith("]")) {
            return MultilineType.ARRAY
        }

        // If we have more than 1 combination of (""") - it means that
        // multi-line is declared in one line, and we can handle it as not a multi-line
        if (value.startsWith("\"\"\"") && value.getCountOfOccurrencesOfSubstring("\"\"\"") == 1) {
            return MultilineType.BASIC_STRING
        }
        if (value.startsWith("'''") && value.getCountOfOccurrencesOfSubstring("\'\'\'") == 1) {
            return MultilineType.LITERAL_STRING
        }

        // Otherwise, the string isn't a multi-line declaration
        return MultilineType.NOT_A_MULTILINE
    }

    /**
     * @return true if string is a last line of multiline value declaration
     */
    private fun String.isEndOfMultilineValue(multilineType: MultilineType, lineNo: Int): Boolean {
        if (multilineType == MultilineType.NOT_A_MULTILINE) {
            throw ParseException("Internal parse exception", lineNo)
        }

        return this.takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
            .trim()
            .endsWith(multilineType.closingSymbols)
    }

    private fun TomlNode.insertStub() {
        if (this.hasNoChildren() && this !is TomlFile && this !is TomlArrayOfTablesElement) {
            this.appendChild(TomlStubEmptyNode(this.lineNo))
        }
    }

    private fun MutableList<String>.trimEmptyTrailingLines(): MutableList<String> {
        if (this.isEmpty()) {
            return this
        }
        // removing all empty lines at the end, to cover empty tables properly
        while (this.last().isEmptyLine()) {
            this.removeLast()
            if (this.isEmpty()) {
                return this
            }
        }
        return this
    }

    private fun String.isArrayOfTables(): Boolean = this.trim().startsWith("[[")

    private fun String.isTableNode(): Boolean {
        val trimmed = this.trim()
        return trimmed.startsWith("[")
    }

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()

    /**
     * @property closingSymbols - symbols indicating that the multi-line is closed
     */
    private enum class MultilineType(val closingSymbols: String) {
        ARRAY("]"),
        BASIC_STRING("\"\"\""),
        LITERAL_STRING("'''"),
        NOT_A_MULTILINE(""),
        ;
    }
}

/**
 * factory adaptor to split the logic of parsing simple values from the logic of parsing collections (like Arrays)
 *
 * @param lineNo
 * @param comments
 * @param inlineComment
 * @param config
 * @return parsed toml node
 */
public fun String.parseTomlKeyValue(
    lineNo: Int,
    comments: List<String>,
    inlineComment: String,
    config: TomlInputConfig
): TomlNode {
    val keyValuePair = this.splitKeyValue(lineNo, config)
    return when {
        keyValuePair.second.startsWith("[") -> TomlKeyValueArray(keyValuePair, lineNo, comments, inlineComment, config)
        keyValuePair.second.startsWith("{") -> TomlInlineTable(keyValuePair, lineNo, comments, inlineComment, config)
        else -> TomlKeyValuePrimitive(keyValuePair, lineNo, comments, inlineComment, config)
    }
}
