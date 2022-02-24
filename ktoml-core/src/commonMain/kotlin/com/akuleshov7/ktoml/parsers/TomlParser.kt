package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.tree.*
import kotlin.jvm.JvmInline

/**
 * @property config - object that stores configuration options for a parser
 */
@JvmInline
@Suppress("WRONG_MULTIPLE_MODIFIERS_ORDER")
public value class TomlParser(private val config: TomlConfig) {

    /**
     * Method for parsing of TOML string (this string should be split with newlines \n or \r\n)
     *
     * @param toml a raw string in the toml format with '\n' separator
     * @return the root TomlFile node of the Tree that we have built after parsing
     */
    public fun parseString(toml: String): TomlFile {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        return parseLines(toml.replace("\r\n", "\n").splitToSequence("\n"))
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
    public fun parseStringsToTomlTree(tomlLines: List<String>, config: TomlConfig): TomlFile {
        return parseStringsToTomlTree(tomlLines.asSequence(), config)
    }

    /**
     * Parsing the list of strings to the TOML intermediate representation (TOML- abstract syntax tree).
     *
     * @param tomlLines list with toml strings (line by line)
     * @param config
     * @return the root node of the resulted toml tree
     * @throws InternalAstException - if toml node does not inherit TomlNode class
     */
    @Suppress("TOO_LONG_FUNCTION")
    public fun parseStringsToTomlTree(tomlLines: Sequence<String>, config: TomlConfig): TomlFile {
        var currentParentalNode: TomlNode = TomlFile(config)
        // link to the head of the tree
        val tomlFileHead = currentParentalNode as TomlFile
        // need to trim empty lines BEFORE the start of processing
        val trimmedTomlLines = tomlLines.trimEmptyLines()
        // here we always store the bucket of the latest created array of tables
        var latestCreatedBucket: TomlArrayOfTablesElement? = null

        var index = 0
        val linesIterator = trimmedTomlLines.iterator()
        while (linesIterator.hasNext()) {
            val line = linesIterator.next()
            val lineNo = index + 1
            // comments and empty lines can easily be ignored in the TomlTree, but we cannot filter them out in mutableTomlLines
            // because we need to calculate and save lineNo
            if (!line.isComment() && !line.isEmptyLine()) {
                if (line.isTableNode()) {
                    if (line.isArrayOfTables()) {
                        // TomlArrayOfTables contains all information about the ArrayOfTables ([[array of tables]])
                        val tableArray = TomlArrayOfTables(line, lineNo, config)
                        val arrayOfTables = tomlFileHead.insertTableToTree(tableArray, latestCreatedBucket)
                        // creating a new empty element that will be used as an element in array and the parent for next key-value records
                        val newArrayElement = TomlArrayOfTablesElement(lineNo, config)
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
                        val tableSection = TomlTablePrimitive(line, lineNo, config)
                        // if the table is the last line in toml, then it has no children, and we need to
                        // add at least fake node as a child
                        if (!linesIterator.hasNext()) {
                            tableSection.appendChild(TomlStubEmptyNode(lineNo, config))
                        }
                        // covering the case when the processed table does not contain nor key-value pairs neither tables (after our insertion)
                        // adding fake nodes to a previous table (it has no children because we have found another table right after)
                        currentParentalNode.insertStub()
                        currentParentalNode = tomlFileHead.insertTableToTree(tableSection)
                    }
                } else {
                    val keyValue = line.parseTomlKeyValue(lineNo, config)
                    // inserting the key-value record to the tree
                    when {
                        keyValue is TomlKeyValue && keyValue.key.isDotted ->
                            // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                            // because table is the same as dotted key
                            tomlFileHead
                                .insertTableToTree(keyValue.createTomlTableFromDottedKey(currentParentalNode, config))
                                .appendChild(keyValue)

                        keyValue is TomlInlineTable ->
                            // in case of inline tables (a = { b = "c" }) we need to create a new parental table and
                            // recursively process all inner nested tables (including inline and dotted)
                            tomlFileHead.insertTableToTree(keyValue.returnTable(tomlFileHead, currentParentalNode))

                        // otherwise, it should simply append the keyValue to the parent
                        else -> currentParentalNode.appendChild(keyValue)
                    }
                }
            }
            index++
        }

        return tomlFileHead
    }

    private fun TomlNode.insertStub() {
        if (this.hasNoChildren() && this !is TomlFile && this !is TomlArrayOfTablesElement) {
            this.appendChild(TomlStubEmptyNode(this.lineNo, config))
        }
    }

    // This code is eavily inspired by the TransformingSequence code in kotlin-lib
    private fun Sequence<String>.trimEmptyLines(): Sequence<String> {
        return object : Sequence<String> {

            override fun iterator(): Iterator<String> {
                return object : Iterator<String> {
                    private val linesIterator = this@trimEmptyLines.iterator()

                    // -1 for unknown, 0 for done, 1 for empty lines, 2 for continue
                    private var nextState: Int = -1
                    private var nextItem: String? = null
                    private var nextRealItem: String? = null
                    private val emptyLinesBuffer = ArrayDeque<String>()

                    private fun calcNext() {
                        var nextEmptyLine = emptyLinesBuffer.removeFirstOrNull()
                        if (nextEmptyLine != null) {
                            nextState = 1
                            nextItem = nextEmptyLine
                            return
                        }
                        if (nextRealItem != null) {
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
                                if (nextEmptyLine == null) {
                                    nextState = 2
                                    nextItem = nextRealItem
                                    nextRealItem = null
                                } else {
                                    nextState = 1
                                    nextItem = nextEmptyLine
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
                            throw NoSuchElementException()
                        }
                        val result = nextItem
                        nextItem = null
                        nextState = -1
                        return result as String
                    }
                }
            }
        }
    }

    private fun String.isArrayOfTables(): Boolean = this.trim().startsWith("[[")

    private fun String.isTableNode(): Boolean {
        val trimmed = this.trim()
        return trimmed.startsWith("[")
    }

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()
}

/**
 * factory adaptor to split the logic of parsing simple values from the logic of parsing collections (like Arrays)
 *
 * @param lineNo
 * @param config
 * @return parsed toml node
 */
public fun String.parseTomlKeyValue(lineNo: Int, config: TomlConfig): TomlNode {
    val keyValuePair = this.splitKeyValue(lineNo, config)
    return when {
        keyValuePair.second.startsWith("[") -> TomlKeyValueArray(keyValuePair, lineNo, config)
        keyValuePair.second.startsWith("{") -> TomlInlineTable(keyValuePair, lineNo, config)
        else -> TomlKeyValuePrimitive(keyValuePair, lineNo, config)
    }
}
