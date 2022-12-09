/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree.nodes

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.TomlInputConfig
import com.akuleshov7.ktoml.TomlOutputConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.*
import com.akuleshov7.ktoml.tree.nodes.pairs.keys.TomlKey
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * tablesList - a list of names of sections (tables) that are included into this particular TomlTable
 * for example: if the TomlTable is [a.b.c] this list will contain [a], [a.b], [a.b.c]
 * @property isSynthetic - flag to determine that this node was synthetically and there is no such table in the input
 */
@Suppress("MULTIPLE_INIT_BLOCKS")
public class TomlTablePrimitive(
    content: String,
    lineNo: Int,
    comments: List<String> = emptyList(),
    inlineComment: String = "",
    config: TomlInputConfig = TomlInputConfig(),
    isSynthetic: Boolean = false
) : TomlTable(
    content,
    lineNo,
    comments,
    inlineComment,
    config,
    isSynthetic
) {
    public override val type: TableType = TableType.PRIMITIVE

    // short table name (only the name without parental prefix, like a - it is used in decoder and encoder)
    override val name: String

    // list of tables (including sub-tables) that are included in this table  (e.g.: {a, a.b, a.b.c} in a.b.c)
    public override lateinit var tablesList: List<String>

    // full name of the table (like a.b.c.d)
    public override lateinit var fullTableName: String

    init {
        val lastIndexOfBrace = content.lastIndexOf("]")
        if (lastIndexOfBrace == -1) {
            throw ParseException("Invalid Tables provided: $content." +
                    " It has missing closing bracket: ']'", lineNo)
        }
        // getting the content inside brackets ([a.b] -> a.b)
        val sectionFromContent = content
            .takeBeforeComment(config.allowEscapedQuotesInLiteralStrings)
            .trimBrackets()
            .trim()

        if (sectionFromContent.isBlank()) {
            throw ParseException("Incorrect blank table name: $content", lineNo)
        }

        fullTableName = sectionFromContent

        val sectionsList = sectionFromContent.splitKeyToTokens(lineNo)
        name = sectionsList.last().trimQuotes()
        tablesList = sectionsList.mapIndexed { index, _ ->
            (0..index).joinToString(".") { sectionsList[it] }
        }
    }

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
        config.input,
        isSynthetic
    )

    override fun TomlEmitter.writeHeader(
        headerKey: TomlKey,
        config: TomlOutputConfig
    ) {
        startTableHeader()

        headerKey.write(emitter = this)

        endTableHeader()
    }

    override fun TomlEmitter.writeChildren(
        headerKey: TomlKey,
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
                writeHeader(headerKey, config)
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
}
