/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException
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
     * Method that searches for a table (including array) with the same name as in [tableName].
     *
     * @param tableName the string with the name that will be searched in the list of children
     */
    public fun findTableInAstByName(tableName: String): List<TomlTable> {
        // getting all child-tables (and arrays of tables) that have the same name as we are trying to find
        val simpleTable = this.children.filterIsInstance<TomlTable>().filter { it.fullTableName == tableName }
        // there cannot be more than 1 table node with the same name on the same level in the tree
        if (simpleTable.size > 1) throw InternalAstException(
            "Invalid number of tables on the same level of AST were found. Is the tree corrupted?"
        )
        // we need to search this table in special technical nodes (TomlArrayOfTablesElement) that also contain tables
        val tableFromElements = this.children.asSequence()
            .filterIsInstance<TomlArrayOfTablesElement>()
            .map { it.children }
            .flatten()
            .filterIsInstance<TomlTable>()
            .filter { it.fullTableName == tableName }
            .toList()
        //
        return if (simpleTable.isNotEmpty()) listOf(simpleTable.last()) else simpleTable +
                if (tableFromElements.isNotEmpty()) listOf(tableFromElements.last()) else tableFromElements
    }

    public fun insertTableToTree(tomlTable: TomlTable, latestCreatedBucket: TomlArrayOfTablesElement? = null): TomlNode {
        var previousParent = this
        tomlTable.tablesList.forEachIndexed { level, subTable ->
            val foundTable = previousParent.findTableInAstByName(subTable)
            var constructNewBucket = false

            if (foundTable.isNotEmpty() && foundTable.single().parent is TomlArrayOfTablesElement) {
                val freeBucket = (foundTable.single().parent?.parent as TomlArrayOfTables).children.last()
                if (tomlTable !is TomlArrayOfTables) {
                    previousParent = freeBucket
                    constructNewBucket = true
                } else if(freeBucket == latestCreatedBucket) {
                    previousParent = freeBucket
                    constructNewBucket = true
                }
            }

            previousParent = if (foundTable.isNotEmpty() && !constructNewBucket) {
                foundTable.single()
            } else {
                if (level == tomlTable.tablesList.lastIndex) {
                    if (previousParent.children.filterIsInstance<TomlArrayOfTablesElement>().isNotEmpty()) {
                        previousParent.children.last().appendChild(tomlTable)
                    } else {
                        previousParent.appendChild(tomlTable)
                    }
                    tomlTable
                } else {
                    val newChildTableName = if (tomlTable is TomlArrayOfTables) {
                        TomlArrayOfTables("[[$subTable]]", lineNo, config, true)
                    } else {
                        TomlTablePrimitive("[$subTable]", lineNo, config, true)
                    }
                    if (previousParent.children.filterIsInstance<TomlArrayOfTablesElement>().isNotEmpty()) {
                        previousParent.children.last().appendChild(newChildTableName)
                    } else {
                        previousParent.appendChild(newChildTableName)
                    }
                    newChildTableName
                }
            }
        }
        return previousParent
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
