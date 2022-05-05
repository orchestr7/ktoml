package com.akuleshov7.ktoml.annotations

import kotlin.annotation.AnnotationTarget.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Marks a TOML element as an inline table. Has no effect on deserialization.
 *
 * ```kotlin
 * @Serializable
 * data class Data(
 *     @TomlInlineTable
 *     val inlineTable: Table,
 *     val tableArray: List<@TomlInlineTable Table>,
 *     val inlineTable2: Table2
 * )
 *
 * @Serializable
 * data class Table(val int: Int)
 *
 * @Serializable
 * @TomlInlineTable
 * data class Table2(val string: String)
 *
 * val data = Data(
 *     inlineTable = Table(int = -1),
 *     tableArray = listOf(
 *         Table(int = 3),
 *         Table(int = 10)
 *     ),
 *     inlineTable2 = Table2(string = "text")
 * )
 * ```
 *
 * would produce:
 *
 * ```toml
 * inlineTable = { int = -1 }
 * tableArray = [ { int = 3 }, { int = 10 } ]
 * inlineTable2 = { string = "text" }
 * ```
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(
    PROPERTY,
    TYPE_PARAMETER,
    CLASS
)
public annotation class TomlInlineTable
