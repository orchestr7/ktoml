package com.akuleshov7.ktoml.tree

import com.akuleshov7.ktoml.TomlConfig
import com.akuleshov7.ktoml.exceptions.ParseException

/**
 * Interface that contains all common methods that are used in KeyValue nodes
 */
public abstract class TomlTable (
    override val content: String,
    override val lineNo: Int,
    override val config: TomlConfig = TomlConfig()
): TomlNode(content, lineNo, config) {
    abstract public var fullTableName: String
    abstract public var tablesList: List<String>
    abstract public val type: TableType
}

public enum class TableType {
    ARRAY,
    PRIMITIVE
}
