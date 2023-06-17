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

/**
 * Unfortunately Levenshtein method is implemented in jline and not ported to Kotlin Native.
 * So we need to implement it (inspired by: https://pl.kotl.in/ifo0z0vMC)
 */
public fun levenshteinDistance(first: String, second: String): Int {
    when {
        first == second -> return 0
        first.isEmpty() -> return second.length
        second.isEmpty() -> return first.length
    }

    val firstLen = first.length + 1
    val secondLen = second.length + 1
    var distance = IntArray(firstLen) { it }
    var newDistance = IntArray(firstLen) { 0 }

    for (i in 1 until secondLen) {
        newDistance[0] = i
        for (j in 1 until firstLen) {
            val costReplace = distance[j - 1] + (if (first[j - 1] == second[i - 1]) 0 else 1)
            val costInsert = distance[j] + 1
            val costDelete = newDistance[j - 1] + 1

            newDistance[j] = minOf(costInsert, costDelete, costReplace)
        }
        distance = newDistance.also { newDistance = distance }
    }
    return distance[firstLen - 1]
}

public fun Iterable<String>.closestEnumName(enumValue: String): String? =
    this.minByOrNull { levenshteinDistance(it, enumValue) }
