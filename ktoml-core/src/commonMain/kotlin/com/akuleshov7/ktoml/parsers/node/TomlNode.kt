package com.akuleshov7.ktoml.parsers.node

import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.exceptions.TomlParsingException
import com.akuleshov7.ktoml.parsers.ParserConf

// Toml specification includes a list of supported data types: String, Integer, Float, Boolean, Datetime, Array, and Table.
sealed class TomlNode(open val content: String, open val lineNo: Int) {
    constructor(keyValuePair: Pair<String, String>, lineNo: Int) : this(
        "${keyValuePair.first}=${keyValuePair.second}",
        lineNo
    )

    open val children: MutableSet<TomlNode> = mutableSetOf()
    open var parent: TomlNode? = null
    abstract val name: String

    fun hasNoChildren() = children.size == 0
    fun getFirstChild() = children.elementAtOrNull(0)
    open fun getNeighbourNodes() = parent!!.children

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
}

/**
 * class for parsing
 */
class TomlKeyValueList(
    keyValuePair: Pair<String, String>,
    override val lineNo: Int,
) : TomlNode(keyValuePair, lineNo), TomlKeyValue {
    override var key: TomlKey = TomlKey(keyValuePair.first, lineNo)
    override val value: TomlValue = parseList(keyValuePair.second, lineNo)
    override val name: String = key.content
}

class TomlKeyValueSimple(
    keyValuePair: Pair<String, String>,
    override val lineNo: Int,
) : TomlNode(keyValuePair, lineNo), TomlKeyValue {
    override var key: TomlKey = TomlKey(keyValuePair.first, lineNo)
    override val value: TomlValue = keyValuePair.second.parseValue(lineNo)
    override val name: String = key.content
}

/**
 * this is a hack to cover empty TOML tables that have missing key-values
 * According the spec: "Empty tables are allowed and simply have no key/value pairs within them."
 *
 * Instances of this stub will be added as children to such parsed tables
 */
class TomlStubEmptyNode(lineNo: Int) : TomlNode("empty_technical_node", lineNo) {
    override val name: String = "empty_technical_node"
}
