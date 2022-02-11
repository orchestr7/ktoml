/**
 * Common class for tables
 */

package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig

/**
 * Abstract class to represent all types of tables: primitive/arrays/etc.
 * @property content - raw string name of the table
 * @property lineNo - line number
 * @property config - toml configuration
 */
public abstract class TomlTable(
    override val content: String,
    override val lineNo: Int,
    override val config: TomlConfig = TomlConfig()
) : TomlNode(
    content,
    lineNo,
    config
) {
    public abstract var fullTableName: String
    public abstract var tablesList: List<String>
    public abstract val type: TableType
}

/**
 * Special Enum that is used in a logic related to insertion of tables to AST
 */
public enum class TableType {
    ARRAY,
    PRIMITIVE,
    ;
}
