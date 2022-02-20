/**
 * Array of tables https://toml.io/en/v1.0.0#array-of-tables
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.findBeginningOfTheComment
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimDoubleBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes

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

        // finding the index of the beginning of the comment (if any)
        val firstHash = content.findBeginningOfTheComment(lastIndexOfBrace)

        // getting the content inside brackets ([a.b] -> a.b)
        val sectionFromContent = content.substring(0, firstHash).trim().trimDoubleBrackets()
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
}

/**
 * This class is used to store elements of array of tables (bucket for key-value records)
 */
public class TomlArrayOfTablesElement(lineNo: Int, config: TomlConfig = TomlConfig()) : TomlNode(
    EMPTY_TECHNICAL_NODE,
    lineNo,
    config
) {
    override val name: String = EMPTY_TECHNICAL_NODE
}
