package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.TomlWritingException
import com.akuleshov7.ktoml.tree.*
import kotlin.jvm.JvmInline

/**
 * @property config - object that stores configuration options for a writer
 */
@JvmInline
@Suppress("WRONG_MULTIPLE_MODIFIERS_ORDER")  // This is, in fact, the canonical order.
public value class TomlWriter(private val config: TomlConfig) {
    public fun writeToString(
        file: TomlFile,
        stringBuilder: StringBuilder = StringBuilder()
    ): String = "${write(file, stringBuilder)}"

    private fun write(file: TomlFile, stringBuilder: StringBuilder): StringBuilder {
        write(file, TomlStringEmitter(stringBuilder, config))

        return stringBuilder
    }

    public fun write(file: TomlFile, emitter: TomlEmitter): Unit =
            file.children.forEach { emitter.writeChild(it) }

    private fun TomlEmitter.writeChild(node: TomlNode): Unit = when (node) {
        is TomlFile ->
            throw TomlWritingException(
                "A file node is not allowed as a child of another file node."
            )
        is TomlKeyValueArray -> TODO()
        is TomlKeyValuePrimitive -> TODO()
        is TomlStubEmptyNode -> TODO()
        is TomlTable -> TODO()
    }

    private fun TomlEmitter.writeKey(key: TomlKey) {
        val keys = key.keyParts

        if (keys.isEmpty()) {
            emitQuotedKey("")

            return
        }

        emitKey(keys[0])

        // Todo: Use an iterator instead of creating a new list.
        keys.drop(1).forEach {
            emitKeyDot()
            emitKey(it)
        }
    }
}
