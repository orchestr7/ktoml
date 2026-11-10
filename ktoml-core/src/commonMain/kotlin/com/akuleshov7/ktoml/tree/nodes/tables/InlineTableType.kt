package com.akuleshov7.ktoml.tree.nodes.tables

import com.akuleshov7.ktoml.annotations.InternalKtomlApi

/**
 * Type of inline table: primitive = "table = { a = 5, b = 6 }" or array: "table_arr = [ { a = 5 }, { a = 6 } ]"
 *
 */
@InternalKtomlApi
public enum class InlineTableType {
    ARRAY,
    PRIMITIVE,
    ;
}
