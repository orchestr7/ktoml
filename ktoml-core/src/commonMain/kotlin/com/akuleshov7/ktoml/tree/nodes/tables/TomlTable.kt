/**
 * Common class for tables
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.takeBeforeComment
import com.akuleshov7.ktoml.parsers.trimBrackets
import com.akuleshov7.ktoml.parsers.trimDoubleBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.writers.TomlEmitter
import com.akuleshov7.ktoml.writers.TomlStringEmitter
import kotlin.jvm.JvmStatic

/**
 * Class to represent all types of tables: primitive/arrays/etc.
 *
 * @param comments
 * @param inlineComment
 * @property fullTableKey
 * @property lineNo - line number
 * @property type The table type
 * @property isSynthetic flag to determine that this node was synthetically and
 * there is no such table in the input
 */
@Suppress("COMMENT_WHITE_SPACE")
public class TomlTable(
    public var fullTableKey: TomlKey,
    override val lineNo: Int,
    public val type: TableType,
    comments: List<String> = emptyList(),
    inlineComment: String = "",
    public var isSynthetic: Boolean = false
) : TomlNode(
    lineNo,
    comments,
    inlineComment
) {
    @Deprecated(
        message = "fullTableName was replaced with fullTableKey; will be removed in future releases.",
        replaceWith = ReplaceWith("fullTableKey.toString()")
    )
    @Suppress("NO_CORRESPONDING_PROPERTY", "CUSTOM_GETTERS_SETTERS")
    public var fullTableName: String
        get() = fullTableKey.toString()
        set(value) {
            fullTableKey = TomlKey(value, lineNo)
        }

    // list of tables (including sub-tables) that are included in this table  (e.g.: {a, a.b, a.b.c} in a.b.c)
    public var tablesList: List<String> = fullTableKey.keyParts.runningReduce { prev, cur -> "$prev.$cur" }

    // short table name (only the name without parental prefix, like a - it is used in decoder and encoder)
    public override val name: String = fullTableKey.keyParts.last().trimQuotes()

    public constructor(
        content: String,
        lineNo: Int,
        type: TableType,
        comments: List<String> = emptyList(),
        inlineComment: String = "",
        isSynthetic: Boolean = false
    ) : this(
        parseSection(content, lineNo, type),
        lineNo,
        type,
        comments,
        inlineComment,
        isSynthetic
    )

    override fun write(
        emitter: TomlEmitter,
        config: TomlOutputConfig
    ) {
        // Todo: Option to explicitly define super tables?
        // Todo: Support dotted key-value pairs (i.e. a.b.c.d = 7)

        val firstChild = children.firstOrNull() ?: return

        if (isExplicit(firstChild) && type == TableType.PRIMITIVE) {
            emitter.writeHeader()

            if (inlineComment.isNotEmpty()) {
                emitter.emitComment(inlineComment, inline = true)
            }

            if (firstChild !is TomlStubEmptyNode) {
                emitter.emitNewLine()
            }

            emitter.indent()

            emitter.writeChildren(children, config)

            emitter.dedent()
        } else {
            emitter.writeChildren(children, config)
        }
    }

    private fun TomlEmitter.writeHeader() {
        startTableHeader(type)
        fullTableKey.write(emitter = this)
        endTableHeader(type)
    }

    private fun TomlEmitter.writeArrayChild(
        index: Int,
        child: TomlNode,
        children: List<TomlNode>,
        config: TomlOutputConfig
    ) {
        if (child is TomlArrayOfTablesElement) {
            if (parent !is TomlArrayOfTablesElement) {
                emitIndent()
            }

            writeChildComments(child)
            writeHeader()
            writeChildInlineComment(child)

            if (!child.hasNoChildren()) {
                emitNewLine()
            }

            indent()

            child.write(emitter = this, config)

            dedent()

            if (index < children.lastIndex) {
                emitNewLine()

                // Primitive pairs have a single newline after, except when a
                // table follows.
                if (child !is TomlKeyValuePrimitive || children[index + 1] is TomlTable) {
                    emitNewLine()
                }
            }
        } else {
            child.write(emitter = this, config)
        }
    }

    private fun TomlEmitter.writePrimitiveChild(
        index: Int,
        prevChild: TomlNode?,
        child: TomlNode,
        children: List<TomlNode>,
        config: TomlOutputConfig
    ) {
        writeChildComments(child)

        // Declare the super table after a nested table, to avoid a pair being a
        // part of the previous table by mistake.
        if ((child is TomlKeyValue || child is TomlInlineTable) && prevChild is TomlTable) {
            dedent()

            emitNewLine()
            emitIndent()
            writeHeader()
            emitNewLine()

            indent()
            indent()
        }

        if (child !is TomlStubEmptyNode) {
            if (child !is TomlTable ||
                (child.type == TableType.PRIMITIVE &&
                        !child.isSynthetic &&
                        child.getFirstChild() !is TomlTable)
            ) {
                emitIndent()
            }
        }

        child.write(emitter = this, config)
        writeChildInlineComment(child)

        if (index < children.lastIndex) {
            emitNewLine()
            // A single newline follows single-line pairs, except when a table
            // follows. Two newlines follow multi-line pairs.
            if ((child is TomlKeyValue && child.isMultiline()) || children[index + 1] is TomlTable) {
                emitNewLine()
            }
        }
    }

    override fun TomlEmitter.writeChildren(
        children: List<TomlNode>,
        config: TomlOutputConfig
    ) {
        when (type) {
            TableType.ARRAY ->
                children.forEachIndexed { i, child ->
                    writeArrayChild(i, child, children, config)
                }

            TableType.PRIMITIVE -> {
                // "children.count() == 1" condition relies on the behavior of the AST result
                if (children.count() == 1 && children.first() is TomlStubEmptyNode) {
                    return
                }

                var prevChild: TomlNode? = null

                children.forEachIndexed { i, child ->
                    writePrimitiveChild(i, prevChild, child, children, config)
                    prevChild = child
                }
            }
        }
    }

    /**
     * Determines whether the table should be explicitly defined. Synthetic tables
     * are implicit except when:
     *  - the first child is a pair, and there are other children (to exclude
     *    dotted pairs)
     *  - the table is empty
     */
    private fun isExplicit(
        firstChild: TomlNode
    ) = if (!isSynthetic) {
        true
    } else {
        when (firstChild) {
            is TomlStubEmptyNode -> true
            is TomlKeyValue,
            is TomlInlineTable -> children.size > 1
            else -> false
        }
    }

    override fun toString(): String = buildString {
        val config = TomlOutputConfig()
        val emitter = TomlStringEmitter(this, config)

        emitter.writeHeader()
    }

    public companion object {
        @JvmStatic
        private fun parseSection(
            content: String,
            lineNo: Int,
            type: TableType
        ): TomlKey {
            val lastIndexOfBrace = content.lastIndexOf(type.close)
            if (lastIndexOfBrace == -1) {
                throw ParseException(
                    "Invalid Tables provided: $content." +
                            " It has missing closing bracket${
                                if (type == TableType.ARRAY) "s" else ""
                            }: '${type.close}'", lineNo
                )
            }
            val sectionFromContent = content.takeBeforeComment(false)
                .trim()
                .let {
                    if (type == TableType.ARRAY) {
                        it.trimDoubleBrackets()
                    } else {
                        it.trimBrackets()
                    }
                }
                .trim()

            if (sectionFromContent.isBlank()) {
                throw ParseException("Incorrect blank name for ${
                    if (type == TableType.ARRAY) "array of tables" else "table"
                }: $content", lineNo)
            }

            return TomlKey(sectionFromContent, lineNo)
        }
    }
}

/**
 * Special Enum that is used in a logic related to insertion of tables to AST
 *
 * @property open The header opening sequence.
 * @property close The header closing sequence.
 */
public enum class TableType(
    internal val open: String,
    internal val close: String
) {
    ARRAY("[[", "]]"),
    PRIMITIVE("[", "]"),
    ;
}
