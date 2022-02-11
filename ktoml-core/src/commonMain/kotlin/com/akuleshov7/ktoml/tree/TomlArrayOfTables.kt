/**
 * Array of tables https://toml.io/en/v1.0.0#array-of-tables
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimDoubleBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes

/**
 * @property isSynthetic
 */
// FixMe: this class is mostly identical to the TomlTable - we should unify them together
public class TomlArrayOfTables(
    content: String,
    lineNo: Int,
    config: TomlConfig = TomlConfig(),
    public val isSynthetic: Boolean = false
) : TomlTable(
    content,
    lineNo,
    config
) {
    public override val type: TableType = TableType.ARRAY

    // short table name (only the name without parental prefix, like a - it is used in decoder and encoder)
    override val name: String

    // list of tables (including sub-tables) that are included in this table  (e.g.: {a, a.b, a.b.c} in a.b.c)
    public override lateinit var tablesList: List<String>

    // full name of the table (like a.b.c.d)
    public override lateinit var fullTableName: String

    init {
        // getting the content inside brackets ([a.b] -> a.b)
        val sectionFromContent = content.trim().trimDoubleBrackets().trim()

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
