package com.akuleshov7.ktoml.annotations

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlin.annotation.AnnotationTarget.*

/**
 * Marks a TOML string element as literal. Has no effect on deserialization.
 *
 * ```kotlin
 * @Serializable
 * data class Data(
 *     @TomlLiteral
 *     val text: String,
 *     val list: List<@TomlLiteral String>
 * )
 *
 * val data = Data(
 *     text = "\"literal\"",
 *     list = listOf("\\[a-z]*\\")
 * )
 * ```
 *
 * would produce:
 *
 * ```toml
 * text = '"literal"'
 * list = [ '\[a-z]*\' ]
 * ```
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(PROPERTY, TYPE_PARAMETER, TYPE)
public annotation class TomlLiteral
