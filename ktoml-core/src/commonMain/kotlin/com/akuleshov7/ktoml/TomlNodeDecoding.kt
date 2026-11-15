/**
 * Reified, top-level convenience extensions for decoding an already-parsed TOML AST node into a
 * typed object via [Toml.decodeFromTomlNode], without re-parsing the source.
 */

package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.annotations.ExperimentalKtomlApi
import com.akuleshov7.ktoml.annotations.InternalKtomlApi
import com.akuleshov7.ktoml.tree.nodes.TomlNode
import kotlinx.serialization.serializer

/**
 * Reified convenience for [Toml.decodeFromTomlNode]: decodes an already-parsed [TomlNode] (e.g. a
 * `TomlTable` obtained by traversing the result of [Toml.tomlParser]) into an object of type [T]
 * WITHOUT re-parsing the input.
 *
 * @param node the already-parsed node to decode; typically a `TomlTable` or a
 *   [com.akuleshov7.ktoml.tree.nodes.TomlFile]
 * @return deserialized object of type T
 */
@ExperimentalKtomlApi
@InternalKtomlApi
public inline fun <reified T> Toml.decodeFromTomlNode(node: TomlNode): T =
    decodeFromTomlNode(serializersModule.serializer(), node)
