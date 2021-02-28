package com.akuleshov7.parsers.node

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

    fun findNodeWithProperType() {}

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

class TomlTable(content: String) : TomlNode(content) {
    var sectionName: String
    var level: Int
    var sections: List<String>

    init {
        val sectionFromContent = "\\[(.*?)]"
            .toRegex()
            .find(content)
            ?.groupValues
            ?.get(1)

        // FixMe: create parse exceptions
        sectionName = sectionFromContent ?: throw Exception()
        level = sectionName.count { it == '.' }
        sections = sectionName.split(".")

    }
}

class TomlVariable(content: String) : TomlNode(content) {
    var key: TomlKey
    var value: TomlValue

    init {
        val keyValue = content.split("=").map { it.trim() }
        println(content)
        println(keyValue)
        if (keyValue.size != 2) {
            // FixMe: need to log a good error and throw a normal exception
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
