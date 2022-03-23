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

    private fun write(file: TomlFile, stringBuilder: StringBuilder): StringBuilder {
        file.write(TomlStringEmitter(stringBuilder, config), config)

        return stringBuilder
    }
}
