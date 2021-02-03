package com.akuleshov7.parsers.node

// Toml specification includes a list of supported data types: String, Integer, Float, Boolean, Datetime, Array, and Table.
sealed class TomlNode(open val content: String) {
    open val children: MutableSet<TomlNode> = mutableSetOf()
    open var parent: TomlNode? = null

    fun insertBefore() {}
    fun insertAfter() {}
    fun addChildAfter() {}
    fun addChildBefore() {}

    fun appendChild(child: TomlNode) {
        children.add(child)
        child.parent = this
    }
}

class TomlFile : TomlNode("rootNode")

class TomlSection(content: String) : TomlNode(content)

class TomlVariable(content: String) : TomlNode(content) {
    var key: TomlKey
    var value: TomlValue

    init {
        val keyValue = content.split("=").map { it.trim() }
        if (keyValue.size != 2) { throw Exception() }

        key = TomlKey(keyValue[0])
        value = TomlInt(keyValue[1])

        this.appendChild(key)
        this.appendChild(value)
    }
}

class TomlKey(content: String) : TomlNode(content)

sealed class TomlValue(content: String) : TomlNode(content)

class TomlString(content: String) : TomlValue(content)
class TomlInt(content: String) : TomlValue(content)
class TomlFloat(content: String) : TomlValue(content)
class TomlDate(content: String) : TomlValue(content)
class TomlBoolean(content: String) : TomlValue(content)
class TomlTable(content: String): TomlValue(content)

class TomlNull(content: String) : TomlValue(content)

class TomlArray(content: String) : TomlValue(content) {
    var values: List<String>
    init {
        values = listOf()
    }
}


fun main() {
    val parent = TomlFile()
    val child1 = TomlVariable("a = 1234")
    val child2 = TomlVariable("b = 12345")

    parent.appendChild(child1)
    parent.appendChild(child2)
}

