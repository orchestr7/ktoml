package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.tree.*
import com.akuleshov7.ktoml.tree.nodes.*
import kotlin.jvm.JvmInline

/**
 * @property config - object that stores configuration options for a parser
 */
@JvmInline
@Suppress("WRONG_MULTIPLE_MODIFIERS_ORDER")
public value class TomlParser(private val config: TomlInputConfig) {
    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(config: TomlConfig) : this(config.input)

    /**
     * Method for parsing of TOML string (this string should be split with newlines \n or \r\n)
     *
     * @param toml a raw string in the toml format with '\n' separator
     * @return the root TomlFile node of the Tree that we have built after parsing
     */
    public fun parseString(toml: String): TomlFile {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        val tomlString = toml.replace("\r\n", "\n")
        return parseStringsToTomlTree(tomlString.split("\n"), config)
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
    public fun parseStringsToTomlTree(tomlLines: List<String>, config: TomlInputConfig): TomlFile {
        var currentParentalNode: TomlNode = TomlFile(config)
        // link to the head of the tree
        val tomlFileHead = currentParentalNode as TomlFile
        // need to trim empty lines BEFORE the start of processing
        val mutableTomlLines = tomlLines.toMutableList().trimEmptyLines()
        // here we always store the bucket of the latest created array of tables
        var latestCreatedBucket: TomlArrayOfTablesElement? = null

        val comments: MutableList<String> = mutableListOf()

        mutableTomlLines.forEachIndexed { index, line ->
            val lineNo = index + 1
            // comments and empty lines can easily be ignored in the TomlTree, but we cannot filter them out in mutableTomlLines
            // because we need to calculate and save lineNo
            if (line.isComment()) {
                comments += line.trimComment()
            } else if (!line.isEmptyLine()) {
                // Parse the inline comment if any
                val inlineComment = line.trimComment()

                if (line.isTableNode()) {
                    if (line.isArrayOfTables()) {
                        // TomlArrayOfTables contains all information about the ArrayOfTables ([[array of tables]])
                        val tableArray = TomlArrayOfTables(line, lineNo, config)
                        val arrayOfTables = tomlFileHead.insertTableToTree(tableArray, latestCreatedBucket)
                        // creating a new empty element that will be used as an element in array and the parent for next key-value records
                        val newArrayElement = TomlArrayOfTablesElement(lineNo, comments, inlineComment, config)
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
                        val tableSection = TomlTablePrimitive(line, lineNo, comments, inlineComment, config)
                        // if the table is the last line in toml, then it has no children, and we need to
                        // add at least fake node as a child
                        if (index == mutableTomlLines.lastIndex) {
                            tableSection.appendChild(TomlStubEmptyNode(lineNo, config))
                        }
                        // covering the case when the processed table does not contain nor key-value pairs neither tables (after our insertion)
                        // adding fake nodes to a previous table (it has no children because we have found another table right after)
                        currentParentalNode.insertStub()
                        currentParentalNode = tomlFileHead.insertTableToTree(tableSection)
                    }
                } else {
                    val keyValue = line.parseTomlKeyValue(lineNo, comments, inlineComment, config)
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

                comments.clear()
            }
        }
        return tomlFileHead
    }

    private fun TomlNode.insertStub() {
        if (this.hasNoChildren() && this !is TomlFile && this !is TomlArrayOfTablesElement) {
            this.appendChild(TomlStubEmptyNode(this.lineNo, config))
        }
    }

    private fun MutableList<String>.trimEmptyLines(): MutableList<String> {
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
