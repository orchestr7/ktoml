/**
 * Array of tables https://toml.io/en/v1.0.0#array-of-tables
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.takeBeforeComment
import com.akuleshov7.ktoml.parsers.trimDoubleBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes
import com.akuleshov7.ktoml.writers.TomlEmitter

/**
 * Class representing array of tables
 *
 * @throws ParseException if the content is wrong
 */
// FixMe: this class is mostly identical to the TomlTable - we should unify them together
public class TomlArrayOfTables(
    content: String,
    lineNo: Int,
    config: TomlConfig = TomlConfig(),
    isSynthetic: Boolean = false
) : TomlTable(
    content,
    lineNo,
    comments = emptyList(),
    inlineComment = "",
    config,
    isSynthetic
) {
    public override val type: TableType = TableType.ARRAY

    // short table name (only the name without parental prefix, like a - it is used in decoder and encoder)
    override val name: String

    // list of tables (including sub-tables) that are included in this table  (e.g.: {a, a.b, a.b.c} in a.b.c)
    public override lateinit var tablesList: List<String>

    // full name of the table (like a.b.c.d)
    public override lateinit var fullTableName: String

    init {
        val lastIndexOfBrace = content.lastIndexOf("]]")
        if (lastIndexOfBrace == -1) {
            throw ParseException("Invalid Array of Tables provided: $content." +
                    " It has missing closing brackets: ']]'", lineNo)
        }

        // getting the content inside brackets ([a.b] -> a.b)
        val sectionFromContent = content.takeBeforeComment(lastIndexOfBrace).trimDoubleBrackets()
            .trim()

        if (sectionFromContent.isBlank()) {
            throw ParseException("Incorrect blank name for array of tables: $content", lineNo)
        }

        fullTableName = sectionFromContent

        val sectionsList = sectionFromContent.splitKeyToTokens(lineNo)
        name = sectionsList.last().trimQuotes()
        tablesList = sectionsList.mapIndexed { index, _ ->
            (0..index).joinToString(".") { sectionsList[it] }
        }
    }

    override fun TomlEmitter.writeHeader(headerKey: TomlKey, config: TomlConfig) {
        startTableArrayHeader()

        headerKey.write(emitter = this, config)

        endTableArrayHeader()
    }

    override fun TomlEmitter.writeChildren(
        headerKey: TomlKey,
        children: List<TomlNode>,
        config: TomlConfig,
        multiline: Boolean
    ) {
        val last = children.lastIndex

        children.forEachIndexed { i, child ->
            if (child is TomlArrayOfTablesElement) {
                if (parent !is TomlArrayOfTablesElement) {
                    emitIndent()
                }

                writeChildComments(child)
                writeHeader(headerKey, config)
                writeChildInlineComment(child)

                if (!child.hasNoChildren()) {
                    emitNewLine()
                }

                indent()

                child.write(emitter = this, config, multiline)

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
                child.write(emitter = this, config, multiline)
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
    inlineComment: String,
    config: TomlConfig = TomlConfig()
) : TomlNode(
    EMPTY_TECHNICAL_NODE,
    lineNo,
    comments,
    inlineComment,
    config
) {
    override val name: String = EMPTY_TECHNICAL_NODE

    override fun write(
        emitter: TomlEmitter,
        config: TomlConfig,
        multiline: Boolean
    ): Unit = emitter.writeChildren(children, config, multiline)
}
