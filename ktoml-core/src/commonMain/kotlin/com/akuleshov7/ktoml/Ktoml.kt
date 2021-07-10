/**
 * Ktoml public API for TOML serialiation and deserialization
 */

package com.akuleshov7.ktoml

import com.akuleshov7.ktoml.decoders.DecoderConf

import okio.ExperimentalFileSystem

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

/**
 * simple deserializer of a string in a toml format (separated by newlines)
 *
 * @param request - string in toml format with '\n' or '\r\n' separation
 * @param decoderConfig - optional config to configure extra options (not required)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> deserialize(
    request: String,
    decoderConfig: DecoderConf = DecoderConf()
): T = KtomlSerializer(decoderConfig).decodeFromString(serializer(), request)

/**
 * partial deserializer of a string in a toml format (separated by newlines). Will deserialize only the part presented
 * under the tomlTableName table. If such table is missing in he input - will throw an exception
 *
 * @param request - string in toml format with '\n' or '\r\n' separation
 * @param decoderConfig - optional config to configure extra options (not required)
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> deserialize(
    request: String,
    tomlTableName: String,
    decoderConfig: DecoderConf = DecoderConf()
): T = KtomlSerializer(decoderConfig).decodeFromString(serializer(), request, tomlTableName)

/**
 * simple deserializer of a file that contains toml. Reading file with okio native library
 *
 * @param tomlFilePath - path to the file where toml is stored
 * @param decoderConfig - optional config to configure extra options (not required)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalFileSystem::class, ExperimentalSerializationApi::class)
inline fun <reified T : Any> deserializeFile(
    tomlFilePath: String,
    decoderConfig: DecoderConf = DecoderConf()
): T = KtomlSerializer(decoderConfig).decodeFromFile(serializer(), tomlFilePath)

/**
 * partial deserializer of a file that contains toml. Reading file with okio native library. Will deserialize only the part presented
 * under the tomlTableName table. If such table is missing in he input - will throw an exception.
 *
 * @param tomlFilePath - path to the file where toml is stored
 * @param decoderConfig - optional config to configure extra options (not required)
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalFileSystem::class, ExperimentalSerializationApi::class)
inline fun <reified T : Any> deserializeFile(
    tomlFilePath: String,
    tomlTableName: String,
    decoderConfig: DecoderConf = DecoderConf()
): T = KtomlSerializer(decoderConfig).decodeFromFile(serializer(), tomlFilePath, tomlTableName)
