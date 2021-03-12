package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.error
import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsingError
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

    /**
     * Method inserts a table (section) to tree. It parses the section name and creates all missing nodes in the tree (even parental).
     * for [a.b.c] it will create 3 nodes: a, b, and c
     *
     * @param tomlTable - a table (section) that should be inserted into the tree
     */
    fun insertTableToTree(tomlTable: TomlTable) {
        // prevParentNode - saved node that is used in a chain
        var prevParentNode: TomlNode = this
        // [a.b.c.d] -> for each section node checking existing node in a tree
        // [a], [a.b], [a.b.c], [a.b.c.d] -> if any of them does not exist we create and insert that in a tree
        //
        // the only trick here is to save the link to the initial tomlTable (append it in the end)
        tomlTable.tablesList.forEachIndexed { level, tableName ->
            val foundTableName = this.findTableInAstByName(tableName, level + 1)
            foundTableName?.let {
                prevParentNode = it
            } ?: run {
                // hack and trick to save the link to the initial node (that was passed as an argument) in the tree
                // so the node will be added only in the end, and it will be the initial node
                if (level != tomlTable.tablesList.size - 1) {
                    val newChildTableName = TomlTable("[$tableName]", lineNo)
                    prevParentNode.appendChild(newChildTableName)
                    prevParentNode = newChildTableName
                } else {
                    prevParentNode.appendChild(tomlTable)
                }
            }
        }
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
}

class TomlKeyValue(content: String, lineNo: Int) : TomlNode(content, lineNo) {
    var key: TomlKey
    var value: TomlValue

    init {
        // FixMe: need to cover a case, when no value is present, because of the comment, but "=" is present: a = # comment
        // FixMe: need to cover a case, when '#' symbol is used inside the string ( a = "# hi") - is this supported?
        val keyValue = content.split("=")
            .map { it.substringBefore("#",) }
            .map { it.trim() }

        if (keyValue.size != 2) {
            "Incorrect format of Key-Value pair. Should be <key = value>, but was $content"
                .parsingError(lineNo)
        }

        val keyStr = keyValue[0].trim().also {
            if (it.isBlank()) {
                "Incorrect format of Key-Value pair. It has empty <key>: $content"
                    .parsingError(lineNo)
            }
        }
        // trimming and removing the comment in the end of the string
        val valueStr = keyValue[1].trim().also {
            if (it.isBlank()) {
                "Incorrect format of Key-Value pair. It has empty <value>: $content"
                    .parsingError(lineNo)
            }
        }

        key = TomlKey(keyStr, lineNo)
        value = parseValue(valueStr, lineNo)

        this.appendChild(key)
        this.appendChild(value)
    }

    /**
     * parsing content of the string to the proper Node type (for date -> TomlDate, string -> TomlString, e.t.c)
     */
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
