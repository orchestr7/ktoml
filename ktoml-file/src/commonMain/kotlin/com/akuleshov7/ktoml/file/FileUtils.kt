/**
 * File utils to read files using okio
 */

package com.akuleshov7.ktoml.file

import okio.*
import okio.Path.Companion.toPath

/**
 * Simple file reading with okio (returning a list with strings)
 *
 * @param filePath string with a path to a file
 * @return list with strings
 * @throws FileNotFoundException if the toml file is missing
 */
internal fun getFileSource(filePath: String): Source {
    try {
        val extension = filePath.substringAfterLast('.', "")
        check(extension == "toml") {
            "TOML file should end with a .toml extension"
        }
        return getOsSpecificFileSystem().source(filePath.toPath())
    } catch (e: FileNotFoundException) {
        throw FileNotFoundException("Not able to find TOML file on path $filePath: ${e.message}")
    }
}

/**
 * Opens a file for writing via a [BufferedSink].
 *
 * @param filePath The path string pointing to a .toml file.
 * @return A [BufferedSink] writing to the specified [tomlFile] path.
 * @throws FileNotFoundException
 */
internal fun openFileForWrite(filePath: String): BufferedSink {
    try {
        val tomlPath = filePath.toPath()
        return getOsSpecificFileSystem().sink(tomlPath).buffer()
    } catch (e: FileNotFoundException) {
        throw FileNotFoundException("Not able to find TOML file on path $filePath: ${e.message}")
    }
}

/**
 * Implementation for getting proper file system to read files with okio
 *
 * @return proper FileSystem
 */
internal expect fun getOsSpecificFileSystem(): FileSystem
