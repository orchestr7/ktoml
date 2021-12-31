package com.akuleshov7.ktoml.writers

import com.akuleshov7.ktoml.KtomlConf
import com.akuleshov7.ktoml.parsers.node.*
import kotlin.jvm.JvmInline

@JvmInline
public value class TomlWriter(private val ktomlConf: KtomlConf) {
    public fun writeToString(
        file: TomlFile,
        stringBuilder: StringBuilder = StringBuilder()
    ): String = "${write(file, stringBuilder)}"

    private fun write(file: TomlFile, stringBuilder: StringBuilder): StringBuilder
    {
        write(file, TomlStringComposer(stringBuilder, ktomlConf))

        return stringBuilder
    }

    public fun write(file: TomlFile, composer: TomlComposer): Unit =
        file.children.forEach { composer.writeChild(it) }

    private fun TomlComposer.writeChild(node: TomlNode) = when (node) {
        is TomlFile ->
            error(
                "A file node is not allowed as a child of another file node."
            )
        is TomlKeyValueArray ->
        {

        }
        is TomlKeyValuePrimitive ->
        {

        }
        is TomlStubEmptyNode -> { }
        is TomlTable ->
        {

        }
    }

    private fun TomlComposer.writeKey(key: TomlKey)
    {
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