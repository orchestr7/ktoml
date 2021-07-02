package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.parsers.node.*
import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * @param toml - this argument can be a path to a toml file or a string in the toml format,
 * depending on how you plan to work with it.
 */
public class TomlParser(val toml: String, val parserConf: ParserConf = ParserConf()) {
    @OptIn(ExperimentalFileSystem::class)
    fun readAndParseFile(): TomlFile {
        try {
            val ktomlPath = toml.toPath()
            val ktomlLinesFromFile = FileSystem.SYSTEM.read(ktomlPath) {
                // FixMe: may be we need to read and at the same time parse (to make it parallel)
                generateSequence { readUtf8Line() }.toList()
            }
            return parseStringsToTomlNode(ktomlLinesFromFile)
        } catch (e: FileNotFoundException) {
            println("Not able to find toml-file in the following path: $toml")
            throw e
        }
    }

    fun parseString(): TomlFile {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        val tomlString = toml.replace("\\r\\n", "\n")
        return parseStringsToTomlNode(tomlString.split("\n"))
    }

    private fun parseStringsToTomlNode(ktomlLines: List<String>): TomlFile {
        // FixMe: should be done in parallel
        var currentParent: TomlNode = TomlFile()
        val tomlFileHead = currentParent as TomlFile
        val mutableKtomlLines = ktomlLines.toMutableList()

        // removing all empty lines at the end, to cover empty tables properly
        while (mutableKtomlLines.last().isEmptyLine()) {
            mutableKtomlLines.removeLast()
        }

        mutableKtomlLines.forEachIndexed { index, line ->
            val lineNo = index + 1
            if (!line.isComment() && !line.isEmptyLine()) {
                if (line.isTableNode()) {
                    val tableSection = TomlTable(line, lineNo)
                    // if the table is the last line in toml, than it has no children and we need to
                    // add at least fake node as a child
                    if (index == mutableKtomlLines.size - 1) {
                        tableSection.appendChild(TomlStubEmptyNode(lineNo))
                    }

                    val newParent = tomlFileHead.insertTableToTree(tableSection)
                    // covering the case when table contains no key-value pairs or no tables (after our insertion)
                    // adding fake nodes to a previous table (it has no children because we have found another table right after)
                    if (currentParent.hasNoChildren()) {
                        currentParent.appendChild(TomlStubEmptyNode(currentParent.lineNo))
                    }
                    currentParent = newParent
                } else {
                    val keyValue = line.parseTomlKeyValue(lineNo, parserConf)
                    if (keyValue !is TomlNode)

                        throw InternalAstException("All Toml nodes should always inherit TomlNode class")

                    if (keyValue.key.isDotted) {
                        // in case parser has faced dot-separated complex key (a.b.c) it should create proper table [a.b],
                        // because table is the same as dotted key
                        val newTableSection = keyValue.createTomlTableFromDottedKey(currentParent)
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

    /**
     * factory adaptor to split the logic of parsing simple values from the logic of parsing collections (like Arrays)
     */
    private fun String.parseTomlKeyValue(lineNo: Int, parserConf: ParserConf): TomlKeyValue {
        val keyValuePair = this.splitKeyValue(lineNo, parserConf)
        return when {
            keyValuePair.second.startsWith("[") -> TomlKeyValueList(keyValuePair, lineNo)
            else -> TomlKeyValueSimple(keyValuePair, lineNo)
        }
    }

    private fun String.isTableNode() = "\\[(.*?)]".toRegex().matches(this.trim())

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()
}
