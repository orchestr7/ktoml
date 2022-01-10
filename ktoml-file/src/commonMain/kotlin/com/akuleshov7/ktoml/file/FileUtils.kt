/**
 * File utils to read files using okio
 */

package com.akuleshov7.ktoml.file

import okio.BufferedSink
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer

/**
 * Simple file reading with okio (returning a list with strings)
 *
 * @param tomlFile string with a path to a file
 * @return list with strings
 * @throws FileNotFoundException if the toml file is missing
 */
internal fun readAndParseFile(tomlFile: String): List<String> {
    try {
        val ktomlPath = tomlFile.toPath()
        return getOsSpecificFileSystem().read(ktomlPath) {
            // FixMe: may be we need to read and at the same time parse (to make it parallel)
            generateSequence { readUtf8Line() }.toList()
        }
    } catch (e: FileNotFoundException) {
        println("Not able to find toml-file in the following path: $tomlFile")
        throw e
    }
}

/**
 * Opens a file for writing via a [BufferedSink].
 *
 * @param tomlFile The path string pointing to a .toml file.
 * @return A [BufferedSink] writing to the specified [tomlFile] path.
 * @throws FileNotFoundException
 */
internal fun openFileForWrite(tomlFile: String): BufferedSink {
    try {
        val ktomlPath = tomlFile.toPath()
        return getOsSpecificFileSystem().sink(ktomlPath).buffer()
    } catch (e: FileNotFoundException) {
        println("Not able to fine toml-file in the following path: $tomlFile")
        throw e
    }
}

/**
 * Implementation for getting proper file system to read files with okio
 *
 * @return proper FileSystem
 */
internal expect fun getOsSpecificFileSystem(): FileSystem
