/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.writers.TomlEmitter

public const val EMPTY_TECHNICAL_NODE: String = "technical_node"

/**
 * Base Node class for AST.
 * Toml specification includes a list of supported data types:
 * String, Integer, Float, Boolean, Datetime, Array, and Table.
 *
 * @param comments Comments prepended to the current node
 *
 * @property content - original node content (used for logging and tests only)
 * @property lineNo - the number of a line from TOML that is linked to the current node
 * @property inlineComment A comment appended to the end of the line
 * @property config
 */
public sealed class TomlNode(
    public open val content: String,
    public open val lineNo: Int,
    comments: List<String>,
    public val inlineComment: String,
    public open val config: TomlConfig = TomlConfig()
) {
    /**
     * A list of comments prepended to the node.
     */
    public val comments: MutableList<String> = comments.toMutableList()
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
        comments: List<String>,
        inlineComment: String,
        config: TomlConfig = TomlConfig()
    ) : this(
        "${key.content}=${value.content}",
        lineNo,
        comments,
        inlineComment,
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
     * @return list of tables that match the provided name
     * @throws InternalAstException
     */
    public fun findTableInAstByName(tableName: String): TomlTable? {
        // getting all child-tables (and arrays of tables) that have the same name as we are trying to find
        val simpleTable = this.children.filterIsInstance<TomlTable>().filter { it.fullTableName == tableName }
        // there cannot be more than 1 table node with the same name on the same level in the tree
        if (simpleTable.size > 1) {
            throw InternalAstException(
                "Invalid number of tables on the same level of AST were found. Is the tree corrupted?"
            )
        }
        // we need to search this table in special technical nodes (TomlArrayOfTablesElement) that also contain tables
        val tableFromElements = this.children.asSequence()
            .filterIsInstance<TomlArrayOfTablesElement>()
            .map { it.children }
            .flatten()
            .filterIsInstance<TomlTable>()
            .filter { it.fullTableName == tableName }
            .toList()
        // return the table that we found among the list of child tables or in the array of tables
        return simpleTable.lastOrNull() ?: tableFromElements.lastOrNull()
    }

    /**
     * @param tomlTable table that we would like to insert
     * @param latestCreatedBucket the bucket of the latest created array of tables
     * @return link to the inserted table inside the tree
     */
    @Suppress("TOO_LONG_FUNCTION")
    public fun insertTableToTree(tomlTable: TomlTable, latestCreatedBucket: TomlArrayOfTablesElement? = null): TomlNode {
        // important to save and update parental node
        var previousParent = this
        // going through parts of the table to create new tables in the tree in case some fragments are missing
        tomlTable.tablesList.forEachIndexed { level, subTable ->
            val foundTable = previousParent.findTableInAstByName(subTable)
            // flag that will be used to check if we need to create a copy of the table in the tree or not
            var constructNewBucket = false
            // if the part of the table was found and it is inside the array - we need to determine if we need to create a copy or not
            if (foundTable != null && foundTable.parent is TomlArrayOfTablesElement) {
                val freeBucket = (foundTable.parent?.parent as TomlArrayOfTables).children.last()
                // need to create a new array of tables in the tree only
                // if there was an array before:
                // [[a]]                                                                      [[a]]
                // [[a.b]]   in case of the nested array we should not create a copy:      [[a.b]]
                // [[a]]                                                                       [[a.b]]
                // [[a.b]]                                                                 [[a.b]]
                if (tomlTable !is TomlArrayOfTables || freeBucket == latestCreatedBucket) {
                    previousParent = freeBucket
                    constructNewBucket = true
                }
            }

            previousParent = if (foundTable != null && !constructNewBucket) {
                // in case of inline tables we generate a structure that already has key-values inserted to it,
                // so we need to copy these children to the TOML AST
                if (level == tomlTable.tablesList.lastIndex) {
                    tomlTable.children.forEach {
                        foundTable.appendChild(it)
                    }
                }
                foundTable
            } else {
                if (level == tomlTable.tablesList.lastIndex) {
                    previousParent.determineParentAndInsertFragmentOfTable(tomlTable)
                    tomlTable
                } else {
                    // creating a synthetic (technical) fragment of the table
                    val newChildTableName = if (tomlTable is TomlArrayOfTables) {
                        TomlArrayOfTables(
                            "[[$subTable]]",
                            lineNo,
                            config,
                            true
                        )
                    } else {
                        TomlTablePrimitive(
                            "[$subTable]",
                            lineNo,
                            tomlTable.comments,
                            tomlTable.inlineComment,
                            config,
                            true
                        )
                    }
                    previousParent.determineParentAndInsertFragmentOfTable(newChildTableName)
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

    /**
     * print the structure of parsed AST tree
     */
    public fun prettyPrint() {
        val sb = StringBuilder()
        prettyPrint(this, sb)
        println(sb.toString())
    }

    /**
     * @return the string with AST tree visual representation
     */
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

    private fun determineParentAndInsertFragmentOfTable(childTable: TomlTable) {
        if (this.children.filterIsInstance<TomlArrayOfTablesElement>().isNotEmpty()) {
            this.children.last().appendChild(childTable)
        } else {
            this.appendChild(childTable)
        }
    }

    /**
     * Writes this node as text to [emitter].
     *
     * @param emitter The [TomlEmitter] instance to write to.
     * @param config The [TomlConfig] instance. Defaults to the node's config.
     * @param multiline Whether to write the node over multiple lines, if possible.
     */
    public abstract fun write(
        emitter: TomlEmitter,
        config: TomlConfig = this.config,
        multiline: Boolean = false
    )

    protected open fun TomlEmitter.writeChildren(
        children: List<TomlNode>,
        config: TomlConfig,
        multiline: Boolean
    ) {
        val last = children.lastIndex

        children.forEachIndexed { i, child ->
            writeChildComments(child)

            emitIndent()
            child.write(emitter = this, config, multiline)

            if (child is TomlKeyValue || child is TomlInlineTable) {
                writeChildInlineComment(child)
            }

            if (i < last) {
                emitNewLine()

                // Primitive pairs have a single newline after, except when a table
                // follows.
                if (child !is TomlKeyValuePrimitive ||
                        children[i + 1] is TomlTable) {
                    emitNewLine()
                }
            }
        }
    }

    protected fun TomlEmitter.writeChildComments(child: TomlNode) {
        child.comments.forEach { comment ->
            emitIndent()
                .emitComment(comment)
                .emitNewLine()
        }
    }

    protected fun TomlEmitter.writeChildInlineComment(child: TomlNode) {
        if (child.inlineComment.isNotEmpty()) {
            emitComment(child.inlineComment, inline = true)
        }
    }

    public companion object {
        // number of spaces that is used to indent levels
        internal const val INDENTING_LEVEL = 4

        /**
         * recursive print the tree using the current node
         *
         * @param node
         * @param level
         * @param result
         */
        public fun prettyPrint(
            node: TomlNode,
            result: StringBuilder,
            level: Int = 0
        ) {
            val spaces = " ".repeat(INDENTING_LEVEL * level)
            result.append("$spaces - ${node::class.simpleName} (${node.content})\n")
            node.children.forEach { child ->
                prettyPrint(child, result, level + 1)
            }
        }
    }
}
