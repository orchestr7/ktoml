package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.ParserConf

// Toml specification includes a list of supported data types: String, Integer, Float, Boolean, Datetime, Array, and Table.
sealed class TomlNode(open val content: String, open val lineNo: Int) {
    open val children: MutableSet<TomlNode> = mutableSetOf()
    open var parent: TomlNode? = null
    abstract val name: String

    fun hasNoChildren() = children.size == 0
    fun getFirstChild() = children.elementAtOrNull(0)
    abstract fun getNeighbourNodes(): MutableSet<TomlNode>

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
        val result =
            if (this is TomlTable && this.fullTableName == searchedTableName && currentLevel == searchedLevel) {
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
            throw TomlParsingException(
                "Internal error: Found several Tables with the same name <$searchedTableName> in AST",
                searchedTable.first().lineNo
            )
        }
        return if (searchedTable.isEmpty()) null else searchedTable[0]
    }

    /**
     * Method inserts a table (section) to tree. It parses the section name and creates all missing nodes in the tree (even parental).
     * for [a.b.c] it will create 3 nodes: a, b, and c
     *
     * @param tomlTable - a table (section) that should be inserted into the tree
     */
    fun insertTableToTree(tomlTable: TomlTable): TomlNode {
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
                // if we came to the last part of table - just insert our table to the end
                if (level == tomlTable.tablesList.size - 1) {
                    prevParentNode.appendChild(tomlTable)
                    prevParentNode = tomlTable
                } else {
                    // hack and trick to save the link to the initial node (that was passed as an argument) in the tree
                    // so the node will be added only in the end, and it will be the initial node
                    // (!) we will mark these tables with 'isSynthetic' flag
                    val newChildTableName = TomlTable("[$tableName]", lineNo, true)
                    prevParentNode.appendChild(newChildTableName)
                    prevParentNode = newChildTableName
                }
            }
        }
        return prevParentNode
    }

    fun appendChild(child: TomlNode) {
        children.add(child)
        child.parent = this
    }

    fun prettyPrint() {
        prettyPrint(this)
    }

    /**
     * This method returns all available table names that can be found in this particular TOML file
     * (!) it will also return synthetic table nodes, that we generated to create a normal tree structure
     */
    fun getAllChildTomlTables(): List<TomlTable> {
        val result = if (this is TomlTable) mutableListOf(this) else mutableListOf()
        return result + this.children.flatMap {
            it.getAllChildTomlTables()
        }
    }

    /**
     * find only real table nodes without synthetics
     */
    fun getRealTomlTables(): List<TomlTable> =
        this.getAllChildTomlTables().filter { !it.isSynthetic }


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
    override val name = "rootNode"

    override fun getNeighbourNodes() =
        throw InternalAstException("Invalid call to getNeighbourNodes() for TomlFile node")
}

/**
 * @property tablesList - a list of names of sections (tables) that are included into this particular TomlTable
 * @property isSynthetic - flag to determine that this node was synthetically and there are no such table in the input
 * for example: if the TomlTable is [a.b.c] this list will contain [a], [a.b], [a.b.c]
 */
class TomlTable(content: String, lineNo: Int, val isSynthetic: Boolean = false) : TomlNode(content, lineNo) {
    // short table name (only the name without parential prefix, like a)
    override val name: String

    // full name of the table (like a.b.c.d)
    var fullTableName: String

    // number of nodes in current table (starting from 0)
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
            throw TomlParsingException("Incorrect blank table name: $content", lineNo)
        }

        fullTableName = sectionFromContent
        level = sectionFromContent.count { it == '.' }

        // FixMe: this is invalid for the following tables: "google.com" (it will be split now)
        val sectionsList = sectionFromContent.split(".")
        name = sectionsList.last()
        tablesList = sectionsList.mapIndexed { index, secton ->
            (0..index).map { sectionsList[it] }.joinToString(".")
        }
    }

    override fun getNeighbourNodes() = parent!!.children
}

// be careful here as 'parserConf' has the default value (for test purposes)
class TomlKeyValue(content: String, lineNo: Int, val parserConf: ParserConf = ParserConf()) :
    TomlNode(content, lineNo) {
    var key: TomlKey
    var value: TomlValue

    // same to the content in the TomlKey
    override val name: String

    init {
        // FixMe: need to cover a case, when '#' symbol is used inside the string ( a = "# hi") (supported by the spec)
        val keyValue = content.substringBefore("#")
            .split("=")
            .map { it.trim() }

        if (keyValue.size != 2) {
            throw TomlParsingException(
                "Incorrect format of Key-Value pair. Should be <key = value>, but was: $content",
                lineNo
            )
        }

        val keyStr = keyValue.getKeyValuePart("value", 0)
        val valueStr = keyValue.getKeyValuePart("value", 1)

        key = TomlKey(keyStr, lineNo)
        value = parseValue(valueStr, lineNo)
        name = key.content
    }

    private fun List<String>.getKeyValuePart(log: String, index: Int) =
        this[index].trim().also {
            // key should never be empty, but the value can be empty (and treated as null)
            // see the discussion: https://github.com/toml-lang/toml/issues/30
            if ((!parserConf.emptyValuesAllowed || index == 0) && it.isBlank()) {
                throw TomlParsingException(
                    "Incorrect format of Key-Value pair. It has empty $log: $content",
                    lineNo
                )
            }
        }

    override fun getNeighbourNodes() = parent!!.children

    /**
     * this is a small hack to support dotted keys
     * in case we have the following key: a.b.c = "val" we will simply create a new table:
     *  [a.b]
     *     c = "val"
     * and we will let our Table mechanism to do everything for us
     */
    fun createTomlTableFromDottedKey(parentNode: TomlNode): TomlTable {
        // for a key: a.b.c it will be [a, b]
        val syntheticTablePrefix = this.key.keyParts.dropLast(1)
        // creating new key with the last dor-separated fragment
        val realKeyWithoutDottedPrefix = TomlKey(key.keyParts.last(), lineNo)
        // updating current KeyValue with this key
        this.key = realKeyWithoutDottedPrefix
        // tables should contain fully qualified name, so we need to add parental name
        val parentalPrefix = if (parentNode is TomlTable) "${parentNode.fullTableName}." else ""
        // and creating a new table that will be created from dotted key
        val newTable = TomlTable(
            "[$parentalPrefix${syntheticTablePrefix.joinToString(".")}]",
            lineNo,
            true
        )

        return newTable
    }

    /**
     * parsing content of the string to the proper Node type (for date -> TomlDate, string -> TomlString, e.t.c)
     */
    private fun parseValue(contentStr: String, lineNo: Int): TomlValue {
        return when (contentStr) {
            "null", "nil", "NULL", "NIL", "" -> TomlNull(lineNo)
            "true", "false" -> TomlBoolean(contentStr, lineNo)
            else -> try {
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

/**
 * this is a hack to cover empty TOML tables that have missing key-values
 * According the spec: "Empty tables are allowed and simply have no key/value pairs within them."
 *
 * Instances of this stub will be added as children to such parsed tables
 */
class TomlStubEmptyNode(lineNo: Int) : TomlNode("empty_technical_node", lineNo) {
    override val name: String = "empty_technical_node"

    override fun getNeighbourNodes() = parent!!.children
}
