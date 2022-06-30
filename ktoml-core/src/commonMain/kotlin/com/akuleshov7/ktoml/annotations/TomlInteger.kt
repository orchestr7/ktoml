package com.akuleshov7.ktoml.annotations

import com.akuleshov7.ktoml.writers.IntegerRepresentation
import com.akuleshov7.ktoml.writers.IntegerRepresentation.DECIMAL
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo
import kotlin.annotation.AnnotationTarget.*

/**
 * Specifies how a TOML integer element is encoded. Has no effect on deserialization.
 *
 * ```kotlin
 * @Serializable
 * data class Data(
 *     @TomlInteger(HEX)
 *     val mask: Int,
 *     val perms: List<@TomlInteger(OCTAL) Int>
 * )
 *
 * val data = Data(
 *     mask = 0x00FF,
 *     perms = listOf(0x1FF, 0x1ED, 0x1A4)
 * )
 * ```
 *
 * would produce:
 *
 * ```toml
 * mask = 0x00FF
 * perms = [ 0o777, 0o755, 0o644 ]
 * ```
 *
 * @property representation How the integer is represented in TOML. The default
 * behavior is [DECIMAL].
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(PROPERTY, TYPE_PARAMETER, TYPE)
public annotation class TomlInteger(
    val representation: IntegerRepresentation = DECIMAL
)
