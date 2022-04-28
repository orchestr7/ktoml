package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.tree.TomlFile
import kotlin.jvm.JvmInline

/**
 * @property config - object that stores configuration options for a writer
 */
@JvmInline
public value class TomlWriter(private val config: TomlConfig) {
    public fun writeToString(
        file: TomlFile,
        stringBuilder: StringBuilder = StringBuilder()
    ): String = "${write(file, stringBuilder)}"

    public fun write(
        file: TomlFile,
        emitter: TomlEmitter
    ): Unit = file.write(emitter, config)

    private fun write(file: TomlFile, stringBuilder: StringBuilder): StringBuilder {
        write(file, TomlStringEmitter(stringBuilder, config))

        return stringBuilder
    }
}
