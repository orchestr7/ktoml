package com.akuleshov7.ktoml.annotations

import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.TYPE_PARAMETER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * Marks a TOML element and its children as multiline. Can be used on strings and
 * arrays. Has no effect on deserialization.
 *
 * ```kotlin
 * @Serializable
 * data class Data(
 *     @TomlMultiline
 *     val multilineString: String,
 *     @TomlMultiline
 *     val multilineArray: List<Int>,
 *     val multilineStringArray: List<@TomlMultiline String>,
 *     @TomlMultiline
 *     val multilineStringArray2: List<String>
 * )
 *
 * val data = Data(text = "Some\nText", list = listOf(3, 5, 7, 11))
 * ```
 *
 * would produce:
 *
 * ```toml
 * multilineString = """
 * Some
 * Text
 * """
 *
 * multilineArray = [
 *     3,
 *     5,
 *     7,
 *     11
 * ]
 *
 * multilineStringArray = [ """
 * string 1
 * """,
 * """
 * string 2
 * """ ]
 *
 * multilineStringArray2 = [
 *     """
 *     string 1
 *     """,
 *     """
 *     string 2
 *     """
 * ]
 * ```
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(PROPERTY, TYPE_PARAMETER)
public annotation class TomlMultiline
