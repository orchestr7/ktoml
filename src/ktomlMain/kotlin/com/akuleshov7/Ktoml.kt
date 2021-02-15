package com.akuleshov7

import com.akuleshov7.decoders.TomlInput
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

/**
 * Ktoml class - is a general class, that should be used to parse toml
 *
 * @param
 *
 */
@OptIn(ExperimentalSerializationApi::class)
public class Ktoml(
    override val serializersModule: SerializersModule = EmptySerializersModule,
) : StringFormat {

    fun <T> readToml(): T {
        TODO()
    }

    fun <T> writeToml(obj: T): List<String> {
        TODO()
    }

    override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        return TomlInput.createFor(string, serializersModule).decodeSerializableValue(deserializer)
    }

    override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        TODO("Not yet implemented")
    }

    public companion object {
        // Ktoml.decodeFromString<Team>(input) or Ktoml.encodeToString<Team>(input)
        @OptIn(InternalSerializationApi::class)
        public inline fun <reified T : Any> decodeFromString(string: String): T =
            Ktoml().decodeFromString(T::class.serializer(), string)

        @OptIn(InternalSerializationApi::class)
        public inline fun <reified T : Any> encodeToString(value: T): String =
            Ktoml().encodeToString(T::class.serializer(), value)
    }
}
