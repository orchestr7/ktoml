package com.akuleshov7.ktoml.tree.nodes.tables

/**
 * Type of inline table: primitive = "table = { a = 5, b = 6 }" or array: "table_arr = [ { a = 5 }, { a = 6 } ]"
 *
 */
public enum class InlineTableType {
    ARRAY,
    PRIMITIVE,
    ;
}
