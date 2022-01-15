package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.tree.TomlFile
import com.akuleshov7.ktoml.tree.TomlKeyValue
import com.akuleshov7.ktoml.tree.TomlKeyValueArray
import com.akuleshov7.ktoml.tree.TomlKeyValuePrimitive
import com.akuleshov7.ktoml.tree.TomlNode
import com.akuleshov7.ktoml.tree.TomlStubEmptyNode
import com.akuleshov7.ktoml.tree.TomlTable
import com.akuleshov7.ktoml.tree.splitKeyValue

/**
 * @property ktomlConf - object that stores configuration options for a parser
 */
public inline class TomlParser(private val ktomlConf: KtomlConf) {
    /**
     * Method for parsing of TOML string (this string should be split with newlines \n or \r\n)
     *
     * @param toml a raw string in the toml format with '\n' separator
     * @return the root TomlFile node of the Tree that we have built after parsing
     */
    public fun parseString(toml: String): TomlFile {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        val tomlString = toml.replace("\r\n", "\n")
        return parseStringsToTomlTree(tomlString.split("\n"), ktomlConf)
    }

    /**
     * Parsing the list of strings to the TOML intermediate representation (TOML- abstract syntax tree).
     *
     * @param ktomlLines list with toml strings (line by line)
     * @param ktomlConf
     * @return the root node of the resulted toml tree
     * @throws InternalAstException - if toml node does not inherit TomlNode class
     */
    public fun parseStringsToTomlTree(ktomlLines: List<String>, ktomlConf: KtomlConf): TomlFile {
        var currentParent: TomlNode = TomlFile(ktomlConf)
        val tomlFileHead = currentParent as TomlFile
        // need to trim empty lines BEFORE the start of processing
        val mutableKtomlLines = ktomlLines.toMutableList().trimEmptyLines()

        mutableKtomlLines.forEachIndexed { index, line ->
            val lineNo = index + 1
            if (!line.isComment() && !line.isEmptyLine()) {
                if (line.isTableNode()) {
                    val tableSection = TomlTable(line, lineNo, ktomlConf)
                    // if the table is the last line in toml, than it has no children and we need to
                    // add at least fake node as a child
                    if (index == mutableKtomlLines.lastIndex) {
                        tableSection.appendChild(TomlStubEmptyNode(lineNo, ktomlConf))
                    }
                    // covering the case when processed table contains no key-value pairs or no tables (after our insertion)
                    // adding fake nodes to a previous table (it has no children because we have found another table right after)
                    if (currentParent.hasNoChildren()) {
                        currentParent.appendChild(TomlStubEmptyNode(currentParent.lineNo, ktomlConf))
                    }
                    currentParent = tomlFileHead.insertTableToTree(tableSection)
                } else {
                    val keyValue = line.parseTomlKeyValue(lineNo, ktomlConf)
                    if (keyValue !is TomlNode) {
                        throw InternalAstException("All Toml nodes should always inherit TomlNode class." +
                                " Check [${keyValue.key}] with $keyValue type")
                    }

                    if (keyValue.key.isDotted) {
                        // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                        // because table is the same as dotted key
                        val newTableSection = keyValue.createTomlTableFromDottedKey(currentParent, ktomlConf)
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
    private fun String.parseTomlKeyValue(lineNo: Int, ktomlConf: KtomlConf): TomlKeyValue {
        val keyValuePair = this.splitKeyValue(lineNo, ktomlConf)
        return when {
            keyValuePair.second.startsWith("[") -> TomlKeyValueArray(keyValuePair, lineNo, ktomlConf)
            else -> TomlKeyValuePrimitive(keyValuePair, lineNo, ktomlConf)
        }
    }

    private fun String.isTableNode(): Boolean {
        val trimmed = this.trim()
        return trimmed.startsWith("[") && trimmed.endsWith("]")
    }

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()
}
