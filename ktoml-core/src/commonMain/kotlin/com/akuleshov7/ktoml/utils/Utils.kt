/**
 * File with a utilities toolkit
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.tree.TomlNode
import com.akuleshov7.ktoml.tree.TomlTablePrimitive

/**
 * searching (BFS) the table with the [fullTableName]
 *
 * @param children list of nodes
 * @param fullTableName string with a table name
 */
public fun findPrimitiveTableInAstByName(children: List<TomlNode>, fullTableName: String): TomlTablePrimitive? {
    if (children.isEmpty()) {
        return null
    }
    children.forEach {
        if (it is TomlTablePrimitive && it.fullTableName == fullTableName) {
            return it
        }
    }

    return findPrimitiveTableInAstByName(children.map { it.children }.flatten(), fullTableName)
}
