/**
 * File utils to read files using okio
 */

package com.akuleshov7.ktoml.file

import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Simple file reading with okio (returning a list with strings)
 *
 * @param tomlFile string with a path to a file
 * @return list with strings
 * @throws e FileNotFoundException if the toml file is missing
 */
@ExperimentalFileSystem
internal fun readAndParseFile(tomlFile: String): List<String> {
    try {
        val ktomlPath = tomlFile.toPath()
        return FileSystem.SYSTEM.read(ktomlPath) {
            // FixMe: may be we need to read and at the same time parse (to make it parallel)
            generateSequence { readUtf8Line() }.toList()
        }
    } catch (e: FileNotFoundException) {
        println("Not able to find toml-file in the following path: $tomlFile")
        throw e
    }
}
