package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException
import com.akuleshov7.ktoml.parsers.splitKeyToTokens
import com.akuleshov7.ktoml.parsers.trimDoubleBrackets
import com.akuleshov7.ktoml.parsers.trimQuotes

// FixMe: this class is mostly identical to the TomlTable - we should unify them together
public class TomlArrayOfTables(
    content: String,
    lineNo: Int,
    config: TomlConfig = TomlConfig(),
    public val isSynthetic: Boolean = false
) : TomlTable(content, lineNo, config) {
    public override val type: TableType = TableType.ARRAY

    // list of tables (including sub-tables) that are included in this table  (e.g.: {a, a.b, a.b.c} in a.b.c)
    public override lateinit var tablesList: List<String>

    // full name of the table (like a.b.c.d)
    public override lateinit var fullTableName: String

    // short table name (only the name without parental prefix, like a - it is used in decoder and encoder)
    override val name: String

    internal val keyValues: MutableList<MutableList<TomlKeyValue>> = mutableListOf()

    internal fun insertKeyValue(keyValue: TomlKeyValue, isNewElementInArray: Boolean) {
        if (isNewElementInArray) {
            // creating a new bucket for the array
            keyValues.add(mutableListOf(keyValue))
        } else {
            // adding new keyValue to the last bucket (it should have been created on the previous step)
           keyValues[keyValues.lastIndex].add(keyValue)
        }
    }

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
