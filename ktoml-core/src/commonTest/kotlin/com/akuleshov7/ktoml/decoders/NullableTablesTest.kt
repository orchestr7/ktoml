package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlInputConfig
import kotlinx.serialization.Serializable
import kotlin.test.Test

@Serializable
data class Key(val value: Long)

@Serializable
data class Config1(val key: Key? = null)

@Serializable
data class Config2(val key: Key? = null)

@Serializable
data class Config3(val key: Key = Key(0L))

@Serializable
data class Config(val key: Key? = Key(0L))

@Serializable
data class Config4(val key: Key)

class NullableTablesTest {
    @Test
    fun nullableKey() {
        val tomlInstance = Toml(
            inputConfig = TomlInputConfig(
                ignoreUnknownNames = true,
                allowEmptyValues = true
            )
        )

        """            
            [key]
            value = 1            
        """.trimIndent()
            .shouldDecodeInto(
                decodedValue = Config1(Key(1L)),
                tomlInstance = tomlInstance
            )

        """            
            [key]
            value = 1            
        """.trimIndent()
            .shouldDecodeInto(
                decodedValue = Config2(Key(1L)),
                tomlInstance = tomlInstance
            )

        """            
            [key]
            value = 1            
        """.trimIndent()
            .shouldDecodeInto(
                decodedValue = Config3(Key(1L)),
                tomlInstance = tomlInstance
            )

        """            
            [key]
            value = 1            
        """.trimIndent()
            .shouldDecodeInto(
                decodedValue = Config4(Key(1L)),
                tomlInstance = tomlInstance
            )
    }
}

class EmptyTomlTest {
    @Test
    fun emptyToml() {
        """            
             
             
        """.trimIndent()
            .shouldDecodeInto(Config())

        "".shouldDecodeInto(Config())
    }
}
