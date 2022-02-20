/**
 * File contains all classes used in Toml AST node
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.findBeginningOfTheComment
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes

/**
 * tablesList - a list of names of sections (tables) that are included into this particular TomlTable
 * for example: if the TomlTable is [a.b.c] this list will contain [a], [a.b], [a.b.c]
 * @property isSynthetic - flag to determine that this node was synthetically and there is no such table in the input
 */
@Suppress("MULTIPLE_INIT_BLOCKS")
public class TomlTablePrimitive(
    content: String,
    lineNo: Int,
    config: TomlConfig = TomlConfig(),
    isSynthetic: Boolean = false
) : TomlTable(
    content,
    lineNo,
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

        // finding the index of the beginning of the comment (if any)
        val firstHash = content.findBeginningOfTheComment(lastIndexOfBrace)

        // getting the content inside brackets ([a.b] -> a.b)
        val sectionFromContent = content.substring(0, firstHash).trim().trimBrackets()
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
}
