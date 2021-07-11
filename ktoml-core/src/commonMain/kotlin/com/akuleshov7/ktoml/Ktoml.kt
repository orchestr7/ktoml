/**
 * Ktoml public API for TOML serialiation and deserialization
 */

package com.akuleshov7.ktoml

import okio.ExperimentalFileSystem

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer

/**
 * simple deserializer of a string in a toml format (separated by newlines)
 *
 * this: request-string in toml format with '\n' or '\r\n' separation
 *
 * @param ktomlConfig - optional config to configure extra options (not required)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> String.deserializeToml(
    ktomlConfig: KtomlConf = KtomlConf()
): T = Toml(ktomlConfig).decodeFromString(serializer(), this)

/**
 * partial deserializer of a string in a toml format (separated by newlines).
 * Will deserialize only the part presented under the tomlTableName table.
 * If such table is missing in he input - will throw an exception
 *
 * (!) Useful when you would like to deserialize only ONE table
 * and you do not want to reproduce whole object structure in the code
 *
 * this: request-string in toml format with '\n' or '\r\n' separation
 *
 * @param ktomlConfig - optional config to configure extra options (not required)
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T : Any> String.deserializeToml(
    tomlTableName: String,
    ktomlConfig: KtomlConf = KtomlConf()
): T = Toml(ktomlConfig).partiallyDecodeFromString(serializer(), this, tomlTableName)

/**
 * simple deserializer of a file that contains toml. Reading file with okio native library
 *
 * this: path to the file where toml is stored
 *
 * @param ktomlConfig - optional config to configure extra options (not required)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalFileSystem::class, ExperimentalSerializationApi::class)
inline fun <reified T : Any> String.deserializeTomlFile(
    ktomlConfig: KtomlConf = KtomlConf()
): T = Toml(ktomlConfig).decodeFromFile(serializer(), this)

/**
 * partial deserializer of a file that contains toml. Reading file with okio native library.
 * Will deserialize only the part presented under the tomlTableName table.
 * If such table is missing in he input - will throw an exception.
 *
 * (!) Useful when you would like to deserialize only ONE table
 * and you do not want to reproduce whole object structure in the code
 *
 * this: path to the file where toml is stored
 *
 * @param ktomlConfig - optional config to configure extra options (not required)
 * @param tomlTableName fully qualified name of the toml table (it should be the full name -  a.b.c.d)
 * @return deserialized object of type T
 */
@OptIn(ExperimentalFileSystem::class, ExperimentalSerializationApi::class)
inline fun <reified T : Any> String.deserializeTomlFile(
    tomlTableName: String,
    ktomlConfig: KtomlConf = KtomlConf()
): T = Toml(ktomlConfig).partiallyDecodeFromFile(serializer(), this, tomlTableName)
