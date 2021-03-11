package com.akuleshov7.parsers.node

import com.akuleshov7.error
import kotlin.system.exitProcess

// Toml specification includes a list of supported data types: String, Integer, Float, Boolean, Datetime, Array, and Table.
sealed class TomlNode(open val content: String, open val lineNo: Int) {
    open val children: MutableSet<TomlNode> = mutableSetOf()
    open var parent: TomlNode? = null

    fun insertBefore() {}
    fun insertAfter() {}
    fun addChildAfter() {}
    fun addChildBefore() {}

    /**
     * This method performs tree traversal and returns all table Nodes that have proper name and are on the proper level
     * @param searchedTableName - name of the table without braces and trimmed
     * @param searchedLevel - level inside of the tree where this table is stored,
     *                        count of levels in a normal tree that has a TomlFile as a root usually starts from 0
     */
    protected fun findTableInAstByName(
        searchedTableName: String,
        searchedLevel: Int,
        currentLevel: Int
    ): List<TomlTable> {
        val result = if (this is TomlTable && this.tableName == searchedTableName && currentLevel == searchedLevel) {
            mutableListOf(this)
        } else {
            mutableListOf()
        }
        return result + this.children.flatMap {
            if (currentLevel + 1 <= searchedLevel) {
                it.findTableInAstByName(searchedTableName, searchedLevel, currentLevel + 1)
            } else {
                mutableListOf()
            }
        }
    }

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

class TomlFile : TomlNode("rootNode", 0) {
    /**
     * This method recursively finds child toml table with the proper name in AST.
     * Stops processing if AST is broken and it has more than one table with the searched name on the same level.
     * @param searchedTableName - the table name that is expected to be found in the list of children of this node
     * @param searchedLevel - the level of nested child node that is searched level (indexed from 1)
     *
     * For example: findTableInAstByName("a.d", 2) will find [a.d] table in the following tree:
     *     a
     *    /  \
     *   a.c a.d
     *        \
     *        a.d.e
     */
    fun findTableInAstByName(searchedTableName: String, searchedLevel: Int): TomlTable? {
        val searchedTable = findTableInAstByName(searchedTableName, searchedLevel, 0)

        if (searchedTable.size > 1) {
            "Internal error: Found several Tables with the same name <$searchedTableName> in AST".error()
            exitProcess(1)
        }
        return if (searchedTable.isEmpty()) null else searchedTable[0]
    }

    fun insertTableToTree(tomlTable: TomlTable) {
        tomlTable.insertTableToTree(this)
    }
}

/**
 * @property tablesList - a list of names of sections (tables) that are included into this particular TomlTable
 * for example: if the TomlTable is [a.b.c] this list will contain [a], [a.b], [a.b.c]
 */
 class TomlTable(content: String, lineNo: Int) : TomlNode(content, lineNo) {
    var tableName: String
    var level: Int
    var tablesList: List<String>

    init {
        val sectionFromContent = "\\[(.*?)]"
            .toRegex()
            .find(content)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?: throw Exception()

        if (sectionFromContent.isBlank()) {
            error("Line $lineNo contains incorrect blank table name: $content")
        }

        tableName = sectionFromContent
        level = sectionFromContent.count { it == '.' }

        val sectionsList = sectionFromContent.split(".")
        tablesList = sectionsList.mapIndexed { index, secton ->
            (0..index).map { sectionsList[it] }.joinToString(".")
        }
    }

    fun insertTableToTree(treeHead: TomlFile) {
        // prevParentNode - saved node that is used in a chain
        var prevParentNode: TomlNode = treeHead
        // [a.b.c.d] -> for each section node checking existing node in a tree
        // [a], [a.b], [a.b.c], [a.b.c.d] -> if any of them does not exist we create and insert that in a tree
        this.tablesList.forEachIndexed { level, tableName ->
            val foundTableName = treeHead.findTableInAstByName(tableName, level + 1)
            foundTableName?.let {
                prevParentNode = it
            } ?: run {
                val newChildTableName = TomlTable("[$tableName]", lineNo)
                prevParentNode.appendChild(newChildTableName)
                prevParentNode = newChildTableName
            }
        }
    }
}

class TomlKeyValue(content: String, lineNo: Int) : TomlNode(content, lineNo) {
    var key: TomlKey
    var value: TomlValue

    init {
        val keyValue = content.split("=").map { it.trim() }
        if (keyValue.size != 2) {
            "Line $lineNo has incorrect format of Key-Value pair. Should be <key = value>, but was $content".error()
            exitProcess(1)
        }

        key = TomlKey(keyValue[0], lineNo)
        value = parseValue(keyValue[1], lineNo)

        this.appendChild(key)
        this.appendChild(value)
    }

    private fun parseValue(contentStr: String, lineNo: Int): TomlValue =
        if (contentStr == "true" || contentStr == "false") {
            TomlBoolean(contentStr, lineNo)
        } else {
            if (contentStr == "null") {
                TomlNull(lineNo)
            } else {
                try {
                    TomlInt(contentStr, lineNo)
                } catch (e: NumberFormatException) {
                    try {
                        TomlFloat(contentStr, lineNo)
                    } catch (e: NumberFormatException) {
                        TomlString(contentStr, lineNo)
                    }
                }
            }
        }
}

class TomlKey(content: String, lineNo: Int) : TomlNode(content, lineNo)

sealed class TomlValue(content: String, lineNo: Int) : TomlNode(content, lineNo)

class TomlString(content: String, lineNo: Int) : TomlValue(content, lineNo)

class TomlInt(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    var value: Int = content.toInt()
}

class TomlFloat(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    var value: Float = content.toFloat()
}

class TomlBoolean(content: String, lineNo: Int) : TomlValue(content, lineNo) {
    var value: Boolean = content.toBoolean()
}

class TomlNull(lineNo: Int) : TomlValue("null", lineNo)
