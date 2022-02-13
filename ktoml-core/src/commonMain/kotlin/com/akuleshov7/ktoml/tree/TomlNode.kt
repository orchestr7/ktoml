/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree

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
    public open val children: MutableList<TomlNode> = mutableListOf()
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
        config: TomlConfig = TomlConfig()
    ) : this(
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
    public open fun getNeighbourNodes(): MutableList<TomlNode> = parent!!.children

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
    ): List<TomlTable> {
        val result =
                if (this is TomlTable && this.fullTableName == searchedTableName && currentLevel == searchedLevel) {
                    mutableListOf(this)
                } else {
                    mutableListOf()
                }
        return result + this.children.flatMap {
            if (currentLevel + 1 <= searchedLevel) {
                val level = if (it is TomlArrayOfTablesElement) currentLevel else currentLevel + 1
                it.findTableInAstByName(searchedTableName, searchedLevel, level)
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
     * @param type
     * @return table that was found or null in case of not found
     * @throws ParseException if found several tables with the same name
     */
    public fun findTableInAstByName(
        searchedTableName: String,
        searchedLevel: Int,
    ): TomlTable? {
        val searchedTables = findTableInAstByName(searchedTableName, searchedLevel, 0)

        if (searchedTables.size > 1) {
            throw ParseException(
                "Internal error: Found several Tables with the same name <$searchedTableName> in AST",
                searchedTables.first().lineNo
            )
        }
        return if (searchedTables.isEmpty()) null else searchedTables[0]
    }

    /**
     * Method inserts a table (section) to tree. It parses the section name and creates all missing nodes in the tree
     * (even parental). For [a.b.c] it will create 3 nodes: a, b, and c
     *
     * @param tomlTable - a table (section) that should be inserted into the tree
     * @param type
     * @return inserted table
     */
    public fun insertTableToTree(tomlTable: TomlTable): TomlNode {
        // prevParentNode - saved node that is used in a chain
        var prevParentNode: TomlNode = this
        // [a.b.c.d] -> for each section node checking existing node in a tree
        // [a], [a.b], [a.b.c], [a.b.c.d] -> if any of them does not exist we create and insert that in a tree
        // 
        // the only trick here is to save the link to the initial tomlTable (append it in the end)
        tomlTable.tablesList.forEachIndexed { level, subTable ->
            // each time we are trying to find the particular table in the tree
            // that is NOT optimal, because:
            // 1) we begin the search from the root of the tree instead of keeping in mind our last search
            // 2) no need to search the whole tree for the [a.b] if we haven't found [a] already
            val foundTableInTree = this.findTableInAstByName(subTable, level + 1)

            foundTableInTree?.let {
                prevParentNode = when {
                    level == tomlTable.tablesList.lastIndex && prevParentNode is TomlArrayOfTablesElement -> {
                        prevParentNode.parent?.children?.last()?.appendChild(tomlTable)
                        tomlTable
                    }
                    it is TomlArrayOfTables && tomlTable.tablesList.size != foundTableInTree.tablesList.size -> it.children.last()
                    else -> it
                }
                // if the new table belongs to the ARRAY OF TABLES - we should insert this table to it's last element
                // but if it's just array of table with the same name (new element) - then
                // we should insert it to parental TomlArrayOfTables, but not to TomlArrayOfTablesElement
            //    FixMe: here should throw an exception in case of table duplication https://github.com/akuleshov7/ktoml/issues/30

            } ?: run {
                // if we came to the last part (to 'd' from a.b.c.d) of the table - just will insert our table to the end
                prevParentNode = if (level == tomlTable.tablesList.lastIndex) {
                    prevParentNode.appendChild(tomlTable)
                    tomlTable
                } else {
                    // hack and trick to save the link to the initial node (that was passed as an argument) in the tree
                    // so the node will be added only in the end, and it will be the initial node
                    // (!) we will mark these tables with 'isSynthetic' flag
                    // also note that we will save the initial type of the table for missing parts
                    val newChildTableName = if(tomlTable is TomlArrayOfTables) {
                        TomlArrayOfTables("[[$subTable]]", lineNo, config, true)
                    } else {
                        TomlTablePrimitive("[$subTable]", lineNo, config, true)
                    }
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
        val sb = StringBuilder()
        prettyPrint(this, sb)
        println(sb.toString())
    }

    public fun prettyStr(): String {
        val sb = StringBuilder()
        prettyPrint(this, sb)
        return sb.toString()
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
        public fun prettyPrint(node: TomlNode, result: StringBuilder, level: Int = 0) {
            val spaces = " ".repeat(INDENTING_LEVEL * level)
            result.append("$spaces - ${node::class.simpleName} (${node.content})\n")
            node.children.forEach { child ->
                prettyPrint(child, result, level + 1)
            }
        }
    }
}
