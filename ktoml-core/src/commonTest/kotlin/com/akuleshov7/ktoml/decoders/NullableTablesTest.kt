package com.akuleshov7.ktoml.decoders

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        val mapper = Toml(
            config = TomlConfig(
                ignoreUnknownNames = true,
                allowEmptyValues = true
            )
        )
        val toml1 = mapper.decodeFromString<Config1>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        assertNotNull(toml1)
        assertEquals(1L, toml1.key?.value)

        val toml2 = mapper.decodeFromString<Config2>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        assertNotNull(toml2)
        assertEquals(1L, toml2.key?.value)

        val toml3 = mapper.decodeFromString<Config3>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        assertNotNull(toml3)
        assertEquals(1L, toml3.key.value)

        val toml4 = mapper.decodeFromString<Config4>(
            """            
            [key]
            value = 1            
        """.trimIndent()
        )

        assertNotNull(toml4)
        assertEquals(1L, toml4.key.value)
    }
}

class EmptyTomlTest {
    @Test
    fun emptyToml() {
        var res = Toml().decodeFromString<Config>(
            """            
             
             
                """.trimIndent()
        )

        assertEquals(Config(), res)

        res = Toml().decodeFromString<Config>(
            "".trimIndent()
        )

        assertEquals(Config(), res)
    }
}
