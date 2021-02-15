package com.akuleshov7.parsers

import com.akuleshov7.error
import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.exit


internal class TomlParser(tomlString: String = "") {
    lateinit var ktomlLines: List<String>

    @ExperimentalFileSystem
    fun readFile(ktomlFilePath: String) {
        try {
            val ktomlPath = ktomlFilePath.toPath()
            ktomlLines = FileSystem.SYSTEM.read(ktomlPath) {
                // FixMe: may be we need to read and at the same time parse (to make it parallel)
                generateSequence { readUtf8Line() }.toList()
            }
            parse()
        } catch (e: FileNotFoundException) {
            "Not able to find file in the following path: $ktomlFilePath".error()
            exit(1)
        }
    }

    private fun parse() {
        val sectionSet = emptySet<String>()

        // should be in parallel
        ktomlLines.forEach { line ->
            println(line)
            // if (date) -> return Date
            // if (string) -> return String
            // if (boolean) -> toBoolean
            // if (array) -> array
        }
    }
}
