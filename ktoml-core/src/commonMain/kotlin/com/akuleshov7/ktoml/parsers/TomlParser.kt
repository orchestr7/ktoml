package com.akuleshov7.ktoml.parsers

import com.akuleshov7.ktoml.error
import com.akuleshov7.ktoml.parsers.node.TomlFile
import com.akuleshov7.ktoml.parsers.node.TomlNode
import com.akuleshov7.ktoml.parsers.node.TomlTable
import com.akuleshov7.ktoml.parsers.node.TomlKeyValue
import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * @param toml - this argument can be a path to a toml file or a string in the toml format,
 * depending on how you plan to work with it.
 */
internal class TomlParser(val toml: String) {
    @OptIn(ExperimentalFileSystem::class)
    fun readAndParseFile(): TomlNode {
        try {
            val ktomlPath = toml.toPath()
            val ktomlLinesFromFile = FileSystem.SYSTEM.read(ktomlPath) {
                // FixMe: may be we need to read and at the same time parse (to make it parallel)
                generateSequence { readUtf8Line() }.toList()
            }
            return parseStringsToTomlNode(ktomlLinesFromFile)
        } catch (e: FileNotFoundException) {
            "Not able to find toml-file in the following path: $toml".error()
            throw e
        }
    }

    fun parseString(): TomlNode {
        // It looks like we need this hack to process line separator properly, as we don't have System.lineSeparator()
        val string = toml.replace("\\r\\n", "\n")
        return parseStringsToTomlNode(string.split("\n"))
    }

    private fun parseStringsToTomlNode(ktomlLines: List<String>): TomlNode {
        // FixMe: should be done in parallel
        var currentParent: TomlNode = TomlFile()
        val tomlFileHead = currentParent as TomlFile

        ktomlLines.forEachIndexed { index, line ->
            val lineno = index + 1
            if (!line.isComment() && !line.isEmptyLine()) {
                if (line.isTableNode()) {
                    val tableSection = TomlTable(line, lineno)
                    tomlFileHead.insertTableToTree(tableSection)
                    currentParent = tableSection
                } else {
                    currentParent.appendChild(TomlKeyValue(line, lineno))
                }
            }
        }

        return tomlFileHead
    }

    private fun String.isTableNode() = "\\[(.*?)]".toRegex().matches(this.trim())

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()
}
