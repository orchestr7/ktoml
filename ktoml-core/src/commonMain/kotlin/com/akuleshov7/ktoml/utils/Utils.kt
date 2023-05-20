/**
 * File with a utilities toolkit
 */

package com.akuleshov7.ktoml.utils

import com.akuleshov7.ktoml.tree.nodes.TableType
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import com.akuleshov7.ktoml.tree.nodes.TomlTable

/**
 * Append a code point to a [StringBuilder]
 *
 * @param codePoint code point
 * @return [StringBuilder] with appended code point
 */
@Throws(IllegalArgumentException::class)
internal expect fun StringBuilder.appendCodePointCompat(codePoint: Int): StringBuilder

/**
 * searching (BFS) the table with the [fullTableName]
 *
 * @param children list of nodes
 * @param fullTableName string with a table name
 */
public fun findPrimitiveTableInAstByName(children: List<TomlNode>, fullTableName: String): TomlTable? {
    if (children.isEmpty()) {
        return null
    }
    children.forEach {
        if (it is TomlTable && it.type == TableType.PRIMITIVE && it.fullTableKey.toString() == fullTableName) {
            return it
        }
    }

    return findPrimitiveTableInAstByName(children.map { it.children }.flatten(), fullTableName)
}
