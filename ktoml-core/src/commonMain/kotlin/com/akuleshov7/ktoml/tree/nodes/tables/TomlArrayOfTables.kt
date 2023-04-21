/**
 * Array of tables https://toml.io/en/v1.0.0#array-of-tables
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class representing array of tables
 *
 * @throws ParseException if the content is wrong
 */
// FixMe: this class is mostly identical to the TomlTable - we should unify them together
public class TomlArrayOfTables(
    fullTableKey: TomlKey,
    lineNo: Int,
    isSynthetic: Boolean = false
) : TomlTable(
    fullTableKey,
    lineNo,
    comments = emptyList(),
    inlineComment = "",
    isSynthetic
) {
    public override val type: TableType = TableType.ARRAY

    public constructor(
        content: String,
        lineNo: Int,
        isSynthetic: Boolean = false
    ) : this(
        parseSection(content, lineNo, isArray = true),
        lineNo,
        isSynthetic
    )

    @Deprecated(
        message = "TomlConfig is deprecated; use TomlInputConfig instead. Will be removed in next releases."
    )
    public constructor(
        content: String,
        lineNo: Int,
        config: TomlConfig,
        isSynthetic: Boolean = false
    ) : this(
        content,
        lineNo,
        isSynthetic
    )

    override fun TomlEmitter.writeHeader(config: TomlOutputConfig) {
        startTableArrayHeader()

        fullTableKey.write(emitter = this)

        endTableArrayHeader()
    }

    override fun TomlEmitter.writeChildren(
        children: List<TomlNode>,
        config: TomlOutputConfig
    ) {
        val last = children.lastIndex

        children.forEachIndexed { i, child ->
            if (child is TomlArrayOfTablesElement) {
                if (parent !is TomlArrayOfTablesElement) {
                    emitIndent()
                }

                writeChildComments(child)
                writeHeader(config)
                writeChildInlineComment(child)

                if (!child.hasNoChildren()) {
                    emitNewLine()
                }

                indent()

                child.write(emitter = this, config)

                dedent()

                if (i < last) {
                    emitNewLine()

                    // Primitive pairs have a single newline after, except when a
                    // table follows.
                    if (child !is TomlKeyValuePrimitive || children[i + 1] is TomlTable) {
                        emitNewLine()
                    }
                }
            } else {
                child.write(emitter = this, config)
            }
        }
    }
}

/**
 * This class is used to store elements of array of tables (bucket for key-value records)
 */
public class TomlArrayOfTablesElement(
    lineNo: Int,
    comments: List<String>,
    inlineComment: String
) : TomlNode(
    lineNo,
    comments,
    inlineComment
) {
    override val name: String = EMPTY_TECHNICAL_NODE

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ): Unit = emitter.writeChildren(children, config)

    override fun toString(): String = EMPTY_TECHNICAL_NODE
}
