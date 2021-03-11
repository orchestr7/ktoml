package com.akuleshov7.parsers

import com.akuleshov7.error
import com.akuleshov7.parsers.node.TomlFile
import com.akuleshov7.parsers.node.TomlNode
import com.akuleshov7.parsers.node.TomlTable
import com.akuleshov7.parsers.node.TomlKeyValue
import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.system.exitProcess


internal class TomlParser(tomlString: String = "") {
    lateinit var ktomlLines: List<String>

    @ExperimentalFileSystem
    fun readFile(ktomlFilePath: String): TomlNode {
        try {
            val ktomlPath = ktomlFilePath.toPath()
            ktomlLines = FileSystem.SYSTEM.read(ktomlPath) {
                // FixMe: may be we need to read and at the same time parse (to make it parallel)
                generateSequence { readUtf8Line() }.toList()
            }
            return parse()
        } catch (e: FileNotFoundException) {
            "Not able to find file in the following path: $ktomlFilePath".error()
            exitProcess(1)
        }
    }

    private fun parse(): TomlNode {
        val allSections = mutableSetOf<TomlTable>()

        // FixMe: should be done in parallel
        var currentParent: TomlNode = TomlFile()
        val head = currentParent

        ktomlLines.forEachIndexed { index, line ->
            val lineno = index + 1
            if (!line.isComment() && !line.isEmptyLine()) {
                if (line.isTableNode()) {
                    val tableSection = TomlTable(line, lineno)
                    allSections.add(tableSection)
                    currentParent.appendChild(tableSection)
                    currentParent = tableSection
                } else {
                    currentParent.appendChild(TomlKeyValue(line, lineno))
                }
            }
        }
        return head
    }

    private fun String.isTableNode() = "\\[(.*?)]".toRegex().matches(this.trim())

    private fun String.isComment() = this.trim().startsWith("#")

    private fun String.isEmptyLine() = this.trim().isEmpty()
}
