/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.parsers.*
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * tablesList - a list of names of sections (tables) that are included into this particular TomlTable
 * for example: if the TomlTable is [a.b.c] this list will contain [a], [a.b], [a.b.c]
 * @property isSynthetic - flag to determine that this node was synthetically and there is no such table in the input
 */
public class TomlTablePrimitive(
    fullTableKey: TomlKey,
    lineNo: Int,
    comments: List<String>,
    inlineComment: String,
    isSynthetic: Boolean = false
) : TomlTable(
    fullTableKey,
    lineNo,
    comments,
    inlineComment,
    isSynthetic
) {
    public override val type: TableType = TableType.PRIMITIVE

    public constructor(
        content: String,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        isSynthetic: Boolean = false
    ) : this(
        parseSection(content, lineNo, isArray = false),
        lineNo,
        comments,
        inlineComment,
        isSynthetic
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        content: String,
        lineNo: Int,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        config: TomlConfig,
        isSynthetic: Boolean = false
    ) : this(
        content,
        lineNo,
        comments,
        inlineComment,
        isSynthetic
    )

    override fun TomlEmitter.writeHeader(
        config: TomlOutputConfig
    ) {
        startTableHeader()

        fullTableKey.write(emitter = this)

        endTableHeader()
    }

    override fun TomlEmitter.writeChildren(
        children: List<TomlNode>,
        config: TomlOutputConfig,
        multiline: Boolean
    ) {
        if (children.first() is TomlStubEmptyNode) {
            return
        }

        var prevChild: TomlNode? = null

        children.forEachIndexed { i, child ->
            writeChildComments(child)

            // Declare the super table after a nested table, to avoid a pair being
            // a part of the previous table by mistake.
            if ((child is TomlKeyValue || child is TomlInlineTable) && prevChild is TomlTable) {
                dedent()

                emitNewLine()
                emitIndent()
                writeHeader(config)
                emitNewLine()

                indent()
                indent()
            }

            when (child) {
                is TomlTablePrimitive ->
                    if (!child.isSynthetic && child.getFirstChild() !is TomlTable) {
                        emitIndent()
                    }
                is TomlArrayOfTables -> { }
                else -> emitIndent()
            }

            child.write(emitter = this, config, multiline)
            writeChildInlineComment(child)

            if (i < children.lastIndex) {
                emitNewLine()
                // A single newline follows single-line pairs, except when a table
                // follows. Two newlines follow multi-line pairs.
                if ((child is TomlKeyValue && multiline) || children[i + 1] is TomlTable) {
                    emitNewLine()
                }
            }

            prevChild = child
        }
    }

    override fun toString(): String = "[$fullTableKey]"
}
