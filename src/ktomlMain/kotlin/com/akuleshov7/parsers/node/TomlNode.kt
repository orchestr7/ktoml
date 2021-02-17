package com.akuleshov7.parsers.node

import platform.posix.exit
import kotlin.system.exitProcess

// Toml specification includes a list of supported data types: String, Integer, Float, Boolean, Datetime, Array, and Table.
sealed class TomlNode(open val content: String) {
    open val children: MutableSet<TomlNode> = mutableSetOf()
    open var parent: TomlNode? = null
    open fun printContent() = content

    fun insertBefore() {}
    fun insertAfter() {}
    fun addChildAfter() {}
    fun addChildBefore() {}

    fun appendChild(child: TomlNode) {
        children.add(child)
        child.parent = this
    }

    companion object {
        // number of spaces that is used to indent levels
        const val INDENTING_LEVEL = 4

        fun prettyPrint(node: TomlNode, level: Int = 0) {
            val spaces = " ".repeat(INDENTING_LEVEL * level)
            println("$spaces - ${node::class.simpleName} (${node.content})")
            node.children.forEach { child ->
                prettyPrint(child, level + 1)
            }
        }
    }
}

class TomlFile : TomlNode("rootNode")

class TomlSection(content: String) : TomlNode(content) {
    var sectionName: String

    init {
        val sectionFromContent = "\\[(.*?)]"
            .toRegex()
            .find(content)
            ?.groupValues
            ?.get(1)

        // FixMe: create parse exceptions
        sectionName = sectionFromContent ?: throw Exception()
    }
}

class TomlVariable(content: String) : TomlNode(content) {
    var key: TomlKey
    var value: TomlValue

    init {
        val keyValue = content.split("=").map { it.trim() }
        // FixMe: need to throw a normal exception
        if (keyValue.size != 2) {
            throw Exception()
        }

        key = TomlKey(keyValue[0])
        value = parseValue(keyValue[1])

        this.appendChild(key)
        this.appendChild(value)
    }

    private fun parseValue(contentStr: String): TomlValue =
        if (contentStr == "true" || contentStr == "false") {
            TomlBoolean(contentStr)
        } else {
            if (contentStr == "null") {
                TomlNull()
            } else {
                try {
                    TomlInt(contentStr)
                } catch (e: NumberFormatException) {
                    try {
                        TomlFloat(contentStr)
                    } catch (e: NumberFormatException) {
                        TomlString(contentStr)
                    }
                }
            }
        }
}

class TomlKey(content: String) : TomlNode(content)

sealed class TomlValue(content: String) : TomlNode(content)

class TomlString(content: String) : TomlValue(content)

class TomlInt(content: String) : TomlValue(content) {
    var value: Int = content.toInt()
}

class TomlFloat(content: String) : TomlValue(content) {
    var value: Float = content.toFloat()
}

class TomlBoolean(content: String) : TomlValue(content) {
    var value: Boolean = content.toBoolean()
}

class TomlNull : TomlValue("null")
