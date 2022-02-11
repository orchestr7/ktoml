/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException

public const val EMPTY_TECHNICAL_NODE: String = "technical_node"

/**
 * Base Node class for AST.
 * Toml specification includes a list of supported data types:
 * String, Integer, Float, Boolean, Datetime, Array, and Table.
 *
 * @property content - original node content (used for logging and tests only)
 * @property lineNo - the number of a line from TOML that is linked to the current node
 * @property config
 */
public sealed class TomlNode(
    public open val content: String,
    public open val lineNo: Int,
    public open val config: TomlConfig = TomlConfig()
) {
    public open val children: MutableSet<TomlNode> = mutableSetOf()
    public open var parent: TomlNode? = null

    // the real toml name of a structure (for table [a] it will be "a", for key b = 1 it will be "b")
    // used for logging and errors AND for matching the name of the node to the name of the properties in the class
    // see: [checkMissingRequiredProperties]
    public abstract val name: String

    // this constructor is used by TomlKeyValueList and TomlKeyValuePrimitive and we concatenate keyValuePair to the content
    // only for logging, debug information and unification of the code
    protected constructor(
        key: TomlKey,
        value: TomlValue,
        lineNo: Int,
        config: TomlConfig = TomlConfig()) : this(
        "${key.content}=${value.content}",
        lineNo,
        config
    )

    /**
     * @return true if has no children
     */
    public fun hasNoChildren(): Boolean = children.size == 0

    /**
     * @return first child or null
     */
    public fun getFirstChild(): TomlNode? = children.elementAtOrNull(0)

    /**
     * @return all neighbours (all children of current node's parent)
     */
    public open fun getNeighbourNodes(): MutableSet<TomlNode> = parent!!.children

    /**
     * This method performs tree traversal and returns all table Nodes that have proper name and are on the proper level
     *
     * @param searchedTableName - name of the table without braces and trimmed
     * @param searchedLevel - level inside of the tree where this table is stored,
     *                        count of levels in a normal tree that has a TomlFile as a root usually starts from 0
     * @param currentLevel
     * @return a list of table nodes with the same name and that stay on the same level
     */
    private fun findTableInAstByName(
        searchedTableName: String,
        searchedLevel: Int,
        currentLevel: Int,
        type: TableType
    ): List<TomlTable> {
        val result =
               // we need to filter nodes by the type of table that we are inserting to the tree (array/primitive)
                if (this is TomlTable && this.type == type &&
                    this.fullTableName == searchedTableName && currentLevel == searchedLevel) {
                    mutableListOf(this)
                } else {
                    mutableListOf()
                }
        return result + this.children.flatMap {
            if (currentLevel + 1 <= searchedLevel) {
                it.findTableInAstByName(searchedTableName, searchedLevel, currentLevel + 1, type)
            } else {
                mutableListOf()
            }
        }
    }

    /**
     * This method recursively finds child toml table with the proper name in AST.
     * Stops processing if AST is broken and it has more than one table with the searched name on the same level.
     *
     * @param searchedTableName - the table name that is expected to be found in the list of children of this node
     * @param searchedLevel - the level of nested child node that is searched level (indexed from 1)
     *
     * For example: findTableInAstByName("a.d", 2) will find [a.d] table in the following tree:
     *     a
     *    /  \
     *   a.c a.d
     *        \
     *        a.d.e
     * @return table that was found or null in case of not found
     * @throws ParseException if found several tables with the same name
     */
    public fun findTableInAstByName(searchedTableName: String, searchedLevel: Int, type: TableType): TomlTable? {
        val searchedTable = findTableInAstByName(searchedTableName, searchedLevel, 0, type)

        if (searchedTable.size > 1) {
            throw ParseException(
                "Internal error: Found several Tables with the same name <$searchedTableName> in AST",
                searchedTable.first().lineNo
            )
        }
        return if (searchedTable.isEmpty()) null else searchedTable[0]
    }


    /**
     * Method inserts a table (section) to tree. It parses the section name and creates all missing nodes in the tree
     * (even parental). For [a.b.c] it will create 3 nodes: a, b, and c
     *
     * @param tomlTable - a table (section) that should be inserted into the tree
     * @return inserted table
     */
    public fun <T: TomlTable> insertTableToTree(tomlTable: T, type: TableType): TomlNode {
        // prevParentNode - saved node that is used in a chain
        var prevParentNode: TomlNode = this
        // [a.b.c.d] -> for each section node checking existing node in a tree
        // [a], [a.b], [a.b.c], [a.b.c.d] -> if any of them does not exist we create and insert that in a tree
        // 
        // the only trick here is to save the link to the initial tomlTable (append it in the end)
        tomlTable.tablesList.forEachIndexed { level, tableName ->
            val foundTableName = this.findTableInAstByName(tableName, level + 1, type)

            foundTableName?.let {
                prevParentNode = it
            } ?: run {
                // if we came to the last part (to 'd' from a.b.c.d) of the table - just will insert our table to the end
                prevParentNode = if (level == tomlTable.tablesList.size - 1) {
                    prevParentNode.appendChild(tomlTable)
                    tomlTable
                } else {
                    // hack and trick to save the link to the initial node (that was passed as an argument) in the tree
                    // so the node will be added only in the end, and it will be the initial node
                    // (!) we will mark these tables with 'isSynthetic' flag
                    val newChildTableName = TomlTablePrimitive("[$tableName]", lineNo, config, true)
                    prevParentNode.appendChild(newChildTableName)
                    newChildTableName
                }
            }
        }
        return prevParentNode
    }

    /**
     * @param child that will be added to this parent
     */
    public fun appendChild(child: TomlNode) {
        children.add(child)
        child.parent = this
    }

    public fun prettyPrint() {
        prettyPrint(this)
    }

    /**
     * This method returns all available table names that can be found in this particular TOML file
     * (!) it will also return synthetic table nodes, that we generated to create a normal tree structure
     *
     * @return all detected toml tables
     */
    public fun getAllChildTomlTables(): List<TomlTablePrimitive> {
        val result = if (this is TomlTablePrimitive) mutableListOf(this) else mutableListOf()
        return result + this.children.flatMap {
            it.getAllChildTomlTables()
        }
    }

    /**
     * find only real table nodes without synthetics
     *
     * @return all real table nodes
     */
    public fun getRealTomlTables(): List<TomlTablePrimitive> =
            this.getAllChildTomlTables().filter { !it.isSynthetic }

    public companion object {
        // number of spaces that is used to indent levels
        internal const val INDENTING_LEVEL = 4

        /**
         * recursive print the tree using the current node
         *
         * @param node
         * @param level
         */
        public fun prettyPrint(node: TomlNode, level: Int = 0) {
            val spaces = " ".repeat(INDENTING_LEVEL * level)
            println("$spaces - ${node::class.simpleName} (${node.content})")
            node.children.forEach { child ->
                prettyPrint(child, level + 1)
            }
        }
    }
}

