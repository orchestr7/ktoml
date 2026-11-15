/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import com.akuleshov7.ktoml.exceptions.InternalAstException
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.tree.nodes.pairs.values.TomlValue
import com.akuleshov7.ktoml.writers.TomlEmitter

@InternalKtomlApi
public const val EMPTY_TECHNICAL_NODE: String = "technical_node"

/**
 * Base Node class for AST.
 * Toml specification includes a list of supported data types:
 * String, Integer, Float, Boolean, Datetime, Array, and Table.
 *
 * @param comments Comments prepended to the current node
 * @property lineNo - the number of a line from TOML that is linked to the current node
 * @property inlineComment A comment appended to the end of the line
 */
@InternalKtomlApi
public sealed class TomlNode(
    public open val lineNo: Int,
    comments: List<String>,
    public val inlineComment: String
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
    // FixMe: need to clarify why this code became unused
    protected constructor(
        key: TomlKey,
        value: TomlValue,
        lineNo: Int,
        comments: List<String>,
        inlineComment: String,
        config: TomlInputConfig = TomlInputConfig()
    ) : this(
        lineNo,
        comments,
        inlineComment
    )

    /**
     * @return true if node has no children
     */
    public fun hasNoChildren(): Boolean = children.isEmpty()

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
        val tableKey = TomlKey(tableName, lineNo)
        val tableKeyName = tableKey.last()

        // getting all child-tables (and arrays of tables) that have the same name as we are trying to find
        val simpleTable = this.children.filterIsInstance<TomlTable>().filter {
            it.fullTableKey == tableKey || it.name == tableKeyName
        }
        // there cannot be more than 1 table node with the same name on the same level in the tree
        if (simpleTable.size > 1) {
            throw InternalAstException(
                "While searching a table by name ($tableName), invalid number of tables on the same level of AST were found. " +
                        "Is the tree corrupted?"
            )
        }
        // we need to search this table in special technical nodes (TomlArrayOfTablesElement) that also contain tables
        val tableFromElements = this.children
            .asSequence()
            .filterIsInstance<TomlArrayOfTablesElement>()
            .map { it.children }
            .flatten()
            .filterIsInstance<TomlTable>()
            .filter { it.fullTableKey == tableKey || it.name == tableKeyName }
            .toList()
        // return the table that we found among the list of child tables or in the array of tables
        return simpleTable.lastOrNull() ?: tableFromElements.lastOrNull()
    }

    /**
     * @param tomlTable table that we would like to insert
     * @param latestCreatedBucket the bucket of the latest created array of tables
     * @param insertionType whether the table comes from an explicit header or from a dotted key
     * @param containerDepth number of leading levels that belong to the enclosing section (the
     *   table/inline-table a dotted key was written in); validation is skipped for those because a
     *   dotted key legitimately lives inside its own section
     * @param validate when true (spec-compliant mode), forbidden table/key redefinitions throw;
     *   when false (ktoml's historical lenient default) they are silently merged as before
     * @return link to the inserted table inside the tree
     * @throws ParseException on a forbidden table/key redefinition (only when [validate] is true)
     */
    @Suppress(
        "UNUSED_PARAMETER",
        "TOO_LONG_FUNCTION",
        "TOO_MANY_LINES_IN_LAMBDA",
        "CyclomaticComplexMethod"
    )
    @InternalKtomlApi
    public fun insertTableToTree(
        tomlTable: TomlTable,
        latestCreatedBucket: TomlArrayOfTablesElement? = null,
        insertionType: TableInsertionType = TableInsertionType.HEADER,
        containerDepth: Int = 0,
        validate: Boolean = false
    ): TomlNode {
        var previousParent: TomlNode = this

        tomlTable.tablesList.forEachIndexed { level, subTable ->
            val isLastLevel = level == tomlTable.tablesList.lastIndex
            // levels below the enclosing section are the container that owns this dotted key — they
            // are not subject to redefinition checks (e.g. the `a` in `a = { a.b = 1 }`)
            val isContainerLevel = level < containerDepth
            // a table header (or dotted-key prefix) may not descend through, or land on, a key that
            // was already defined as a non-table value (e.g. `a = 1` then `[a.b]`)
            if (validate && !isContainerLevel) {
                previousParent.checkNoValueConflict(subTable, tomlTable, isLastLevel, insertionType)
            }
            val foundTable = previousParent.findTableInAstByName(subTable)

            previousParent = when {
                foundTable != null && isLastLevel && foundTable.type == tomlTable.type -> {
                    if (validate) {
                        foundTable.checkRedefinitionAllowed(tomlTable, insertionType)
                        foundTable.mergeProvenance(insertionType)
                    }
                    tomlTable.children.forEach { foundTable.appendCheckedKeyValue(it, validate) }
                    foundTable
                }

                // same name already exists but as a different kind of table (e.g. `[tbl]` vs `[[tbl]]`)
                foundTable != null && isLastLevel && validate ->
                    throw ParseException(
                        "Cannot redefine table '$subTable' on line ${tomlTable.lineNo}: it was already " +
                                "defined as a ${foundTable.type} table (line ${foundTable.lineNo})",
                        tomlTable.lineNo
                    )

                foundTable != null -> {
                    if (validate && !isContainerLevel) {
                        foundTable.checkPassThroughAllowed(tomlTable, insertionType)
                    }
                    foundTable.resolveInsertionParent()
                }

                isLastLevel -> {
                    tomlTable.provenance = insertionType.provenanceFor(isLastLevel = true)
                    previousParent.determineParentAndInsertFragmentOfTable(tomlTable)
                    tomlTable
                }

                else -> {
                    val newChildTableName = TomlTable(
                        TomlKey(subTable, lineNo),
                        lineNo,
                        TableType.PRIMITIVE,
                        tomlTable.comments,
                        tomlTable.inlineComment,
                        isSynthetic = true
                    ).apply {
                        provenance = insertionType.provenanceFor(isLastLevel = false)
                    }
                    previousParent.determineParentAndInsertFragmentOfTable(newChildTableName)
                    newChildTableName
                }
            }
        }
        return previousParent
    }

    /**
     * The header/dotted prefix [subTable] may not collide with an existing non-table value
     * (key-value pair) at this level — e.g. `a = 1` then `[a.b]`, or `a = 1` then `a.b = 2`.
     *
     * @param subTable
     * @param tomlTable
     * @param isLastLevel
     * @param insertionType
     * @throws ParseException
     */
    internal fun checkNoValueConflict(
        subTable: String,
        tomlTable: TomlTable,
        isLastLevel: Boolean,
        insertionType: TableInsertionType
    ) {
        val targetName = TomlKey(subTable, lineNo).last()
        val conflictingValue = childrenInThisOrArrayElement()
            .filterIsInstance<TomlKeyValue>()
            .firstOrNull { it.key.last() == targetName }
        conflictingValue?.let {
            val verb = if (isLastLevel && insertionType == TableInsertionType.HEADER) "redefine" else "extend"
            throw ParseException(
                "Cannot $verb key '$targetName' on line ${tomlTable.lineNo} as a table: it was already " +
                        "defined as a value",
                tomlTable.lineNo
            )
        }
    }

    /**
     * Validates an attempt to land a header/dotted key/inline table on an existing table of the
     * same type. Allowed: making an implicit super-table explicit, and an inline table reconciling
     * with the synthetic tables created by its own dotted keys. Forbidden: a second explicit
     * definition of a table, a header/dotted/inline definition over a dotted-key or inline table,
     * and a dotted key appending to an already explicitly-defined (closed) table.
     *
     * @param tomlTable
     * @param insertionType
     * @throws ParseException
     */
    @Suppress("CyclomaticComplexMethod")
    internal fun checkRedefinitionAllowed(tomlTable: TomlTable, insertionType: TableInsertionType) {
        if (this !is TomlTable) {
            return
        }
        // array-of-tables headers legitimately append a new element to an existing array
        if (type == TableType.ARRAY && insertionType == TableInsertionType.HEADER) {
            return
        }
        val allowed = when (insertionType) {
            // an implicit super-table may be made explicit by its first header
            TableInsertionType.HEADER -> provenance == TableProvenance.IMPLICIT_SUPER
            // additional dotted keys may extend a table created implicitly or by previous dotted keys,
            // but not one closed by an explicit header or an inline table
            TableInsertionType.DOTTED_KEY -> provenance == TableProvenance.IMPLICIT_SUPER ||
                    provenance == TableProvenance.DOTTED_KEY
            // an inline table only ever reconciles with the synthetic tables its own dotted keys made
            TableInsertionType.INLINE_TABLE -> provenance == TableProvenance.INLINE_TABLE
        }
        if (allowed) {
            return
        }
        val reason = when (insertionType) {
            TableInsertionType.DOTTED_KEY ->
                "Cannot extend table '$fullTableKey' with dotted keys on line ${tomlTable.lineNo}: " +
                        "it was already defined on line $lineNo"
            else ->
                "Cannot redefine table '$fullTableKey' on line ${tomlTable.lineNo}: it was already " +
                        "defined on line $lineNo"
        }
        throw ParseException(reason, tomlTable.lineNo)
    }

    /**
     * Validates passing *through* an existing table on the way to a deeper one. A dotted key may
     * not descend through a table that was explicitly defined by a header in a different section,
     * and nothing may descend through a closed inline table.
     *
     * @param tomlTable
     * @param insertionType
     * @throws ParseException
     */
    internal fun checkPassThroughAllowed(tomlTable: TomlTable, insertionType: TableInsertionType) {
        if (this !is TomlTable) {
            return
        }
        // an inline table is self-contained: while it is being built, it legitimately descends
        // through the synthetic tables created by its own dotted keys
        if (insertionType == TableInsertionType.INLINE_TABLE) {
            return
        }
        // nothing else may descend through (extend) a closed inline table
        if (provenance == TableProvenance.INLINE_TABLE) {
            throw ParseException(
                "Cannot extend inline table '$fullTableKey' on line ${tomlTable.lineNo}: " +
                        "it was defined on line $lineNo",
                tomlTable.lineNo
            )
        }
        if (insertionType == TableInsertionType.DOTTED_KEY &&
                provenance == TableProvenance.EXPLICIT_HEADER &&
                type == TableType.PRIMITIVE
        ) {
            throw ParseException(
                "Cannot extend table '$fullTableKey' with dotted keys on line ${tomlTable.lineNo}: " +
                        "it was already explicitly defined on line $lineNo",
                tomlTable.lineNo
            )
        }
    }

    /**
     * When a header lands on an existing implicit super-table, promote it to explicit so a later
     * duplicate header is rejected.
     *
     * @param insertionType
     */
    internal fun mergeProvenance(insertionType: TableInsertionType) {
        // promote an implicit super-table to explicit so a later duplicate header is rejected; the
        // node's `isSynthetic` flag is intentionally left untouched so writer output is unchanged
        if (this is TomlTable && insertionType == TableInsertionType.HEADER &&
                provenance == TableProvenance.IMPLICIT_SUPER
        ) {
            provenance = TableProvenance.EXPLICIT_HEADER
        }
    }

    internal fun childrenInThisOrArrayElement(): List<TomlNode> {
        val element = children.filterIsInstance<TomlArrayOfTablesElement>().lastOrNull()
        return element?.children ?: children
    }

    /**
     * @return the number of fully-qualified key-parts of the enclosing table section. A dotted key
     *   written under this node owns these leading levels, so they are excluded from redefinition
     *   validation in [insertTableToTree].
     */
    internal fun sectionDepth(): Int = when (this) {
        is TomlTable -> fullTableKey.keyParts.size
        is TomlArrayOfTablesElement -> (parent as? TomlTable)?.fullTableKey?.keyParts?.size ?: 0
        else -> 0
    }

    /**
     * Appends a key-value pair after checking that its name does not collide with a table (or array
     * of tables) already defined under this node — e.g. `a.b.c = 1` then `a.b = 2`, or an array of
     * tables `[[parent.arr]]` then a `arr = 2` pair in `[parent]`.
     *
     * @param keyValue the key-value node to append
     * @param validate when true (spec-compliant mode) a name collision throws; when false the pair
     *   is appended unconditionally, matching ktoml's historical lenient behavior
     * @throws ParseException on a name collision with an existing table (only when [validate] is true)
     */
    internal fun appendCheckedKeyValue(keyValue: TomlNode, validate: Boolean) {
        keyValue.normalizedKeyName()?.let { keyName ->
            checkKeyCanBeDefined(keyName, keyValue.lineNo, validate)
        }
        appendChild(keyValue)
    }

    internal fun checkKeyCanBeDefined(
        keyName: String,
        keyLineNo: Int,
        validate: Boolean
    ) {
        if (!validate) {
            return
        }

        val duplicateKey = childrenInThisOrArrayElement()
            .firstOrNull { it.normalizedKeyName() == keyName }
        duplicateKey?.let {
            throw ParseException(
                "Cannot define key '$keyName' on line $keyLineNo: it was already defined as a " +
                        "key on line ${duplicateKey.lineNo}",
                keyLineNo
            )
        }

        val conflictingTable = childrenInThisOrArrayElement()
            .filterIsInstance<TomlTable>()
            .firstOrNull { it.name == keyName }
        conflictingTable?.let {
            throw ParseException(
                "Cannot define key '$keyName' on line $keyLineNo: it was already defined as a " +
                        "table on line ${conflictingTable.lineNo}",
                keyLineNo
            )
        }
    }

    private fun TomlNode.normalizedKeyName(): String? = when (this) {
        is TomlKeyValue -> key.last()
        is TomlInlineTable -> key?.last()
        else -> null
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
     * Important: as prettyPrint calls toString() of the node, and not just prints the value, but emits and reconstruct a source string,
     * so in some cases (for example in case of multiline strings) it can work incorrectly.
     *
     * @param emitLine - if true - will print line number in this debug print
     */
    @Suppress("DEBUG_PRINT")
    public fun prettyPrint(emitLine: Boolean = false) {
        val sb = StringBuilder()
        prettyPrint(this, sb, emitLine)
        println(sb.toString())
    }

    /**
     * @param emitLine - if true - will print line number in this debug print
     * @return the string with AST tree visual representation
     */
    public fun prettyStr(emitLine: Boolean = false): String {
        val sb = StringBuilder()
        prettyPrint(this, sb, emitLine)
        return sb.toString()
    }

    /**
     * This method returns all available table names that can be found in this particular TOML file
     * (!) it will also return synthetic table nodes, that we generated to create a normal tree structure
     *
     * @return all detected toml tables
     */
    public fun getAllChildTomlTables(): List<TomlTable> {
        val result = if (this is TomlTable && type == TableType.PRIMITIVE) mutableListOf(this) else mutableListOf()
        return result + this.children.flatMap {
            it.getAllChildTomlTables()
        }
    }

    /**
     * find only real table nodes without synthetics
     *
     * @return all real table nodes
     */
    public fun getRealTomlTables(): List<TomlTable> =
        this.getAllChildTomlTables().filter { !it.isSynthetic }

    private fun determineParentAndInsertFragmentOfTable(childTable: TomlTable) {
        if (this.children.filterIsInstance<TomlArrayOfTablesElement>().isNotEmpty()) {
            this.children.last().appendChild(childTable)
        } else {
            this.appendChild(childTable)
        }
    }

    private fun TomlTable.resolveInsertionParent(): TomlNode =
        if (type == TableType.ARRAY) {
            children.lastOrNull() as? TomlArrayOfTablesElement ?: this
        } else {
            this
        }

    /**
     * Writes this node as text to [emitter].
     *
     * @param emitter The [TomlEmitter] instance to write to.
     * @param config The [TomlConfig] instance. Defaults to the node's config.
     */
    public abstract fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig = TomlOutputConfig()
    )

    protected open fun TomlEmitter.writeChildren(
        children: List<TomlNode>,
        config: TomlOutputConfig
    ) {
        val last = children.lastIndex

        children.forEachIndexed { i, child ->
            writeChildComments(child)

            if (child !is TomlTable) {
                emitIndent()
            }

            child.write(emitter = this, config)

            if (child is TomlKeyValue || child is TomlInlineTable) {
                writeChildInlineComment(child)
            }

            if (i < last) {
                emitNewLine()

                // A single newline follows single-line pairs, except when a table
                // follows. Two newlines follow multi-line pairs.
                if ((child is TomlKeyValueArray && child.isMultiline()) || children[i + 1] is TomlTable) {
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

    // Todo: Do we keep whitespace in pairs and change parser tests? Trim it and
    // maintain compatibility? Add a "formatting" option later?
    override fun toString(): String =
        Toml.tomlWriter
            .writeNode(this)
            .replace(" = ", "=")

    internal fun print(emitLine: Boolean = false): String =
        "${this::class.simpleName} ($this)${if (emitLine) "[line:${this.lineNo}]" else ""}\n"

    public companion object {
        // number of spaces that is used to indent levels
        internal const val INDENTING_LEVEL = 4

        /**
         * recursive print the tree using the current node
         *
         * @param node that will be printed
         * @param level depth of hierarchy for print
         * @param result string builder where the result is stored
         * @param emitLine if true - will print line number in this debug print
         */
        public fun prettyPrint(
            node: TomlNode,
            result: StringBuilder,
            emitLine: Boolean = false,
            level: Int = 0
        ) {
            val spaces = " ".repeat(INDENTING_LEVEL * level)
            // we are using print() method here instead of toString()
            result.append("$spaces - ${node.print(emitLine)}")
            node.children.forEach { child ->
                prettyPrint(child, result, emitLine, level + 1)
            }
        }
    }
}

/**
 * Why a table is being inserted into the AST. Drives redefinition validation in
 * [TomlNode.insertTableToTree].
 *
 * @property HEADER an explicit `[table]` or `[[array]]` header line
 * @property DOTTED_KEY a synthetic table created for a dotted key (e.g. `a.b.c = 1`)
 * @property INLINE_TABLE a table created from an inline-table value (`a = { b = 1 }`), including the
 *   synthetic tables for any dotted keys written inside it
 */
@InternalKtomlApi
public enum class TableInsertionType {
    DOTTED_KEY,
    HEADER,
    INLINE_TABLE,
    ;

    /**
     * @param isLastLevel whether this is the target table of the insertion (vs an intermediate
     *   super-table)
     * @return the [TableProvenance] to stamp on a newly-created table node for this insertion
     */
    internal fun provenanceFor(isLastLevel: Boolean): TableProvenance = when (this) {
        HEADER -> if (isLastLevel) TableProvenance.EXPLICIT_HEADER else TableProvenance.IMPLICIT_SUPER
        DOTTED_KEY -> TableProvenance.DOTTED_KEY
        INLINE_TABLE -> TableProvenance.INLINE_TABLE
    }
}
