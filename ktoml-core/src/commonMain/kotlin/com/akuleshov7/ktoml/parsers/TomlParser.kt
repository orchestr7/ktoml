package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.exceptions.InternalDecodingException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.enums.MultilineType
import com.akuleshov7.ktoml.tree.nodes.*
import com.akuleshov7.ktoml.utils.LinesIteratorWrapper
import com.akuleshov7.ktoml.utils.checkNoTomlControlChars
import com.akuleshov7.ktoml.utils.newLineChar
import kotlin.jvm.JvmInline

private const val IDEOGRAPHIC_SPACE = '\u3000'
private const val TRIPLE_QUOTE_LENGTH = 3

/**
 * @param config - object that stores configuration options for a parser
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
        val trimmedTomlLines = tomlLines
            .checkNoTomlControlChars()
            .trimEmptyLines()
        // here we always store the bucket of the latest created array of tables
        var latestCreatedBucket: TomlArrayOfTablesElement? = null

        val comments: MutableList<String> = mutableListOf()
        val linesIterator = LinesIteratorWrapper(trimmedTomlLines.iterator())
        // in spec-compliant mode we reject table/key redefinitions; ktoml's lenient default merges them
        val validate = !config.allowTableRedefinition
        // all lines will be streamed sequentially
        while (linesIterator.hasNext()) {
            val line = linesIterator.next()
            val lineNo = linesIterator.lineNo
            line.validateTomlWhitespace(lineNo, config)

            // comments and empty lines can easily be ignored in the TomlTree, but we cannot filter them out in mutableTomlLines
            // because we need to calculate and save lineNo
            if (line.isComment()) {
                comments += line.trimComment(config.allowEscapedQuotesInLiteralStrings)
            } else if (!line.isEmptyLine()) {
                // Parse the inline comment if any
                val inlineComment = line.trimComment(config.allowEscapedQuotesInLiteralStrings)

                val multilineType = TomlMultilineString.getMultilineType(line, config)
                val tomlLine = if (multilineType != MultilineType.NOT_A_MULTILINE) {
                    val tomlMultilineString = TomlMultilineString(config, linesIterator, line)
                    comments += tomlMultilineString.getComments()
                    tomlMultilineString.getLine()
                } else {
                    line
                }

                if (tomlLine.isTableNode()) {
                    if (tomlLine.isArrayOfTables()) {
                        // TomlArrayOfTables contains all information about the ArrayOfTables ([[array of tables]])
                        val tableArray = TomlTable(tomlLine, lineNo, TableType.ARRAY)
                        val arrayOfTables = tomlFileHead.insertTableToTree(tableArray, latestCreatedBucket, validate = validate)
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
                        val tableSection = TomlTable(tomlLine, lineNo, TableType.PRIMITIVE, comments, inlineComment)
                        // if the table is the last line in toml, then it has no children, and we need to
                        // add at least fake node as a child
                        if (!linesIterator.hasNext()) {
                            tableSection.appendChild(TomlStubEmptyNode(lineNo))
                        }
                        // covering the case when the processed table does not contain nor key-value pairs neither tables (after our insertion)
                        // adding fake nodes to a previous table (it has no children because we have found another table right after)
                        currentParentalNode.insertStub()
                        currentParentalNode = tomlFileHead.insertTableToTree(tableSection, validate = validate)
                    }
                } else {
                    val keyValue = tomlLine.parseTomlKeyValue(lineNo, comments, inlineComment, config)
                    // inserting the key-value record to the tree
                    when {
                        keyValue is TomlKeyValue && keyValue.key.isDotted ->
                            // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                            // because table is the same as dotted key
                            tomlFileHead
                                .insertTableToTree(
                                    keyValue.createTomlTableFromDottedKey(currentParentalNode),
                                    insertionType = TableInsertionType.DOTTED_KEY,
                                    containerDepth = currentParentalNode.sectionDepth(),
                                    validate = validate
                                )
                                .appendCheckedKeyValue(keyValue, validate)

                        keyValue is TomlInlineTable -> {
                            // in case of inline tables (a = { b = "c" }) we need to create a new parental table and
                            // recursively process all inner nested tables (including inline and dotted)
                            keyValue.key?.last()?.let { keyName ->
                                currentParentalNode.checkKeyCanBeDefined(keyName, keyValue.lineNo, validate)
                            }
                            tomlFileHead.insertTableToTree(
                                keyValue.returnTable(tomlFileHead, currentParentalNode, validate),
                                insertionType = TableInsertionType.INLINE_TABLE,
                                containerDepth = currentParentalNode.sectionDepth(),
                                validate = validate
                            )
                        }

                        // otherwise, it should simply append the keyValue to the parent
                        else -> currentParentalNode.appendCheckedKeyValue(keyValue, validate)
                    }
                }
                comments.clear()
            }
        }
        return tomlFileHead
    }

    private fun Sequence<String>.checkNoTomlControlChars(): Sequence<String> = sequence {
        var lineNo = 1
        for (line in this@checkNoTomlControlChars) {
            line.checkNoTomlControlChars(lineNo)
            yield(line)
            lineNo++
        }
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

    @Suppress("NESTED_BLOCK")
    private fun String.validateTomlWhitespace(lineNo: Int, config: TomlInputConfig) {
        val chars = replaceEscaped(config.allowEscapedQuotesInLiteralStrings)
        var quote: String? = null
        var index = 0
        while (index < chars.length) {
            val symbol = chars[index]
            val currentQuote = quote
            if (currentQuote == null) {
                when {
                    symbol == '#' -> return
                    symbol == IDEOGRAPHIC_SPACE -> throw ParseException(
                        "Only ASCII space and tab are permitted as TOML whitespace; found U+3000.",
                        lineNo,
                    )
                    symbol == '"' || symbol == '\'' -> {
                        val openingQuote = chars.openingQuoteAt(index, symbol)
                        quote = openingQuote
                        index += openingQuote.length
                        continue
                    }
                    else -> Unit
                }
            } else if (chars.startsWith(currentQuote, index)) {
                quote = null
                index += currentQuote.length
                continue
            }
            index += 1
        }
    }

    private fun String.openingQuoteAt(index: Int, symbol: Char): String =
        if (index + 2 < length && this[index + 1] == symbol && this[index + 2] == symbol) {
            symbol.toString().repeat(TRIPLE_QUOTE_LENGTH)
        } else {
            symbol.toString()
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
    val value = keyValuePair.second
    // [{..}, {..}] — a pure array of inline tables is modelled as an array-of-tables.
    // A mixed array that only starts with an inline table ([{..}, "b", 1]) is a normal array.
    val isArrayOfInlineTables = value.startsWithIgnoreAllWhitespaces("[{") &&
            with(TomlInlineTable.Companion) { value.isArrayOfInlineTables() }
    return when {
        isArrayOfInlineTables ->
            TomlInlineTable(
                keyValuePair, lineNo, comments, inlineComment, config
            )
        keyValuePair.second.startsWith("{") -> TomlInlineTable(keyValuePair, lineNo, comments, inlineComment, config)
        keyValuePair.second.startsWith("[") -> TomlKeyValueArray(keyValuePair, lineNo, comments, inlineComment, config)
        else -> TomlKeyValuePrimitive(keyValuePair, lineNo, comments, inlineComment, config)
    }
}
