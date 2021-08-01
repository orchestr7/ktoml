package com.akuleshov7.ktoml

import okio.ExperimentalFileSystem
import okio.FileNotFoundException
import okio.FileSystem
import okio.Path.Companion.toPath

@ExperimentalFileSystem
internal fun readAndParseFile(toml: String): List<String> {
    try {
        val ktomlPath = toml.toPath()
        return FileSystem.SYSTEM.read(ktomlPath) {
            // FixMe: may be we need to read and at the same time parse (to make it parallel)
            generateSequence { readUtf8Line() }.toList()
        }
    } catch (e: FileNotFoundException) {
        println("Not able to find toml-file in the following path: $toml")
        throw e
    }
}
