/**
 * File utils to read files using okio
 */

package com.akuleshov7.ktoml.file

import okio.*
import okio.Path.Companion.toPath

/**
 * Uses okio to get the source from a path
 *
 * @throws FileNotFoundException if the toml file is missing
 */
internal fun getFileSource(filePath: String): Source {
    try {
        return getOsSpecificFileSystem().source(filePath.toPath())
    } catch (e: FileNotFoundException) {
        throw FileNotFoundException("Not able to find TOML file on path $filePath: ${e.message}")
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
        val tomlPath = tomlFile.toPath()
        return getOsSpecificFileSystem().sink(tomlPath).buffer()
    } catch (e: FileNotFoundException) {
        throw FileNotFoundException("Not able to find TOML file on path $tomlFile: ${e.message}")
    }
}

/**
 * Implementation for getting proper file system to read files with okio
 *
 * @return proper FileSystem
 */
internal expect fun getOsSpecificFileSystem(): FileSystem
