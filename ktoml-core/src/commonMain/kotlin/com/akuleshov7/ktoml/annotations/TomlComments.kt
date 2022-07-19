package com.akuleshov7.ktoml.annotations

import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

// FixMe: Default parameters after varargs are broken as of 1.7: KT-53235. There's
//  no obvious workaround

/**
 * Specifies comments to be applied the TOML element produced by a property during
 * serialization. Has no effect on deserialization.
 *
 * ```kotlin
 * @Serializable
 * data class Data(
 *     @TomlComments(inline = "Inline")
 *     val x: Int,
 *     @TomlComments(
 *         "Descriptive comment 1",
 *         "Descriptive comment 2"
 *     )
 *     val y: Int
 * )
 *
 * val data = Data(x = 3, y = 7)
 * ```
 *
 * would produce:
 *
 * ```toml
 * x = 3 # Inline
 *
 * # Descriptive comment 1
 * # Descriptive comment 2
 * y = 7
 * ```
 *
 * @property lines Comment lines to be placed before the TOML element.
 * @property inline A comment placed *inline* with the TOML element, at the end
 * of the line. If empty (the default), no comment will be written.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(PROPERTY)
public annotation class TomlComments(
    vararg val lines: String,
    val inline: String = ""
)
