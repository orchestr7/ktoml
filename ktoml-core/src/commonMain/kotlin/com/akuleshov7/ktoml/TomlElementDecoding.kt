/**
 * Convenience functions for decoding parsed TOML elements.
 */

package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import kotlinx.serialization.serializer

/**
 * Reified convenience for [Toml.decodeFromTomlElement].
 *
 * @param element parsed element to decode
 * @return decoded value
 */
@ExperimentalKtomlApi
public inline fun <reified T> Toml.decodeFromTomlElement(element: TomlElement): T =
    decodeFromTomlElement(serializersModule.serializer(), element)
