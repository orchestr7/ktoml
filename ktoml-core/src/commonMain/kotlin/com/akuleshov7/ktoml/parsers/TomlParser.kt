package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.parsers.node.TomlFile
import com.akuleshov7.ktoml.parsers.node.TomlKeyValue
import com.akuleshov7.ktoml.parsers.node.TomlKeyValueArray
import com.akuleshov7.ktoml.parsers.node.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.parsers.node.TomlNode
import com.akuleshov7.ktoml.parsers.node.TomlStubEmptyNode
import com.akuleshov7.ktoml.parsers.node.TomlTable
import com.akuleshov7.ktoml.parsers.node.splitKeyValue
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
    public fun parseStringsToTomlTree(tomlLines: List<String>, config: TomlConfig): TomlFile {
        var currentParent: TomlNode = TomlFile(config)
        val tomlFileHead = currentParent as TomlFile
        // need to trim empty lines BEFORE the start of processing
        val mutableTomlLines = tomlLines.toMutableList().trimEmptyLines()

        mutableTomlLines.forEachIndexed { index, line ->
            val lineNo = index + 1
            if (!line.isComment() && !line.isEmptyLine()) {
                if (line.isTableNode()) {
                    val tableSection = TomlTable(line, lineNo, config)
                    // if the table is the last line in toml, than it has no children and we need to
                    // add at least fake node as a child
                    if (index == mutableTomlLines.lastIndex) {
                        tableSection.appendChild(TomlStubEmptyNode(lineNo, config))
                    }
                    // covering the case when processed table contains no key-value pairs or no tables (after our insertion)
                    // adding fake nodes to a previous table (it has no children because we have found another table right after)
                    if (currentParent.hasNoChildren()) {
                        currentParent.appendChild(TomlStubEmptyNode(currentParent.lineNo, config))
                    }
                    currentParent = tomlFileHead.insertTableToTree(tableSection)
                } else {
                    val keyValue = line.parseTomlKeyValue(lineNo, config)
                    if (keyValue !is TomlNode) {
                        throw InternalAstException("All Toml nodes should always inherit TomlNode class." +
                                " Check [${keyValue.key}] with $keyValue type")
                    }

                    if (keyValue.key.isDotted) {
                        // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                        // because table is the same as dotted key
                        val newTableSection = keyValue.createTomlTableFromDottedKey(currentParent, config)
                        tomlFileHead
                            .insertTableToTree(newTableSection)
                            .appendChild(keyValue)
                    } else {
                        // otherwise it should simply append the keyValue to the parent
                        currentParent.appendChild(keyValue)
                    }
                }
            }
        }
        return tomlFileHead
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

    /**
     * factory adaptor to split the logic of parsing simple values from the logic of parsing collections (like Arrays)
     */
    private fun String.parseTomlKeyValue(lineNo: Int, config: TomlConfig): TomlKeyValue {
        val keyValuePair = this.splitKeyValue(lineNo, config)
        return when {
            keyValuePair.second.startsWith("[") -> TomlKeyValueArray(keyValuePair, lineNo, config)
            else -> TomlKeyValuePrimitive(keyValuePair, lineNo, config)
        }
    }

    private fun String.isTableNode(): Boolean {
        val trimmed = this.trim()
        return trimmed.startsWith("[") && trimmed.endsWith("]")
    }

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()
}
